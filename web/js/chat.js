/**
 * ===================================================================
 * UDP MULTICHAT ENTERPRISE - CLIENT CONTROLLER (VANILLA JS)
 * Xu ly dong bo thoi gian thuc, truyen nhan tep/folder, sticker, link preview
 * ===================================================================
 */

let currentRoom = null;
let currentUser = null;
let lastTimestamp = 0;
let pollTimer = null;
let isAudioEnabled = true;
let selectedWhisper = { userId: '', username: '' };
let replyingTo = null; // { msgKey, msgDomId, senderName, preview }
const messageStore = new Map();
const reactionStore = new Map();
const QUICK_EMOJIS = ['👍', '❤️', '😂', '😮', '😢'];

function getMessageKey(timestamp, senderId) {
  return `${timestamp}_${senderId || ''}`;
}

function getMessageDomId(timestamp, senderId) {
  return 'msg-' + timestamp + '-' + (senderId ? senderId.substring(0, 8) : '0');
}

function getHiddenStorageKey() {
  const roomCode = currentRoom ? currentRoom.roomCode : 'DEFAULT';
  const uid = currentUser ? currentUser.userId : 'anon';
  return `hiddenMsgs_${roomCode}_${uid}`;
}

function getHiddenMessageSet() {
  try {
    const raw = localStorage.getItem(getHiddenStorageKey());
    if (!raw) return new Set();
    return new Set(JSON.parse(raw));
  } catch (e) {
    return new Set();
  }
}

function isHiddenMessage(msgKey) {
  return getHiddenMessageSet().has(msgKey);
}

function hideMessageForMe(msgKey) {
  try {
    const set = getHiddenMessageSet();
    set.add(msgKey);
    localStorage.setItem(getHiddenStorageKey(), JSON.stringify(Array.from(set)));
  } catch (e) {
  }
}

function showToast(text) {
  let toast = document.getElementById('chatToastNotice');
  if (!toast) {
    toast = document.createElement('div');
    toast.id = 'chatToastNotice';
    toast.className = 'chat-toast-notice';
    document.body.appendChild(toast);
  }
  toast.innerText = text;
  toast.classList.add('show');
  clearTimeout(toast._hideTimer);
  toast._hideTimer = setTimeout(() => {
    toast.classList.remove('show');
  }, 2000);
}

function setWhisperTarget(userId, username) {
  selectedWhisper.userId = (userId || '').trim();
  selectedWhisper.username = (username || '').trim();
}

function getWhisperTarget() {
  if (selectedWhisper.userId) {
    return { targetUserId: selectedWhisper.userId, targetUser: selectedWhisper.username };
  }
  const select = document.getElementById('recipientSelect');
  const dSelect = document.getElementById('desktopRecipientSelect');
  const activeSelect = (select && select.value) ? select : ((dSelect && dSelect.value) ? dSelect : null);
  if (!activeSelect) {
    return { targetUserId: '', targetUser: '' };
  }
  const opt = activeSelect.options[activeSelect.selectedIndex];
  return {
    targetUserId: activeSelect.value,
    targetUser: opt ? (opt.dataset.username || activeSelect.value) : activeSelect.value
  };
}

// Khoi tao Web Audio API de tao am thanh thong bao
let audioCtx = null;
function playNotificationSound() {
  if (!isAudioEnabled) return;
  try {
    if (!audioCtx) {
      audioCtx = new (window.AudioContext || window.webkitAudioContext)();
    }
    if (audioCtx.state === 'suspended') {
      audioCtx.resume();
    }
    const osc = audioCtx.createOscillator();
    const gain = audioCtx.createGain();
    osc.type = 'sine';
    osc.frequency.setValueAtTime(587.33, audioCtx.currentTime); // D5
    osc.frequency.exponentialRampToValueAtTime(880, audioCtx.currentTime + 0.1); // A5
    gain.gain.setValueAtTime(0.08, audioCtx.currentTime);
    gain.gain.exponentialRampToValueAtTime(0.001, audioCtx.currentTime + 0.2);
    osc.connect(gain);
    gain.connect(audioCtx.destination);
    osc.start();
    osc.stop(audioCtx.currentTime + 0.2);
  } catch (e) {
  }
}

// Khoi dong phien chat
function initChat(roomData, userData) {
  currentRoom = roomData;
  currentUser = userData;

  scrollToBottom();

  // Bat dau vong lap polling lay tin nhan moi
  pollMessages();
  pollTimer = setInterval(pollMessages, 1200);

  // Xu ly go phim Enter de gui
  const textarea = document.getElementById('chatInput');
  if (textarea) {
    textarea.addEventListener('keydown', function(e) {
      if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        sendChatMessage();
      }
    });

    textarea.addEventListener('input', function() {
      this.style.height = 'auto';
      this.style.height = Math.min(this.scrollHeight, 84) + 'px';
    });
  }

  if (typeof initPickerCatalog === 'function') {
    initPickerCatalog();
  }

  // Dong picker va action sheet khi click ra ngoai
  document.addEventListener('click', function(e) {
    const picker = document.getElementById('pickerPopup');
    const pickerBtn = document.getElementById('btnTogglePicker');
    if (picker && !picker.contains(e.target) && pickerBtn && !pickerBtn.contains(e.target)) {
      picker.style.display = 'none';
    }

    const sheet = document.getElementById('composerActionSheet');
    const sheetBtn = document.getElementById('btnToggleActionSheet');
    if (sheet && !sheet.contains(e.target) && sheetBtn && !sheetBtn.contains(e.target)) {
      sheet.style.display = 'none';
    }

    if (!e.target.closest('.msg-react-wrapper')) {
      document.querySelectorAll('.msg-react-popup.open').forEach(el => el.classList.remove('open'));
    }
    if (!e.target.closest('.msg-more-wrapper')) {
      document.querySelectorAll('.msg-more-menu.open').forEach(el => el.classList.remove('open'));
    }
    if (!e.target.closest('.msg-row')) {
      document.querySelectorAll('.msg-row.actions-open').forEach(el => el.classList.remove('actions-open'));
    }
  });
}

