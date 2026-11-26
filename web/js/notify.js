/**
 * ===================================================================
 * UDP MULTICHAT - EXPRESSIVE NOTIFICATION & CONFIRM SYSTEM
 * Thay the hoan toan alert() / confirm() mac dinh bang Toast & Modal
 * theo chuan Design Tokens (Light & Dark Mode) va SVG Icon dong bo.
 * ===================================================================
 */
(function (global) {
  const ICONS = {
    success: '<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path><polyline points="22 4 12 14.01 9 11.01"></polyline></svg>',
    error: '<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"></circle><line x1="15" y1="9" x2="9" y2="15"></line><line x1="9" y1="9" x2="15" y2="15"></line></svg>',
    warning: '<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path><line x1="12" y1="9" x2="12" y2="13"></line><line x1="12" y1="17" x2="12.01" y2="17"></line></svg>',
    info: '<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M4.9 19.1C1 15.2 1 8.8 4.9 4.9"></path><path d="M7.8 16.2c-2.3-2.3-2.3-6.1 0-8.5"></path><circle cx="12" cy="12" r="2"></circle><path d="M16.2 7.8c2.3 2.3 2.3 6.1 0 8.5"></path><path d="M19.1 4.9C23 8.8 23 15.2 19.1 19.1"></path></svg>',
    danger: '<svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"></polyline><path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"></path><path d="M10 11v6"></path><path d="M14 11v6"></path></svg>',
    leave: '<svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path><polyline points="16 17 21 12 16 7"></polyline><line x1="21" y1="12" x2="9" y2="12"></line></svg>',
    shield: '<svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path><line x1="12" y1="8" x2="12" y2="12"></line><line x1="12" y1="16" x2="12.01" y2="16"></line></svg>',
    lock: '<svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect><path d="M7 11V7a5 5 0 0 1 10 0v4"></path></svg>',
    eye: '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path><circle cx="12" cy="12" r="3"></circle></svg>',
    close: '<svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>'
  };

  const DEFAULT_TAGS = {
    success: 'Tín hiệu xanh',
    error: 'Trục trặc',
    warning: 'Lưu ý',
    info: 'Sóng UDP'
  };

  function escapeText(str) {
    if (str == null) return '';
    return String(str)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }

  function ensureStack() {
    let stack = document.getElementById('appNotifyStack');
    if (!stack) {
      stack = document.createElement('div');
      stack.id = 'appNotifyStack';
      stack.className = 'notify-stack';
      stack.setAttribute('aria-live', 'polite');
      document.body.appendChild(stack);
    }
    return stack;
  }

  function toast(message, options) {
    const opts = options || {};
    const type = ['success', 'error', 'warning', 'info'].includes(opts.type) ? opts.type : 'info';
    const duration = typeof opts.duration === 'number' ? opts.duration : 5000;
    const tag = opts.tag || DEFAULT_TAGS[type];

    const stack = ensureStack();
    while (stack.children.length >= 4) {
      stack.removeChild(stack.firstChild);
    }

    const card = document.createElement('div');
    card.className = `notify-label-card notify-${type}`;
    card.setAttribute('role', 'status');
    card.innerHTML = `
      <div class="notify-icon-box">${ICONS[type]}</div>
      <div class="notify-body">
        <div class="notify-tag-row">
          <span class="notify-tag-pill">${escapeText(tag)}</span>
          ${opts.code ? `<span class="notify-code-chip">${escapeText(opts.code)}</span>` : ''}
        </div>
        <div class="notify-message">${escapeText(message)}</div>
      </div>
      <button type="button" class="notify-close-btn" aria-label="Đóng thông báo">${ICONS.close}</button>
      <div class="notify-progress" style="animation-duration: ${duration}ms;"></div>
    `;

    const dismiss = () => {
      if (card.dataset.closing) return;
      card.dataset.closing = '1';
      card.classList.remove('is-visible');
      card.classList.add('is-leaving');
      setTimeout(() => {
        if (card.parentNode) card.parentNode.removeChild(card);
      }, 500);
    };

    card.querySelector('.notify-close-btn').addEventListener('click', dismiss);

    let remaining = duration;
    let startedAt = Date.now();
    let timer = setTimeout(dismiss, remaining);

    card.addEventListener('mouseenter', () => {
      clearTimeout(timer);
      remaining = Math.max(0, remaining - (Date.now() - startedAt));
      const prog = card.querySelector('.notify-progress');
      if (prog) prog.style.animationPlayState = 'paused';
    });
    card.addEventListener('mouseleave', () => {
      startedAt = Date.now();
      const prog = card.querySelector('.notify-progress');
      if (prog) prog.style.animationPlayState = 'running';
      timer = setTimeout(dismiss, Math.max(remaining, 800));
    });

    stack.appendChild(card);
    requestAnimationFrame(() => {
      requestAnimationFrame(() => card.classList.add('is-visible'));
    });
    return dismiss;
  }

  function openDialog(config) {
    return new Promise((resolve) => {
      const existing = document.getElementById('appConfirmOverlay');
      if (existing) existing.remove();

      const tone = config.tone || 'primary'; // primary | danger | warning
      const iconKey = config.icon || (tone === 'danger' ? 'danger' : (tone === 'warning' ? 'warning' : 'info'));
      const badgeLabel = config.badge || 'Xác nhận thao tác';
      const title = config.title || 'Bạn chắc chắn chứ?';
      const message = config.message || '';
      const confirmText = config.confirmText || 'Đồng ý';
      const cancelText = config.cancelText || 'Để sau';
      const singleButton = !!config.singleButton;

      const overlay = document.createElement('div');
      overlay.id = 'appConfirmOverlay';
      overlay.className = 'notify-dialog-overlay';
      overlay.innerHTML = `
        <div class="notify-dialog-card tone-${escapeText(tone)}" role="dialog" aria-modal="true">
          <div class="notify-dialog-top">
            <div class="notify-dialog-icon">${ICONS[iconKey] || ICONS.info}</div>
            <span class="notify-dialog-badge">${escapeText(badgeLabel)}</span>
          </div>
          <h3 class="notify-dialog-title">${escapeText(title)}</h3>
          <p class="notify-dialog-message">${escapeText(message)}</p>
          <div class="notify-dialog-actions">
            ${singleButton ? '' : `<button type="button" class="btn btn-secondary notify-btn-cancel">${escapeText(cancelText)}</button>`}
            <button type="button" class="btn ${tone === 'danger' ? 'btn-danger' : 'btn-primary'} notify-btn-confirm">${escapeText(confirmText)}</button>
          </div>
        </div>
      `;

      let settled = false;
      const closeWith = (result) => {
        if (settled) return;
        settled = true;
        document.removeEventListener('keydown', onKey);
        overlay.classList.remove('is-visible');
        setTimeout(() => {
          if (overlay.parentNode) overlay.parentNode.removeChild(overlay);
          resolve(result);
        }, 180);
      };

      const onKey = (e) => {
        if (e.key === 'Escape') closeWith(false);
        if (e.key === 'Enter') closeWith(true);
      };
      document.addEventListener('keydown', onKey);

      overlay.addEventListener('click', (e) => {
        if (e.target === overlay && !singleButton) closeWith(false);
      });

      const cancelBtn = overlay.querySelector('.notify-btn-cancel');
      if (cancelBtn) {
        cancelBtn.addEventListener('click', () => closeWith(false));
      }
      const confirmBtn = overlay.querySelector('.notify-btn-confirm');
      confirmBtn.addEventListener('click', () => closeWith(true));

      document.body.appendChild(overlay);
      requestAnimationFrame(() => {
        overlay.classList.add('is-visible');
        confirmBtn.focus();
      });
    });
  }

  function openPrompt(config) {
    return new Promise((resolve) => {
      const existing = document.getElementById('appConfirmOverlay');
      if (existing) existing.remove();

      const tone = config.tone || 'primary';
      const iconKey = config.icon || 'lock';
      const badgeLabel = config.badge || 'Bảo mật kênh';
      const title = config.title || 'Nhập mã bảo mật';
      const message = config.message || '';
      const placeholder = config.placeholder || 'Nhập mã...';
      const confirmText = config.confirmText || 'Xác nhận';
      const cancelText = config.cancelText || 'Hủy';
      const inputType = config.inputType || 'password';

      const overlay = document.createElement('div');
      overlay.id = 'appConfirmOverlay';
      overlay.className = 'notify-dialog-overlay';
      overlay.innerHTML = `
        <div class="notify-dialog-card tone-${escapeText(tone)}" role="dialog" aria-modal="true">
          <div class="notify-dialog-top">
            <div class="notify-dialog-icon">${ICONS[iconKey] || ICONS.lock}</div>
            <span class="notify-dialog-badge">${escapeText(badgeLabel)}</span>
          </div>
          <h3 class="notify-dialog-title">${escapeText(title)}</h3>
          <p class="notify-dialog-message" style="margin-bottom: 14px;">${escapeText(message)}</p>
          <div class="notify-prompt-input-wrap">
            <input type="${escapeText(inputType)}" class="notify-prompt-input" placeholder="${escapeText(placeholder)}" autocomplete="off" />
            ${inputType === 'password' ? `<button type="button" class="notify-prompt-eye" title="Hiện / Ẩn mã">${ICONS.eye}</button>` : ''}
          </div>
          <div class="notify-dialog-actions" style="margin-top: 18px;">
            <button type="button" class="btn btn-secondary notify-btn-cancel">${escapeText(cancelText)}</button>
            <button type="button" class="btn btn-primary notify-btn-confirm">${escapeText(confirmText)}</button>
          </div>
        </div>
      `;

      const inputEl = overlay.querySelector('.notify-prompt-input');
      const eyeBtn = overlay.querySelector('.notify-prompt-eye');
      if (eyeBtn) {
        eyeBtn.addEventListener('click', () => {
          inputEl.type = (inputEl.type === 'password') ? 'text' : 'password';
          inputEl.focus();
        });
      }

      let settled = false;
      const closeWith = (val) => {
        if (settled) return;
        settled = true;
        document.removeEventListener('keydown', onKey);
        overlay.classList.remove('is-visible');
        setTimeout(() => {
          if (overlay.parentNode) overlay.parentNode.removeChild(overlay);
          resolve(val);
        }, 180);
      };

      const submitVal = () => {
        const val = inputEl.value.trim();
        if (!val) {
          inputEl.classList.add('is-invalid');
          inputEl.focus();
          setTimeout(() => inputEl.classList.remove('is-invalid'), 450);
          return;
        }
        closeWith(val);
      };

      const onKey = (e) => {
        if (e.key === 'Escape') closeWith(null);
        if (e.key === 'Enter') {
          e.preventDefault();
          submitVal();
        }
      };
      document.addEventListener('keydown', onKey);

      overlay.addEventListener('click', (e) => {
        if (e.target === overlay) closeWith(null);
      });
      overlay.querySelector('.notify-btn-cancel').addEventListener('click', () => closeWith(null));
      overlay.querySelector('.notify-btn-confirm').addEventListener('click', submitVal);

      document.body.appendChild(overlay);
      requestAnimationFrame(() => {
        overlay.classList.add('is-visible');
        inputEl.focus();
      });
    });
  }

  const PENDING_KEY = 'udp_pending_notify_queue';

  function defer(message, options) {
    try {
      const raw = sessionStorage.getItem(PENDING_KEY);
      const list = raw ? JSON.parse(raw) : [];
      list.push({ message: message, options: options || {} });
      sessionStorage.setItem(PENDING_KEY, JSON.stringify(list.slice(-3)));
    } catch (e) {
    }
  }

  function flushDeferred() {
    try {
      const raw = sessionStorage.getItem(PENDING_KEY);
      if (!raw) return;
      sessionStorage.removeItem(PENDING_KEY);
      const list = JSON.parse(raw);
      if (Array.isArray(list)) {
        list.forEach((item, idx) => {
          setTimeout(() => toast(item.message, item.options), idx * 140);
        });
      }
    } catch (e) {
    }
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', flushDeferred);
  } else {
    flushDeferred();
  }

  global.AppNotify = {
    toast,
    defer,
    success: (msg, opts) => toast(msg, Object.assign({ type: 'success' }, opts)),
    error: (msg, opts) => toast(msg, Object.assign({ type: 'error' }, opts)),
    warning: (msg, opts) => toast(msg, Object.assign({ type: 'warning' }, opts)),
    info: (msg, opts) => toast(msg, Object.assign({ type: 'info' }, opts)),
    confirm: (opts) => openDialog(Object.assign({ singleButton: false }, opts)),
    modalAlert: (opts) => openDialog(Object.assign({ singleButton: true, confirmText: 'Đã hiểu' }, opts)),
    prompt: (opts) => openPrompt(opts || {})
  };
})(window);
