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
      this.style.height = Math.min(this.scrollHeight, 100) + 'px';
    });
  }

  // Dong picker khi click ra ngoai
  document.addEventListener('click', function(e) {
    const picker = document.getElementById('pickerPopup');
    const pickerBtn = document.getElementById('btnTogglePicker');
    if (picker && !picker.contains(e.target) && pickerBtn && !pickerBtn.contains(e.target)) {
      picker.style.display = 'none';
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
        data.messages.forEach(msg => {
          appendMessage(msg);
          if (msg.timestamp > lastTimestamp) {
            lastTimestamp = msg.timestamp;
          }
          if (currentUser && msg.senderId !== currentUser.userId && msg.type !== 'HEARTBEAT') {
            hasNewFromOther = true;
          }
        });

        if (hasNewFromOther) {
          playNotificationSound();
        }
        scrollToBottom();
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
  if (!container) return;

  const msgId = 'msg-' + msg.timestamp + '-' + (msg.senderId ? msg.senderId.substring(0, 5) : '0');
  if (document.getElementById(msgId)) return;

  const isSelf = currentUser && msg.senderId === currentUser.userId;
  const isWhisper = msg.type === 'WHISPER' || (msg.targetUser && msg.targetUser.trim() !== '');
  const isSystem = msg.type === 'SYSTEM' || msg.type === 'JOIN' || msg.type === 'LEAVE' || msg.type === 'PROFILE_UPDATE';

  const row = document.createElement('div');
  row.id = msgId;
  row.className = `msg-row ${isSystem ? 'system' : (isSelf ? 'self' : 'other')} ${isWhisper ? 'whisper' : ''}`;

  if (isSystem) {
    row.innerHTML = `<div class="msg-bubble">${escapeHtml(msg.content)} <span class="msg-time">${msg.formattedTime || ''}</span></div>`;
    container.appendChild(row);
    return;
  }

  let bodyHtml = '';

  if (msg.type === 'STICKER') {
    let stickerUrl = 'assets/stickers/cat_love.svg';
    if (msg.extraData && msg.extraData.stickerUrl) {
      stickerUrl = msg.extraData.stickerUrl;
    }
    bodyHtml = `<img src="${stickerUrl}" class="sticker-msg" alt="Sticker" />`;

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
  const whisperTag = isWhisper ? `<span class="badge badge-whisper">🔒 Riêng cho: ${escapeHtml(msg.targetUser || 'Bạn')}</span>` : '';

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

  if (isSelf) {
    const selfAdminBadge = (currentUser && currentUser.role === 'ADMIN') ? '<span class="badge badge-admin-crown" style="font-size:10px; margin-left:4px;">👑 Admin</span>' : '';
    row.innerHTML = `
      <div class="msg-bubble">
        <div class="msg-meta">
          ${selfAdminBadge}
          ${whisperTag}
          <span class="msg-time">${msg.formattedTime || ''}</span>
        </div>
        ${bodyHtml}
      </div>
    `;
  } else {
    row.innerHTML = `
      <div class="avatar-wrapper">
        <img src="${avatarSrc}" class="member-avatar" alt="${escapeHtml(msg.senderName)}" />
      </div>
      <div class="msg-bubble">
        <div class="msg-meta">
          <span class="${senderClass}">${escapeHtml(msg.senderName)} ${adminBadge}</span>
          ${whisperTag}
          <span class="msg-time">${msg.formattedTime || ''}</span>
        </div>
        ${bodyHtml}
      </div>
    `;
  }

  container.appendChild(row);
}

// Gui tin nhan text hoac link
async function sendChatMessage() {
  const textarea = document.getElementById('chatInput');
  if (!textarea) return;

  const text = textarea.value.trim();
  if (!text) return;

  const recipientSelect = document.getElementById('recipientSelect');
  const targetUser = recipientSelect ? recipientSelect.value : '';

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

  if (targetUser) {
    postData.append('targetUser', targetUser);
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
    }
  } catch (err) {
    alert('Lỗi kết nối khi gửi tin nhắn UDP.');
  }
}

// Gui Sticker
async function sendSticker(pack, stickerId, stickerUrl) {
  togglePicker(false);
  const postData = new URLSearchParams();
  postData.append('type', 'STICKER');
  postData.append('content', '[Sticker]');
  postData.append('pack', pack);
  postData.append('stickerId', stickerId);
  postData.append('stickerUrl', stickerUrl);

  try {
    await fetch('api/chat?action=send', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
      body: postData.toString()
    });
  } catch (err) {
    alert('Lỗi gửi sticker.');
  }
}

// Chen Emoji vao textarea
function insertEmoji(emoji) {
  const textarea = document.getElementById('chatInput');
  if (textarea) {
    textarea.value += emoji;
    textarea.focus();
  }
}

// Tai len File
async function uploadFile(fileInput) {
  const file = fileInput.files[0];
  if (!file) return;

  const formData = new FormData();
  formData.append('uploadType', 'file');
  formData.append('file', file);

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

  const formData = new FormData();
  formData.append('uploadType', 'folder');
  formData.append('folderName', folderName);

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
  const select = document.getElementById('recipientSelect');

  if (countEl) countEl.innerText = members.length;
  if (!container) return;

  const currentSelectedUser = select ? select.value : '';

  container.innerHTML = '';
  if (select) {
    select.innerHTML = '<option value="">Cả phòng (Tất cả)</option>';
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
        item.addEventListener('click', () => {
          if (select) {
            select.value = u.username;
          }
        });

        if (select) {
          const opt = document.createElement('option');
          opt.value = u.username;
          opt.innerText = `🔒 Nhắn riêng: ${u.username}`;
          if (u.username === currentSelectedUser) {
            opt.selected = true;
          }
          select.appendChild(opt);
        }
      }

      container.appendChild(item);
    });
  };

  renderGroup('👑 QUẢN TRỊ VIÊN (ADMINS)', admins, true);
  renderGroup('👤 THÀNH VIÊN (MEMBERS)', regularMembers, false);
}

function scrollToBottom() {
  const c = document.getElementById('chatMessages');
  if (c) {
    c.scrollTop = c.scrollHeight;
  }
}

function togglePicker(force) {
  const p = document.getElementById('pickerPopup');
  if (!p) return;
  if (typeof force === 'boolean') {
    p.style.display = force ? 'flex' : 'none';
  } else {
    p.style.display = p.style.display === 'flex' ? 'none' : 'flex';
  }
}

function switchPickerTab(tabName) {
  document.querySelectorAll('.picker-tab').forEach(b => b.classList.remove('active'));
  document.querySelectorAll('.picker-panel').forEach(p => p.style.display = 'none');

  const tabBtn = document.getElementById('tabBtn-' + tabName);
  const panel = document.getElementById('pickerPanel-' + tabName);
  if (tabBtn) tabBtn.classList.add('active');
  if (panel) panel.style.display = 'grid';
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