// Lay tin nhan moi va danh sach thanh vien
async function pollMessages() {
  if (!currentRoom) return;

  try {
    const resp = await fetch(`api/chat?action=poll&roomCode=${encodeURIComponent(currentRoom.roomCode)}&since=${lastTimestamp}`);
    const data = await resp.json();

    if (data.success) {
      // Kiem tra neu bi Admin kick
      if (data.isKicked) {
        clearInterval(pollTimer);
        alert('Bạn đã bị Quản trị viên mời ra khỏi phòng chat!');
        window.location.href = 'index.jsp';
        return;
      }

      // Dong bo trang thai phong bi khoa
      if (data.roomStatus) {
        const b = document.getElementById('roomStatusBadge');
        if (b) {
          b.innerText = data.roomStatus;
          b.className = 'badge ' + (data.roomStatus === 'ACTIVE' ? 'badge-online' : 'badge-admin');
        }
        const lockBanner = document.getElementById('roomLockedBanner');
        if (lockBanner) {
          lockBanner.style.display = (data.roomStatus === 'LOCKED') ? 'block' : 'none';
        }
      }

      // Kiem tra bi tat tieng
      const chatInput = document.getElementById('chatInput');
      if (chatInput) {
        if (data.isMuted) {
          chatInput.placeholder = '🚫 Bạn đang bị Admin tắt tiếng trong phòng này...';
        } else if (data.roomStatus === 'LOCKED' && currentUser && currentUser.role !== 'ADMIN') {
          chatInput.placeholder = '🔒 Phòng chat đang bị tạm khóa bởi Admin...';
        } else {
          chatInput.placeholder = 'Nhập tin nhắn... (Enter để gửi, Shift+Enter để xuống dòng)';
        }
      }

      if (data.messages && data.messages.length > 0) {
        let hasNewFromOther = false;
        let hasAppendedVisible = false;
        data.messages.forEach(msg => {
          if (msg.type === 'RECALL') {
            handleRecallEvent(msg);
          } else if (msg.type === 'REACTION') {
            // Reaction se duoc cap nhat dong bo ben duoi qua data.reactions
          } else {
            const added = appendMessage(msg);
            if (added) hasAppendedVisible = true;
            if (currentUser && msg.senderId !== currentUser.userId && msg.type !== 'HEARTBEAT') {
              hasNewFromOther = true;
            }
          }
          if (msg.timestamp > lastTimestamp) {
            lastTimestamp = msg.timestamp;
          }
        });

        if (hasNewFromOther) {
          playNotificationSound();
        }
        if (hasAppendedVisible) {
          scrollToBottom();
        }
      }

      // Dong bo cam xuc (reactions) cho cac tin nhan dang hien thi
      if (data.reactions) {
        syncAllReactions(data.reactions);
      }

      // Cap nhat danh sach online
      if (data.members) {
        renderMembersList(data.members);
      }
    }
  } catch (err) {
    console.warn('Loi polling:', err);
  }
}

// Render tin nhan vao vung cuon
function appendMessage(msg) {
  const container = document.getElementById('chatMessages');
  if (!container) return false;

  const msgKey = getMessageKey(msg.timestamp, msg.senderId);
  const msgId = getMessageDomId(msg.timestamp, msg.senderId);
  if (document.getElementById(msgId)) return false;

  if (isHiddenMessage(msgKey)) return false;

  const isSelf = currentUser && msg.senderId === currentUser.userId;
  const isWhisper = msg.type === 'WHISPER' || (msg.targetUser && msg.targetUser.trim() !== '');
  const isSystem = msg.type === 'SYSTEM' || msg.type === 'JOIN' || msg.type === 'LEAVE' || msg.type === 'PROFILE_UPDATE';

  // Loc phong thu lop Frontend: tuyet doi khong hien thi tin nhan rieng khong thuoc ve currentUser
  if (isWhisper) {
    if (!currentUser) return false;
    const msgTargetId = msg.targetUserId || (msg.extraData && typeof msg.extraData === 'object' ? msg.extraData.targetUserId : '');
    let isRecipient = false;
    if (msgTargetId && msgTargetId.trim() !== '') {
      isRecipient = (msgTargetId === currentUser.userId);
    } else if (msg.targetUser && msg.targetUser.trim() !== '') {
      const targetLower = msg.targetUser.trim().toLowerCase();
      isRecipient = (currentUser.userId && currentUser.userId.toLowerCase() === targetLower)
        || (currentUser.username && currentUser.username.trim().toLowerCase() === targetLower);
    }
    if (!isSelf && !isRecipient) {
      return false;
    }
  }

  const extra = (msg.extraData && typeof msg.extraData === 'object') ? msg.extraData : {};
  const isRecalled = extra.recalled === true;

  const row = document.createElement('div');
  row.id = msgId;
  row.dataset.msgKey = msgKey;
  row.className = `msg-row ${isSystem ? 'system' : (isSelf ? 'self' : 'other')} ${isWhisper ? 'whisper' : ''} ${isRecalled ? 'recalled' : ''}`;

  if (isSystem) {
    row.innerHTML = `<div class="msg-bubble">${escapeHtml(msg.content)} <span class="msg-time">${msg.formattedTime || ''}</span></div>`;
    container.appendChild(row);
    return true;
  }

  messageStore.set(msgKey, msg);

  let bodyHtml = '';
  if (isRecalled) {
    bodyHtml = `<div class="msg-recalled-text">🚫 Tin nhắn đã được thu hồi</div>`;
  } else if (msg.type === 'STICKER') {
    let stickerUrl = 'assets/stickers/cat_love.svg';
    if (msg.extraData && msg.extraData.stickerUrl) {
      stickerUrl = msg.extraData.stickerUrl;
    }
    bodyHtml = `<img src="${stickerUrl}" class="sticker-msg" alt="Sticker" />`;

  } else if (msg.type === 'GIF') {
    const gData = (msg.extraData && typeof msg.extraData === 'object') ? msg.extraData : {};
    const gifUrl = gData.gifUrl || 'assets/gifs/hello.gif';
    const gifTitle = gData.title || 'GIF';
    bodyHtml = `<img src="${escapeHtml(gifUrl)}" class="gif-msg" onclick="openLightbox('${escapeHtml(gifUrl)}')" alt="${escapeHtml(gifTitle)}" title="${escapeHtml(gifTitle)}" loading="lazy" />`;

  } else if (msg.type === 'FILE') {
    const fData = msg.extraData || {};
    const fName = fData.fileName || 'Tệp đính kèm';
    const fSize = formatBytes(fData.fileSize || 0);
    const fUrl = fData.fileUrl || '#';
    const isImg = isImageExtension(fName);

    bodyHtml = `
      <div>${escapeHtml(msg.content)}</div>
      ${isImg ? `<img src="${fUrl}" class="image-preview" onclick="openLightbox('${fUrl}')" alt="Xem ảnh" />` : ''}
      <div class="file-card">
        <div class="file-icon-box">
          <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
            <polyline points="14 2 14 8 20 8"></polyline>
          </svg>
        </div>
        <div class="file-info">
          <div class="file-name" title="${escapeHtml(fName)}">${escapeHtml(fName)}</div>
          <div class="file-size">${fSize}</div>
        </div>
        <a href="${fUrl}" download="${escapeHtml(fName)}" class="file-download-btn">Tải xuống</a>
      </div>
    `;

  } else if (msg.type === 'FOLDER') {
    const fData = msg.extraData || {};
    const fName = fData.folderName || 'Thư mục';
    const count = fData.fileCount || 0;
    const fSize = formatBytes(fData.totalSize || 0);
    const zUrl = fData.zipUrl || '#';

    bodyHtml = `
      <div>${escapeHtml(msg.content)}</div>
      <div class="folder-card">
        <div class="file-icon-box">
          <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"></path>
          </svg>
        </div>
        <div class="file-info">
          <div class="file-name" title="${escapeHtml(fName)}">${escapeHtml(fName)} (ZIP)</div>
          <div class="file-size">${count} tệp • ${fSize}</div>
        </div>
        <a href="${zUrl}" download="${escapeHtml(fName)}.zip" class="file-download-btn">Tải ZIP</a>
      </div>
    `;

  } else if (msg.type === 'LINK') {
    const lData = msg.extraData || {};
    const url = lData.url || extractUrl(msg.content) || '#';
    const title = lData.title || url;
    const domain = lData.domain || extractDomain(url);

    bodyHtml = `
      <div>${formatMessageText(msg.content)}</div>
      <a href="${url}" target="_blank" rel="noopener noreferrer" class="link-card">
        <div class="link-card-body">
          <div class="link-domain">
            <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"></path>
              <path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"></path>
            </svg>
            ${escapeHtml(domain)}
          </div>
          <div class="link-title">${escapeHtml(title)}</div>
        </div>
      </a>
    `;

  } else {
    bodyHtml = formatMessageText(msg.content);
  }

  const avatarSrc = msg.avatarUrl || 'assets/avatars/avatar1.svg';
  let whisperTag = '';
  if (isWhisper) {
    if (isSelf) {
      whisperTag = `<span class="badge badge-whisper">🔒 Riêng tới ${escapeHtml(msg.targetUser || '')}</span>`;
    } else {
      whisperTag = `<span class="badge badge-whisper">🔒 ${escapeHtml(msg.senderName || 'Ai đó')} nhắn riêng cho bạn</span>`;
    }
  }

  // Kiem tra vai tro Admin de render badge noi bat
  let isAdminMsg = false;
  if (msg.extraData) {
    if (typeof msg.extraData === 'object' && msg.extraData.role === 'ADMIN') {
      isAdminMsg = true;
    } else if (typeof msg.extraData === 'string' && msg.extraData.indexOf('"role":"ADMIN"') !== -1) {
      isAdminMsg = true;
    }
  }
  if (!isAdminMsg && msg.senderName && (msg.senderName.toLowerCase().indexOf('admin') !== -1 || msg.senderName.toLowerCase().indexOf('quản trị') !== -1)) {
    isAdminMsg = true;
  }
  const adminBadge = isAdminMsg ? '<span class="badge badge-admin-crown" style="font-size:10px; margin-left:4px;">👑 Admin</span>' : '';
  const senderClass = isAdminMsg ? 'admin-msg-sender' : 'msg-sender';

  const replyQuoteHtml = (!isRecalled && extra.replyToId)
    ? `<div class="msg-reply-quote" onclick="scrollToMessage('${escapeHtml(extra.replyToId)}')" title="Nhấp để xem tin nhắn gốc">
         <div class="msg-reply-sender">${escapeHtml(extra.replyToSender || 'Tin nhắn')}</div>
         <div class="msg-reply-preview">${escapeHtml(extra.replyToPreview || '')}</div>
       </div>`
    : '';

  const canRecall = !isRecalled && (isSelf || (currentUser && currentUser.role === 'ADMIN'));
  const canCopy = !isRecalled && msg.type !== 'STICKER';
  const emojiButtonsHtml = QUICK_EMOJIS.map(em =>
    `<button type="button" class="react-emoji-btn" onclick="sendReaction('${escapeHtml(msgKey)}','${em}')">${em}</button>`
  ).join('');

  const svgReply = '<svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="9 14 4 9 9 4"></polyline><path d="M20 20v-7a4 4 0 0 0-4-4H4"></path></svg>';
  const svgSmile = '<svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"></circle><path d="M8 14s1.5 2 4 2 4-2 4-2"></path><line x1="9" y1="9" x2="9.01" y2="9"></line><line x1="15" y1="9" x2="15.01" y2="9"></line></svg>';
  const svgMore = '<svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="1"></circle><circle cx="19" cy="12" r="1"></circle><circle cx="5" cy="12" r="1"></circle></svg>';
  const svgCopy = '<svg xmlns="http://www.w3.org/2000/svg" width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path></svg>';
  const svgTrash = '<svg xmlns="http://www.w3.org/2000/svg" width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"></polyline><path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"></path><path d="M10 11v6"></path><path d="M14 11v6"></path></svg>';
  const svgHide = '<svg xmlns="http://www.w3.org/2000/svg" width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>';

  const actionsHtml = !isRecalled
    ? `<div class="msg-actions">
         <button type="button" class="msg-action-btn" onclick="replyToMessage('${escapeHtml(msgKey)}')" title="Trả lời">${svgReply}</button>
         <div class="msg-react-wrapper">
           <button type="button" class="msg-action-btn" onclick="toggleReactMenu(this)" title="Thả cảm xúc">${svgSmile}</button>
           <div class="msg-react-popup">${emojiButtonsHtml}</div>
         </div>
         <div class="msg-more-wrapper">
           <button type="button" class="msg-action-btn" onclick="toggleMoreMenu(this)" title="Tùy chọn">${svgMore}</button>
           <div class="msg-more-menu">
             ${canCopy ? `<button type="button" class="msg-more-item" onclick="copyMessageContent('${escapeHtml(msgKey)}')">${svgCopy}<span>Sao chép</span></button>` : ''}
             ${canRecall ? `<button type="button" class="msg-more-item danger" onclick="recallMessage('${escapeHtml(msgKey)}')">${svgTrash}<span>Thu hồi</span></button>` : ''}
             <button type="button" class="msg-more-item" onclick="deleteMessageForMe('${escapeHtml(msgKey)}')">${svgHide}<span>Xóa phía tôi</span></button>
           </div>
         </div>
       </div>`
    : `<div class="msg-actions">
         <button type="button" class="msg-action-btn" onclick="deleteMessageForMe('${escapeHtml(msgKey)}')" title="Ẩn tin nhắn này">${svgHide}</button>
       </div>`;

  const reactionsHtml = !isRecalled ? `<div class="msg-reactions" id="react-${escapeHtml(msgId)}"></div>` : '';

  if (isSelf) {
    const selfAdminBadge = (currentUser && currentUser.role === 'ADMIN') ? '<span class="badge badge-admin-crown" style="font-size:10px; margin-left:4px;">👑 Admin</span>' : '';
    row.innerHTML = `
      <div class="msg-content-col">
        <div class="msg-bubble-wrap">
          ${actionsHtml}
          <div class="msg-bubble">
            <div class="msg-meta">
              ${selfAdminBadge}
              ${whisperTag}
              <span class="msg-time">${msg.formattedTime || ''}</span>
            </div>
            ${replyQuoteHtml}
            ${bodyHtml}
          </div>
        </div>
        ${reactionsHtml}
      </div>
    `;
  } else {
    row.innerHTML = `
      <div class="avatar-wrapper">
        <img src="${avatarSrc}" class="member-avatar" alt="${escapeHtml(msg.senderName)}" />
      </div>
      <div class="msg-content-col">
        <div class="msg-bubble-wrap">
          <div class="msg-bubble">
            <div class="msg-meta">
              <span class="${senderClass}">${escapeHtml(msg.senderName)} ${adminBadge}</span>
              ${whisperTag}
              <span class="msg-time">${msg.formattedTime || ''}</span>
            </div>
            ${replyQuoteHtml}
            ${bodyHtml}
          </div>
          ${actionsHtml}
        </div>
        ${reactionsHtml}
      </div>
    `;
  }

  const bubbleEl = row.querySelector('.msg-bubble');
  if (bubbleEl) {
    bubbleEl.addEventListener('click', (e) => {
      if (e.target.closest('a, button, img, .msg-reply-quote')) return;
      const wasOpen = row.classList.contains('actions-open');
      document.querySelectorAll('.msg-row.actions-open').forEach(el => el.classList.remove('actions-open'));
      if (!wasOpen) {
        row.classList.add('actions-open');
      }
    });
  }

  container.appendChild(row);
  if (!isRecalled && reactionStore.has(msgKey)) {
    renderReactionsForMessage(msgKey);
  }
  return true;
}

// Gui tin nhan text hoac link
async function sendChatMessage() {
  const textarea = document.getElementById('chatInput');
  if (!textarea) return;

  const text = textarea.value.trim();
  if (!text) return;

  const whisper = getWhisperTarget();

  const detectedUrl = extractUrl(text);
  let postData = new URLSearchParams();

  if (detectedUrl) {
    postData.append('type', 'LINK');
    postData.append('content', text);
    postData.append('url', detectedUrl);
    postData.append('title', text);
    postData.append('domain', extractDomain(detectedUrl));
  } else {
    postData.append('type', 'CHAT');
    postData.append('content', text);
  }

  if (whisper.targetUserId) {
    postData.append('targetUserId', whisper.targetUserId);
    postData.append('targetUser', whisper.targetUser);
  }

  if (replyingTo) {
    postData.append('replyToId', replyingTo.msgDomId);
    postData.append('replyToSender', replyingTo.senderName);
    postData.append('replyToPreview', replyingTo.preview);
  }

  textarea.value = '';
  textarea.style.height = 'auto';

  try {
    const resp = await fetch('api/chat?action=send', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
      body: postData.toString()
    });
    const res = await resp.json();
    if (!res.success) {
      alert(res.message || 'Không thể gửi tin nhắn.');
    } else {
      clearReply();
      pollMessages();
    }
  } catch (err) {
    alert('Lỗi kết nối khi gửi tin nhắn UDP.');
  }
}

// Gui Sticker
async function sendSticker(pack, stickerId, stickerUrl) {
  togglePicker(false);
  const whisper = getWhisperTarget();
  const postData = new URLSearchParams();
  postData.append('type', 'STICKER');
  postData.append('content', '[Sticker]');
  postData.append('pack', pack);
  postData.append('stickerId', stickerId);
  postData.append('stickerUrl', stickerUrl);
  if (whisper.targetUserId) {
    postData.append('targetUserId', whisper.targetUserId);
    postData.append('targetUser', whisper.targetUser);
  }
  if (replyingTo) {
    postData.append('replyToId', replyingTo.msgDomId);
    postData.append('replyToSender', replyingTo.senderName);
    postData.append('replyToPreview', replyingTo.preview);
  }

  try {
    const resp = await fetch('api/chat?action=send', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
      body: postData.toString()
    });
    const res = await resp.json();
    if (!res.success) {
      alert(res.message || 'Không thể gửi sticker.');
    } else {
      clearReply();
      pollMessages();
    }
  } catch (err) {
    alert('Lỗi gửi sticker.');
  }
}

// Gui GIF
async function sendGif(gifUrl, title) {
  togglePicker(false);
  const whisper = getWhisperTarget();
  const postData = new URLSearchParams();
  postData.append('type', 'GIF');
  postData.append('content', '[GIF]');
  postData.append('gifUrl', gifUrl);
  postData.append('title', title || 'GIF');
  if (whisper.targetUserId) {
    postData.append('targetUserId', whisper.targetUserId);
    postData.append('targetUser', whisper.targetUser);
  }
  if (replyingTo) {
    postData.append('replyToId', replyingTo.msgDomId);
    postData.append('replyToSender', replyingTo.senderName);
    postData.append('replyToPreview', replyingTo.preview);
  }

  try {
    const resp = await fetch('api/chat?action=send', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
      body: postData.toString()
    });
    const res = await resp.json();
    if (!res.success) {
      alert(res.message || 'Không thể gửi GIF.');
    } else {
      clearReply();
      pollMessages();
    }
  } catch (err) {
    alert('Lỗi kết nối khi gửi GIF.');
  }
}

// Chen Emoji vao textarea tai vi tri con tro & luu gan day
function insertEmoji(emoji) {
  const textarea = document.getElementById('chatInput');
  if (textarea) {
    const start = textarea.selectionStart != null ? textarea.selectionStart : textarea.value.length;
    const end = textarea.selectionEnd != null ? textarea.selectionEnd : textarea.value.length;
    const before = textarea.value.substring(0, start);
    const after = textarea.value.substring(end);
    textarea.value = before + emoji + after;
    const nextPos = start + emoji.length;
    textarea.focus();
    try {
      textarea.setSelectionRange(nextPos, nextPos);
    } catch (e) {
    }
    textarea.style.height = 'auto';
    textarea.style.height = Math.min(textarea.scrollHeight, 84) + 'px';
  }
  recordRecentEmoji(emoji);
}

// Tai len File
async function uploadFile(fileInput) {
  const file = fileInput.files[0];
  if (!file) return;

  const whisper = getWhisperTarget();
  const formData = new FormData();
  formData.append('uploadType', 'file');
  formData.append('file', file);
  if (whisper.targetUserId) {
    formData.append('targetUserId', whisper.targetUserId);
    formData.append('targetUser', whisper.targetUser);
  }

  try {
    const resp = await fetch('api/upload', {
      method: 'POST',
      body: formData
    });
    const res = await resp.json();
    if (!res.success) {
      alert(res.message || 'Lỗi tải lên tệp.');
    }
    fileInput.value = '';
  } catch (e) {
    alert('Lỗi kết nối khi tải lên tệp.');
  }
}

// Tai len Folder
async function uploadFolder(folderInput) {
  const files = folderInput.files;
  if (!files || files.length === 0) return;

  let folderName = 'ThuMuc_' + Math.floor(Math.random() * 1000);
  if (files[0].webkitRelativePath) {
    const parts = files[0].webkitRelativePath.split('/');
    if (parts.length > 1) {
      folderName = parts[0];
    }
  }

  const whisper = getWhisperTarget();
  const formData = new FormData();
  formData.append('uploadType', 'folder');
  formData.append('folderName', folderName);
  if (whisper.targetUserId) {
    formData.append('targetUserId', whisper.targetUserId);
    formData.append('targetUser', whisper.targetUser);
  }

  for (let i = 0; i < files.length; i++) {
    formData.append('files', files[i]);
  }

  try {
    const resp = await fetch('api/upload', {
      method: 'POST',
      body: formData
    });
    const res = await resp.json();
    if (!res.success) {
      alert(res.message || 'Lỗi tải lên thư mục.');
    }
    folderInput.value = '';
  } catch (e) {
    alert('Lỗi khi tải lên thư mục.');
  }
}

// Cap nhat danh sach thanh vien online (Phan nhom Admin va Member theo chuan SaaS)
function renderMembersList(members) {
  const container = document.getElementById('membersList');
  const countEl = document.getElementById('onlineCount');
  const mobileCountEl = document.getElementById('mobileOnlineCount');
  const headerOnlineText = document.getElementById('headerOnlineText');
  const headerBadgeCount = document.getElementById('headerBadgeCount');
  const select = document.getElementById('recipientSelect');

  if (countEl) countEl.innerText = members.length;
  if (mobileCountEl) mobileCountEl.innerText = members.length;
  if (headerBadgeCount) headerBadgeCount.innerText = members.length;
  if (headerOnlineText) headerOnlineText.innerText = `${members.length} người trực tuyến`;
  if (!container) return;

  const desktopSelect = document.getElementById('desktopRecipientSelect');
  const currentSelectedId = selectedWhisper.userId || (select ? select.value : '') || (desktopSelect ? desktopSelect.value : '');
  let matchedSelectedUser = null;

  container.innerHTML = '';
  if (select) {
    select.innerHTML = '<option value="">Cả phòng (Tất cả)</option>';
  }
  if (desktopSelect) {
    desktopSelect.innerHTML = '<option value="">Cả phòng (Tất cả)</option>';
  }

  // Chia danh sach thanh 2 nhom: Quản trị viên (ADMIN) & Thành viên (MEMBER)
  const admins = members.filter(u => u.role === 'ADMIN');
  const regularMembers = members.filter(u => u.role !== 'ADMIN');

  const renderGroup = (groupTitle, list, isAdminGroup) => {
    if (list.length === 0) return;

    const groupHeader = document.createElement('div');
    groupHeader.className = 'member-group-title';
    groupHeader.innerHTML = `
      <span>${groupTitle}</span>
      <span class="group-count">${list.length}</span>
    `;
    container.appendChild(groupHeader);

    list.forEach(u => {
      const isSelf = currentUser && u.userId === currentUser.userId;
      const item = document.createElement('div');
      item.className = 'member-item' + (isAdminGroup ? ' is-admin' : '');
      item.title = `${u.username} (${u.role}) - Nhấp để nhắn riêng`;

      const roleBadgeHtml = (u.role === 'ADMIN')
        ? '<span class="badge badge-admin-crown" style="font-size:9.5px; padding:1px 5px;">👑 Admin</span>'
        : '<span class="badge badge-member-mini">Thành viên</span>';

      item.innerHTML = `
        <div class="avatar-wrapper">
          <img src="${u.avatarUrl}" class="member-avatar" alt="${escapeHtml(u.username)}" />
          <span class="avatar-badge"></span>
        </div>
        <div class="member-info">
          <div class="member-name">
            <span>${escapeHtml(u.username)}</span>
            ${isSelf ? '<small class="self-tag">(Bạn)</small>' : ''}
            ${roleBadgeHtml}
          </div>
          <div class="member-bio">${escapeHtml(u.bio || 'Đang trực tuyến')}</div>
        </div>
      `;

      if (!isSelf) {
        if (currentSelectedId && u.userId === currentSelectedId) {
          matchedSelectedUser = u;
        }
        item.addEventListener('click', () => {
          if (typeof updateWhisperState === 'function') {
            updateWhisperState(u.userId, u.username);
          } else {
            setWhisperTarget(u.userId, u.username);
            if (select) select.value = u.userId;
            if (desktopSelect) desktopSelect.value = u.userId;
          }
          if (typeof toggleInfoDrawer === 'function') {
            toggleInfoDrawer(false);
          }
          if (typeof toggleSidebar === 'function') {
            toggleSidebar(false);
          }
          if (window.innerWidth <= 991 && typeof closeSidebarDrawer === 'function') {
            closeSidebarDrawer();
          }
          const chatInput = document.getElementById('chatInput');
          if (chatInput) chatInput.focus();
        });

        if (select) {
          const opt = document.createElement('option');
          opt.value = u.userId;
          opt.dataset.username = u.username;
          opt.innerText = `🔒 Nhắn riêng: ${u.username}`;
          if (u.userId === currentSelectedId) {
            opt.selected = true;
          }
          select.appendChild(opt);
        }

        if (desktopSelect) {
          const opt2 = document.createElement('option');
          opt2.value = u.userId;
          opt2.dataset.username = u.username;
          opt2.innerText = `🔒 ${u.username}`;
          if (u.userId === currentSelectedId) {
            opt2.selected = true;
          }
          desktopSelect.appendChild(opt2);
        }
      }

      container.appendChild(item);
    });
  };

  renderGroup('👑 QUẢN TRỊ VIÊN (ADMINS)', admins, true);
  renderGroup('👤 THÀNH VIÊN (MEMBERS)', regularMembers, false);

  if (currentSelectedId) {
    if (matchedSelectedUser) {
      if (typeof updateWhisperState === 'function') {
        updateWhisperState(matchedSelectedUser.userId, matchedSelectedUser.username);
      }
    } else {
      // Nguoi nhan da roi phong hoac bi kick -> tu dong huy trang thai nhan rieng
      if (typeof clearWhisper === 'function') {
        clearWhisper();
      }
    }
  }
}

function scrollToBottom() {
  const c = document.getElementById('chatMessages');
  if (c) {
    c.scrollTop = c.scrollHeight;
  }
}

const PICKER_EMOJI_GROUPS = [
  {
    id: 'smileys',
    title: 'Mặt cười & Biểu cảm',
    items: [
      { c: '😀', k: 'cuoi vui smile happy' }, { c: '😄', k: 'cuoi tuoi grin' }, { c: '😁', k: 'he rang' },
      { c: '😂', k: 'cuoi ra nuoc mat haha lol' }, { c: '🤣', k: 'cuoi lan lon haha' }, { c: '😊', k: 'mim cuoi de thuong' },
      { c: '😍', k: 'yeu thich tim mat' }, { c: '🥰', k: 'yeu thuong am ap' }, { c: '😘', k: 'hon gui yeu' },
      { c: '😎', k: 'ngau kinh ram cool' }, { c: '🤩', k: 'ngoi sao an tuong' }, { c: '🥳', k: 'tiec chuc mung party' },
      { c: '😏', k: 'nham hiem cuoi nhe' }, { c: '🤔', k: 'suy nghi thac mac' }, { c: '🤨', k: 'nghi ngo' },
      { c: '😮', k: 'ngac nhien wow' }, { c: '😲', k: 'bat ngo' }, { c: '🥺', k: 'nan ni de thuong' },
      { c: '😢', k: 'buon khoc sad' }, { c: '😭', k: 'khoc to hu hu' }, { c: '😤', k: 'tuc gian' },
      { c: '😡', k: 'gian du angry' }, { c: '🤯', k: 'no nao soc' }, { c: '😴', k: 'buon ngu' },
      { c: '😇', k: 'thien than' }, { c: '🤗', k: 'om hug' }, { c: '🤭', k: 'che mieng cuoi' },
      { c: '🤫', k: 'suyt bi mat' }
    ]
  },
  {
    id: 'gestures',
    title: 'Cử chỉ & Trái tim',
    items: [
      { c: '👍', k: 'thich ok like dong y' }, { c: '👎', k: 'khong thich' }, { c: '👌', k: 'ok chuan' },
      { c: '👏', k: 'vo tay chuc mung clap' }, { c: '🙌', k: 'hoan ho' }, { c: '🤝', k: 'bat tay hop tac' },
      { c: '🙏', k: 'cam on xin chao' }, { c: '👋', k: 'vay tay xin chao hello' }, { c: '✌️', k: 'chien thang peace' },
      { c: '🤞', k: 'chuc may man' }, { c: '💪', k: 'manh me co len' }, { c: '🫶', k: 'ban tay trai tim' },
      { c: '❤️', k: 'tim yeu love' }, { c: '🧡', k: 'tim cam' }, { c: '💛', k: 'tim vang' },
      { c: '💚', k: 'tim xanh' }, { c: '💙', k: 'tim duong' }, { c: '💜', k: 'tim tim' },
      { c: '🤍', k: 'tim trang' }, { c: '💔', k: 'tan vo' }, { c: '💯', k: '100 diem chuan' }
    ]
  },
  {
    id: 'work',
    title: 'Công việc & Hoạt động',
    items: [
      { c: '🔥', k: 'lua chay dinh fire' }, { c: '✨', k: 'lap lanh dep' }, { c: '🎉', k: 'phao hoa chuc mung party' },
      { c: '🎊', k: 'tung hoa' }, { c: '🚀', k: 'ten lua deploy phong' }, { c: '💻', k: 'laptop code lap trinh' },
      { c: '☕', k: 'ca phe coffee' }, { c: '🐛', k: 'con bo bug loi' }, { c: '✅', k: 'xong hoan thanh done' },
      { c: '⚡', k: 'tia set nhanh' }, { c: '🎯', k: 'muc tieu chuan' }, { c: '💡', k: 'y tuong sang tao' },
      { c: '📚', k: 'sach hoc tap' }, { c: '🎵', k: 'nhac vui' }
    ]
  }
];

const PICKER_STICKERS = [
  { pack: 'cat', id: 'love', url: 'assets/stickers/cat_love.svg', label: 'Mèo yêu thương tim' },
  { pack: 'cat', id: 'cool', url: 'assets/stickers/cat_cool.svg', label: 'Mèo ngầu kính râm cool' },
  { pack: 'cat', id: 'party', url: 'assets/stickers/cat_party.svg', label: 'Mèo tiệc tùng chúc mừng party' },
  { pack: 'cat', id: 'wow', url: 'assets/stickers/cat_wow.svg', label: 'Mèo ngạc nhiên wow' },
  { pack: 'tech', id: 'rocket', url: 'assets/stickers/tech_rocket.svg', label: 'Tên lửa tăng tốc rocket' },
  { pack: 'tech', id: 'coffee', url: 'assets/stickers/tech_coffee.svg', label: 'Cà phê lập trình coffee' },
  { pack: 'tech', id: 'bug', url: 'assets/stickers/tech_bug.svg', label: 'Sửa lỗi bug dev' },
  { pack: 'tech', id: 'ship', url: 'assets/stickers/tech_ship.svg', label: 'Hoàn thành xong OK ship' },
  { pack: 'react', id: 'like', url: 'assets/stickers/react_like.svg', label: 'Thích đồng ý like ok' },
  { pack: 'react', id: 'heart', url: 'assets/stickers/react_heart.svg', label: 'Trái tim yêu love' },
  { pack: 'react', id: 'fire', url: 'assets/stickers/react_fire.svg', label: 'Rực cháy quá đỉnh fire' },
  { pack: 'react', id: 'clap', url: 'assets/stickers/react_clap.svg', label: 'Vỗ tay chúc mừng clap' }
];

const PICKER_GIFS = [
  { id: 'hello', url: 'assets/gifs/hello.gif', title: 'Xin chào', keywords: 'xin chao hello hi chao' },
  { id: 'haha', url: 'assets/gifs/haha.gif', title: 'Hahaha', keywords: 'haha cuoi vui lol' },
  { id: 'love', url: 'assets/gifs/love.gif', title: 'Yêu thương', keywords: 'yeu thuong love tim' },
  { id: 'ok', url: 'assets/gifs/ok.gif', title: 'Chuẩn luôn', keywords: 'ok chuan dong y xong' },
  { id: 'clap', url: 'assets/gifs/clap.gif', title: 'Tuyệt vời', keywords: 'vo tay tuyet voi clap' },
  { id: 'fire', url: 'assets/gifs/fire.gif', title: 'Quá đỉnh', keywords: 'lua qua dinh fire hot' },
  { id: 'coding', url: 'assets/gifs/coding.gif', title: 'Đang code', keywords: 'code lap trinh dev fix bug' },
  { id: 'party', url: 'assets/gifs/party.gif', title: 'Chúc mừng', keywords: 'chuc mung party tiec' }
];

let activePickerTab = 'emoji';
let activeStickerPack = 'all';
let pickerInitialized = false;

function getRecentEmojis() {
  try {
    const raw = localStorage.getItem('udp_recent_emojis');
    const arr = raw ? JSON.parse(raw) : [];
    return Array.isArray(arr) ? arr : [];
  } catch (e) {
    return [];
  }
}

function recordRecentEmoji(emoji) {
  try {
    const recent = getRecentEmojis().filter(e => e !== emoji);
    recent.unshift(emoji);
    localStorage.setItem('udp_recent_emojis', JSON.stringify(recent.slice(0, 7)));
    if (activePickerTab === 'emoji') {
      renderEmojiPanel(getPickerQuery());
    }
  } catch (e) {
  }
}

function getPickerQuery() {
  const input = document.getElementById('pickerSearchInput');
  return input ? input.value.trim() : '';
}

function initPickerCatalog() {
  if (pickerInitialized) return;
  pickerInitialized = true;
  renderEmojiPanel('');
  renderStickerPanel('all', '');
  renderGifPanel('');
}

function renderEmojiPanel(query) {
  const panel = document.getElementById('pickerPanel-emoji');
  if (!panel) return;
  const q = (query || '').toLowerCase();
  let html = '';

  if (!q) {
    const recent = getRecentEmojis();
    if (recent.length > 0) {
      html += `<div class="picker-section-title">Thường dùng</div><div class="picker-emoji-grid">`;
      html += recent.map(em => `<button type="button" class="picker-emoji-btn" onclick="insertEmoji('${em}')" title="Chèn ${em}">${em}</button>`).join('');
      html += `</div>`;
    }
  }

  let totalMatched = 0;
  PICKER_EMOJI_GROUPS.forEach(group => {
    const filtered = q
      ? group.items.filter(it => it.c.includes(q) || it.k.includes(q))
      : group.items;
    if (filtered.length > 0) {
      totalMatched += filtered.length;
      html += `<div class="picker-section-title">${escapeHtml(group.title)}</div><div class="picker-emoji-grid">`;
      html += filtered.map(it => `<button type="button" class="picker-emoji-btn" onclick="insertEmoji('${it.c}')" title="${escapeHtml(it.k.split(' ')[0])}">${it.c}</button>`).join('');
      html += `</div>`;
    }
  });

  if (totalMatched === 0) {
    html = `<div class="picker-empty">Không tìm thấy biểu tượng phù hợp.</div>`;
  }
  panel.innerHTML = html;
}

function renderStickerPanel(pack, query) {
  const panel = document.getElementById('pickerPanel-sticker');
  if (!panel) return;
  const q = (query || '').toLowerCase();
  const filtered = PICKER_STICKERS.filter(s => {
    const matchPack = (pack === 'all') || (s.pack === pack);
    const matchQuery = !q || s.label.toLowerCase().includes(q) || s.id.includes(q) || s.pack.includes(q);
    return matchPack && matchQuery;
  });

  if (filtered.length === 0) {
    panel.innerHTML = `<div class="picker-empty" style="grid-column: 1 / -1;">Không tìm thấy sticker phù hợp.</div>`;
    return;
  }

  panel.innerHTML = filtered.map(s =>
    `<button type="button" class="picker-sticker-btn" onclick="sendSticker('${s.pack}','${s.id}','${s.url}')" title="${escapeHtml(s.label)}">
       <img src="${s.url}" alt="${escapeHtml(s.label)}" loading="lazy" />
     </button>`
  ).join('');
}

function renderGifPanel(query) {
  const panel = document.getElementById('pickerPanel-gif');
  if (!panel) return;
  const raw = (query || '').trim();
  const q = raw.toLowerCase();

  // Ho tro dan URL .gif truc tiep de gui
  const isGifUrl = /^https?:\/\/.+\.(gif|webp)(\?.*)?$/i.test(raw);
  if (isGifUrl) {
    panel.innerHTML = `
      <div class="picker-gif-url-card" style="grid-column: 1 / -1;">
        <img src="${escapeHtml(raw)}" alt="GIF Preview" onerror="this.style.display='none'" />
        <button type="button" class="btn btn-primary btn-sm" onclick="sendGif('${escapeHtml(raw)}','GIF')">Gửi GIF này</button>
      </div>
    `;
    return;
  }

  const filtered = q
    ? PICKER_GIFS.filter(g => g.title.toLowerCase().includes(q) || g.keywords.includes(q))
    : PICKER_GIFS;

  if (filtered.length === 0) {
    panel.innerHTML = `<div class="picker-empty" style="grid-column: 1 / -1;">Không có GIF khớp từ khóa. Bạn có thể dán trực tiếp link .gif vào ô tìm kiếm.</div>`;
    return;
  }

  panel.innerHTML = filtered.map(g =>
    `<button type="button" class="picker-gif-btn" onclick="sendGif('${g.url}','${escapeHtml(g.title)}')" title="Gửi GIF: ${escapeHtml(g.title)}">
       <img src="${g.url}" alt="${escapeHtml(g.title)}" loading="lazy" />
       <span class="gif-badge">GIF</span>
     </button>`
  ).join('');
}

function togglePicker(force) {
  const p = document.getElementById('pickerPopup');
  if (!p) return;
  initPickerCatalog();
  const willOpen = (typeof force === 'boolean') ? force : (p.style.display !== 'flex');
  p.style.display = willOpen ? 'flex' : 'none';
  if (willOpen) {
    switchPickerTab(activePickerTab);
  }
}

function switchPickerTab(tabName) {
  activePickerTab = tabName || 'emoji';
  document.querySelectorAll('.picker-tab').forEach(b => {
    const isActive = (b.id === 'tabBtn-' + activePickerTab);
    b.classList.toggle('active', isActive);
    b.setAttribute('aria-selected', isActive ? 'true' : 'false');
  });
  document.querySelectorAll('.picker-panel').forEach(p => p.style.display = 'none');

  const panel = document.getElementById('pickerPanel-' + activePickerTab);
  if (panel) {
    panel.style.display = (activePickerTab === 'emoji') ? 'block' : 'grid';
  }

  const packBar = document.getElementById('stickerPackBar');
  if (packBar) {
    packBar.style.display = (activePickerTab === 'sticker') ? 'flex' : 'none';
  }

  const searchInput = document.getElementById('pickerSearchInput');
  if (searchInput) {
    if (activePickerTab === 'emoji') searchInput.placeholder = 'Tìm biểu tượng cảm xúc...';
    else if (activePickerTab === 'sticker') searchInput.placeholder = 'Tìm nhãn dán (Mèo, Dev, Cảm xúc)...';
    else searchInput.placeholder = 'Tìm GIF hoặc dán đường dẫn .gif...';
  }
  filterPickerItems();
}

function filterStickerPack(pack) {
  activeStickerPack = pack || 'all';
  document.querySelectorAll('.picker-subtab').forEach(btn => {
    btn.classList.toggle('active', btn.dataset.pack === activeStickerPack);
  });
  renderStickerPanel(activeStickerPack, getPickerQuery());
}

function filterPickerItems() {
  const q = getPickerQuery();
  const clearBtn = document.getElementById('pickerSearchClear');
  if (clearBtn) {
    clearBtn.style.display = q ? 'inline-flex' : 'none';
  }
  if (activePickerTab === 'emoji') {
    renderEmojiPanel(q);
  } else if (activePickerTab === 'sticker') {
    renderStickerPanel(activeStickerPack, q);
  } else if (activePickerTab === 'gif') {
    renderGifPanel(q);
  }
}

function clearPickerSearch() {
  const input = document.getElementById('pickerSearchInput');
  if (input) {
    input.value = '';
    input.focus();
  }
  filterPickerItems();
}

function toggleTheme() {
  const current = document.documentElement.getAttribute('data-theme');
  const target = current === 'dark' ? 'light' : 'dark';
  document.documentElement.setAttribute('data-theme', target);
  localStorage.setItem('udp_theme', target);
}

function openLightbox(src) {
  const modal = document.getElementById('lightboxModal');
  const img = document.getElementById('lightboxImage');
  if (modal && img) {
    img.src = src;
    modal.style.display = 'flex';
  }
}

function closeLightbox() {
  const modal = document.getElementById('lightboxModal');
  if (modal) modal.style.display = 'none';
}

function extractUrl(text) {
  const urlRegex = /(https?:\/\/[^\s]+)/g;
  const match = text.match(urlRegex);
  return match ? match[0] : null;
}

function extractDomain(url) {
  try {
    return new URL(url).hostname;
  } catch (e) {
    return url;
  }
}

function formatMessageText(text) {
  if (!text) return '';
  const escaped = escapeHtml(text);
  const urlRegex = /(https?:\/\/[^\s]+)/g;
  return escaped.replace(urlRegex, url => `<a href="${url}" target="_blank" rel="noopener noreferrer" style="color:inherit;text-decoration:underline;font-weight:600;">${url}</a>`);
}

function isImageExtension(name) {
  if (!name) return false;
  const lower = name.toLowerCase();
  return lower.endsWith('.png') || lower.endsWith('.jpg') || lower.endsWith('.jpeg') || lower.endsWith('.gif') || lower.endsWith('.webp') || lower.endsWith('.svg');
}

function formatBytes(bytes) {
  if (bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
}

function escapeHtml(s) {
  if (!s) return '';
  return s.replace(/&/g, '&amp;')
          .replace(/</g, '&lt;')
          .replace(/>/g, '&gt;')
          .replace(/"/g, '&quot;')
          .replace(/'/g, '&#39;');
}

function copyRoomCode(code) {
  navigator.clipboard.writeText(code).then(() => {
    alert('Đã sao chép mã phòng: ' + code);
  });
}

// ===================================================================
// CHUC NANG TIN NHAN: TRA LOI, THU HOI, XOA PHIA TOI, SAO CHEP, CAM XUC
// ===================================================================

function buildMessagePreview(msg) {
  if (!msg) return '';
  if (msg.type === 'STICKER') return '[Sticker]';
  if (msg.type === 'GIF') return '[GIF]';
  if (msg.type === 'FILE') {
    const fName = (msg.extraData && msg.extraData.fileName) ? msg.extraData.fileName : 'Tệp đính kèm';
    return '[Tệp] ' + fName;
  }
  if (msg.type === 'FOLDER') {
    const fName = (msg.extraData && msg.extraData.folderName) ? msg.extraData.folderName : 'Thư mục';
    return '[Thư mục] ' + fName;
  }
  const text = (msg.content || '').trim();
  return text.length > 75 ? text.substring(0, 75) + '...' : text;
}

function replyToMessage(msgKey) {
  const msg = messageStore.get(msgKey);
  if (!msg) return;
  const domId = getMessageDomId(msg.timestamp, msg.senderId);
  const preview = buildMessagePreview(msg);
  replyingTo = {
    msgKey: msgKey,
    msgDomId: domId,
    senderName: msg.senderName || 'Thành viên',
    preview: preview
  };

  const ind = document.getElementById('replyIndicator');
  const senderEl = document.getElementById('replyTargetSender');
  const previewEl = document.getElementById('replyTargetPreview');
  if (senderEl) senderEl.innerText = replyingTo.senderName;
  if (previewEl) previewEl.innerText = preview;
  if (ind) ind.style.display = 'flex';

  const input = document.getElementById('chatInput');
  if (input) input.focus();
}

function clearReply() {
  replyingTo = null;
  const ind = document.getElementById('replyIndicator');
  if (ind) ind.style.display = 'none';
}

function scrollToMessage(msgDomId) {
  const el = document.getElementById(msgDomId);
  if (!el) {
    showToast('Tin nhắn gốc không còn trong khung chat hiện tại.');
    return;
  }
  el.scrollIntoView({ behavior: 'smooth', block: 'center' });
  el.classList.add('msg-highlight');
  setTimeout(() => el.classList.remove('msg-highlight'), 1500);
}

function copyMessageContent(msgKey) {
  const msg = messageStore.get(msgKey);
  if (!msg || !msg.content) return;
  navigator.clipboard.writeText(msg.content).then(() => {
    showToast('Đã sao chép tin nhắn');
  }).catch(() => {
    showToast('Không thể sao chép tự động');
  });
}

function deleteMessageForMe(msgKey) {
  const msg = messageStore.get(msgKey);
  hideMessageForMe(msgKey);
  if (msg) {
    const domId = getMessageDomId(msg.timestamp, msg.senderId);
    const el = document.getElementById(domId);
    if (el) el.remove();
  } else {
    const el = document.querySelector(`[data-msg-key="${CSS.escape(msgKey)}"]`);
    if (el) el.remove();
  }
  showToast('Đã xóa tin nhắn ở phía bạn');
}

async function recallMessage(msgKey) {
  const msg = messageStore.get(msgKey);
  if (!msg) return;
  if (!confirm('Thu hồi tin nhắn này với tất cả mọi người?')) return;

  const postData = new URLSearchParams();
  postData.append('action', 'recall');
  postData.append('targetTimestamp', String(msg.timestamp));
  postData.append('targetSenderId', msg.senderId || '');

  try {
    const resp = await fetch('api/chat', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
      body: postData.toString()
    });
    const res = await resp.json();
    if (!res.success) {
      alert(res.message || 'Không thể thu hồi tin nhắn.');
    } else {
      applyRecalledState(msg.timestamp, msg.senderId);
      pollMessages();
    }
  } catch (e) {
    alert('Lỗi kết nối khi thu hồi tin nhắn.');
  }
}

function handleRecallEvent(eventMsg) {
  const extra = (eventMsg && eventMsg.extraData && typeof eventMsg.extraData === 'object') ? eventMsg.extraData : {};
  const targetTs = extra.targetTimestamp;
  const targetSenderId = extra.targetSenderId;
  if (targetTs && targetSenderId) {
    applyRecalledState(targetTs, targetSenderId);
  }
}

function applyRecalledState(targetTimestamp, targetSenderId) {
  const msgKey = getMessageKey(targetTimestamp, targetSenderId);
  const domId = getMessageDomId(targetTimestamp, targetSenderId);
  const row = document.getElementById(domId);
  const stored = messageStore.get(msgKey);
  if (stored) {
    stored.content = '🚫 Tin nhắn đã được thu hồi';
    stored.extraData = Object.assign({}, stored.extraData || {}, { recalled: true });
  }
  if (replyingTo && replyingTo.msgKey === msgKey) {
    clearReply();
  }
  reactionStore.delete(msgKey);

  if (!row) return;
  row.classList.add('recalled');
  const bubble = row.querySelector('.msg-bubble');
  if (bubble) {
    const meta = bubble.querySelector('.msg-meta');
    const metaHtml = meta ? meta.outerHTML : '';
    bubble.innerHTML = `${metaHtml}<div class="msg-recalled-text">🚫 Tin nhắn đã được thu hồi</div>`;
  }
  const actions = row.querySelector('.msg-actions');
  if (actions) {
    actions.innerHTML = `<button type="button" class="msg-action-btn" onclick="deleteMessageForMe('${escapeHtml(msgKey)}')" title="Ẩn tin nhắn này"><svg xmlns="http://www.w3.org/2000/svg" width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg></button>`;
  }
  const reactionsEl = row.querySelector('.msg-reactions');
  if (reactionsEl) {
    reactionsEl.innerHTML = '';
    reactionsEl.style.display = 'none';
  }
}

function toggleMoreMenu(btnEl) {
  const wrapper = btnEl.closest('.msg-more-wrapper');
  if (!wrapper) return;
  const menu = wrapper.querySelector('.msg-more-menu');
  if (!menu) return;
  const wasOpen = menu.classList.contains('open');
  document.querySelectorAll('.msg-more-menu.open, .msg-react-popup.open').forEach(el => el.classList.remove('open'));
  if (!wasOpen) {
    menu.classList.add('open');
  }
}

function toggleReactMenu(btnEl) {
  const wrapper = btnEl.closest('.msg-react-wrapper');
  if (!wrapper) return;
  const popup = wrapper.querySelector('.msg-react-popup');
  if (!popup) return;
  const wasOpen = popup.classList.contains('open');
  document.querySelectorAll('.msg-more-menu.open, .msg-react-popup.open').forEach(el => el.classList.remove('open'));
  if (!wasOpen) {
    popup.classList.add('open');
  }
}

async function sendReaction(msgKey, emoji) {
  document.querySelectorAll('.msg-react-popup.open').forEach(el => el.classList.remove('open'));
  const msg = messageStore.get(msgKey);
  if (!msg) return;

  const postData = new URLSearchParams();
  postData.append('action', 'react');
  postData.append('targetTimestamp', String(msg.timestamp));
  postData.append('targetSenderId', msg.senderId || '');
  postData.append('emoji', emoji);

  try {
    const resp = await fetch('api/chat', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
      body: postData.toString()
    });
    const res = await resp.json();
    if (res.success) {
      if (res.reactions) {
        reactionStore.set(msgKey, res.reactions);
        renderReactionsForMessage(msgKey);
      }
      pollMessages();
    }
  } catch (e) {
  }
}

function syncAllReactions(reactionsObj) {
  if (!reactionsObj || typeof reactionsObj !== 'object') return;
  const seenKeys = new Set();
  Object.keys(reactionsObj).forEach(msgKey => {
    seenKeys.add(msgKey);
    reactionStore.set(msgKey, reactionsObj[msgKey]);
    renderReactionsForMessage(msgKey);
  });
  // Xoa reaction neu tin nhan khong con reaction nao
  reactionStore.forEach((_, msgKey) => {
    if (!seenKeys.has(msgKey)) {
      reactionStore.delete(msgKey);
      renderReactionsForMessage(msgKey);
    }
  });
}

function renderReactionsForMessage(msgKey) {
  const parts = msgKey.split('_');
  const ts = parts[0];
  const senderId = parts.slice(1).join('_');
  const domId = getMessageDomId(ts, senderId);
  const el = document.getElementById('react-' + domId);
  if (!el) return;

  const data = reactionStore.get(msgKey);
  const counts = (data && data.counts) ? data.counts : {};
  const myReaction = (data && data.myReaction) ? data.myReaction : '';
  const entries = Object.entries(counts).filter(([, c]) => c > 0);

  if (entries.length === 0) {
    el.innerHTML = '';
    el.style.display = 'none';
    return;
  }

  el.style.display = 'flex';
  el.innerHTML = entries.map(([emoji, count]) => {
    const activeClass = (emoji === myReaction) ? ' active' : '';
    return `<button type="button" class="reaction-pill${activeClass}" onclick="sendReaction('${escapeHtml(msgKey)}','${emoji}')">${emoji} <span>${count}</span></button>`;
  }).join('');
}
