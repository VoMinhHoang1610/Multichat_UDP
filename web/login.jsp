<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="multichat_udp.Account" %>
<%
    HttpSession sess = request.getSession(false);
    if (sess != null && sess.getAttribute("account") != null) {
        response.sendRedirect("index.jsp");
        return;
    }
%>
<!DOCTYPE html>
<html lang="vi" data-theme="light">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Đăng Nhập &amp; Đăng Ký - UDP Multichat Enterprise</title>
    <!-- Cache-busting CSS query -->
    <link rel="stylesheet" href="css/style.css?v=9.0.<%= System.currentTimeMillis() %>">
</head>
<body>

<!-- Top-Right Theme Toggle -->
<div class="auth-top-actions">
    <button class="btn-icon" onclick="toggleTheme()" title="Chuyển đổi giao diện Sáng / Tối" id="themeToggleBtn">
        <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="5"></circle>
            <line x1="12" y1="1" x2="12" y2="3"></line>
            <line x1="12" y1="21" x2="12" y2="23"></line>
            <line x1="4.22" y1="4.22" x2="5.64" y2="5.64"></line>
            <line x1="18.36" y1="18.36" x2="19.78" y2="19.78"></line>
            <line x1="1" y1="12" x2="3" y2="12"></line>
            <line x1="21" y1="12" x2="23" y2="12"></line>
            <line x1="4.22" y1="19.78" x2="5.64" y2="18.36"></line>
            <line x1="18.36" y1="5.64" x2="19.78" y2="4.22"></line>
        </svg>
    </button>
</div>

<!-- Centered Auth Frame -->
<div class="auth-wrapper">
    <div class="auth-card">
        
        <!-- HEADER -->
        <div class="auth-header">
            <div class="auth-brand-mark">
                <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path>
                </svg>
            </div>
            <h2>UDP Multichat</h2>
            <p>Đăng nhập hoặc tạo tài khoản để tham gia phòng chat UDP Multicast</p>
        </div>

        <!-- SEGMENTED TAB BAR -->
        <div class="auth-tabs-wrapper">
            <div class="auth-tabs">
                <button type="button" class="auth-tab-btn active" id="tabBtnLogin" onclick="switchAuthTab('login')">
                    <svg xmlns="http://www.w3.org/2000/svg" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4"></path>
                        <polyline points="10 17 15 12 10 7"></polyline>
                        <line x1="15" y1="12" x2="3" y2="12"></line>
                    </svg>
                    Đăng Nhập
                </button>
                <button type="button" class="auth-tab-btn" id="tabBtnRegister" onclick="switchAuthTab('register')">
                    <svg xmlns="http://www.w3.org/2000/svg" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                        <circle cx="8.5" cy="7" r="4"></circle>
                        <line x1="20" y1="8" x2="20" y2="14"></line>
                        <line x1="23" y1="11" x2="17" y2="11"></line>
                    </svg>
                    Đăng Ký
                </button>
            </div>
        </div>

        <!-- NOTIFICATION BANNER -->
        <div id="authAlert" class="auth-alert" style="display: none;"></div>

        <!-- TAB 1: FORM DANG NHAP -->
        <div id="authPanelLogin" class="auth-body">
            <form onsubmit="handleLoginSubmit(event)">
                <div class="form-row">
                    <label class="form-label" for="loginUsername">TÊN ĐĂNG NHẬP (USERNAME) *</label>
                    <div class="input-group">
                        <svg class="input-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                            <path d="M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2"></path>
                            <circle cx="12" cy="7" r="4"></circle>
                        </svg>
                        <input type="text" id="loginUsername" placeholder="Ví dụ: admin hoặc user1" required autocomplete="username">
                    </div>
                </div>

                <div class="form-row">
                    <label class="form-label" for="loginPassword">MẬT KHẨU *</label>
                    <div class="input-group">
                        <svg class="input-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                            <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
                            <path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
                        </svg>
                        <input type="password" id="loginPassword" placeholder="Nhập mật khẩu..." required autocomplete="current-password">
                    </div>
                </div>

                <button type="submit" id="btnLoginSubmit" class="btn btn-primary" style="width: 100%; height: 46px; font-size: 14px; margin-top: 6px;">
                    <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4"></path>
                        <polyline points="10 17 15 12 10 7"></polyline>
                        <line x1="15" y1="12" x2="3" y2="12"></line>
                    </svg>
                    Đăng Nhập Vào Hệ Thống
                </button>
            </form>

            <!-- DEMO ACCOUNTS HELPER PILLS -->
            <div class="demo-section">
                <div class="demo-title">Tài khoản mẫu (nhấp để điền nhanh)</div>
                <div class="demo-grid">
                    <div class="demo-pill-card" onclick="quickFill('admin', 'admin123', this)">
                        <div class="demo-role-badge" style="color: var(--warning-text);">Admin</div>
                        <div class="demo-creds">admin / admin123</div>
                    </div>
                    <div class="demo-pill-card" onclick="quickFill('user1', '123456', this)">
                        <div class="demo-role-badge" style="color: var(--primary-text);">Member 1</div>
                        <div class="demo-creds">user1 / 123456</div>
                    </div>
                    <div class="demo-pill-card" onclick="quickFill('user2', '123456', this)">
                        <div class="demo-role-badge" style="color: var(--primary-text);">Member 2</div>
                        <div class="demo-creds">user2 / 123456</div>
                    </div>
                </div>
            </div>
        </div>

        <!-- TAB 2: FORM DANG KY -->
        <div id="authPanelRegister" class="auth-body" style="display: none;">
            <form onsubmit="handleRegisterSubmit(event)">
                <div class="form-row">
                    <label class="form-label" for="regUsername">TÊN ĐĂNG NHẬP (USERNAME) *</label>
                    <div class="input-group">
                        <svg class="input-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                            <path d="M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2"></path>
                            <circle cx="12" cy="7" r="4"></circle>
                        </svg>
                        <input type="text" id="regUsername" placeholder="Ví dụ: dev_hoang" required autocomplete="username" pattern="[a-zA-Z0-9_]{3,30}" title="Từ 3 đến 30 ký tự (chữ cái, chữ số, gạch dưới)">
                    </div>
                    <div class="form-hint">Tên định danh duy nhất trong hệ thống socket UDP.</div>
                </div>

                <div class="form-row">
                    <label class="form-label" for="regFullName">HỌ VÀ TÊN HIỂN THỊ *</label>
                    <div class="input-group">
                        <svg class="input-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                            <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                            <circle cx="12" cy="7" r="4"></circle>
                        </svg>
                        <input type="text" id="regFullName" placeholder="Họ và tên của bạn..." required>
                    </div>
                </div>

                <div class="form-row">
                    <label class="form-label" for="regPassword">MẬT KHẨU *</label>
                    <div class="input-group">
                        <svg class="input-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                            <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
                            <path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
                        </svg>
                        <input type="password" id="regPassword" placeholder="Tối thiểu 6 ký tự..." required autocomplete="new-password" minlength="6">
                    </div>
                </div>

                <div class="form-row">
                    <label class="form-label" for="regEmail">EMAIL LIÊN HỆ</label>
                    <div class="input-group">
                        <svg class="input-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                            <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"></path>
                            <polyline points="22,6 12,13 2,6"></polyline>
                        </svg>
                        <input type="email" id="regEmail" placeholder="example@email.com">
                    </div>
                </div>

                <div class="form-row">
                    <label class="form-label">CHỌN AVATAR ĐẠI DIỆN</label>
                    <div class="avatar-grid">
                        <div class="avatar-choice selected" onclick="selectRegAvatar('assets/avatars/avatar1.svg', this)">
                            <img src="assets/avatars/avatar1.svg" alt="A1">
                        </div>
                        <div class="avatar-choice" onclick="selectRegAvatar('assets/avatars/avatar2.svg', this)">
                            <img src="assets/avatars/avatar2.svg" alt="A2">
                        </div>
                        <div class="avatar-choice" onclick="selectRegAvatar('assets/avatars/avatar3.svg', this)">
                            <img src="assets/avatars/avatar3.svg" alt="A3">
                        </div>
                        <div class="avatar-choice" onclick="selectRegAvatar('assets/avatars/avatar4.svg', this)">
                            <img src="assets/avatars/avatar4.svg" alt="A4">
                        </div>
                        <div class="avatar-choice" onclick="selectRegAvatar('assets/avatars/avatar5.svg', this)">
                            <img src="assets/avatars/avatar5.svg" alt="A5">
                        </div>
                        <div class="avatar-choice" onclick="selectRegAvatar('assets/avatars/avatar6.svg', this)">
                            <img src="assets/avatars/avatar6.svg" alt="A6">
                        </div>
                        <div class="avatar-choice" onclick="selectRegAvatar('assets/avatars/avatar7.svg', this)">
                            <img src="assets/avatars/avatar7.svg" alt="A7">
                        </div>
                        <div class="avatar-choice" onclick="selectRegAvatar('assets/avatars/avatar8.svg', this)">
                            <img src="assets/avatars/avatar8.svg" alt="A8">
                        </div>
                    </div>
                    <input type="hidden" id="regAvatarUrl" value="assets/avatars/avatar1.svg">
                </div>

                <div class="form-hint" style="margin-bottom: 14px;">
                    ℹ️ Tài khoản đăng ký mới sẽ tự động được cấp quyền <strong>MEMBER (Thành Viên)</strong>.
                </div>

                <button type="submit" id="btnRegSubmit" class="btn btn-primary" style="width: 100%; height: 46px; font-size: 14px;">
                    <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                        <circle cx="8.5" cy="7" r="4"></circle>
                        <line x1="20" y1="8" x2="20" y2="14"></line>
                        <line x1="23" y1="11" x2="17" y2="11"></line>
                    </svg>
                    Tạo Tài Khoản Thành Viên Mới
                </button>
            </form>
        </div>

    </div>
</div>

<script>
    function switchAuthTab(tab) {
        const btnLogin = document.getElementById('tabBtnLogin');
        const btnReg = document.getElementById('tabBtnRegister');
        const panelLogin = document.getElementById('authPanelLogin');
        const panelReg = document.getElementById('authPanelRegister');
        const alertEl = document.getElementById('authAlert');
        if (alertEl) alertEl.style.display = 'none';

        if (tab === 'login') {
            btnLogin.classList.add('active');
            btnReg.classList.remove('active');
            panelLogin.style.display = 'block';
            panelReg.style.display = 'none';
        } else {
            btnReg.classList.add('active');
            btnLogin.classList.remove('active');
            panelReg.style.display = 'block';
            panelLogin.style.display = 'none';
        }
    }

    function selectRegAvatar(url, el) {
        document.querySelectorAll('#authPanelRegister .avatar-choice').forEach(a => a.classList.remove('selected'));
        el.classList.add('selected');
        document.getElementById('regAvatarUrl').value = url;
    }

    function showAlert(msg, isSuccess) {
        const a = document.getElementById('authAlert');
        if (!a) return;
        a.innerText = msg;
        a.style.display = 'flex';
        if (isSuccess) {
            a.style.background = '#dcfce7';
            a.style.color = '#15803d';
            a.style.border = '1px solid #86efac';
        } else {
            a.style.background = '#fee2e2';
            a.style.color = '#b91c1c';
            a.style.border = '1px solid #fca5a5';
        }
    }

    function quickFill(user, pass, element) {
        document.getElementById('loginUsername').value = user;
        document.getElementById('loginPassword').value = pass;

        if (element) {
            element.classList.add('flashing');
            setTimeout(() => {
                element.classList.remove('flashing');
            }, 500);
        }

        showAlert('Đã tự động điền: ' + user + '. Nhấp "Đăng Nhập" để truy cập.', true);
    }

    async function handleLoginSubmit(e) {
        e.preventDefault();
        const u = document.getElementById('loginUsername').value.trim();
        const p = document.getElementById('loginPassword').value.trim();
        const btn = document.getElementById('btnLoginSubmit');
        btn.disabled = true;

        try {
            const body = new URLSearchParams({
                action: 'login',
                username: u,
                password: p
            });

            const resp = await fetch('api/auth', {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
                body: body.toString()
            });

            const text = await resp.text();
            let data = null;
            try {
                data = JSON.parse(text);
            } catch (jsonErr) {
                console.error("Server response:", text);
                showAlert('Lỗi phản hồi máy chủ (' + resp.status + ')! Vui lòng thử lại.', false);
                btn.disabled = false;
                return;
            }

            if (data && data.success) {
                showAlert('Đăng nhập thành công! Đang chuyển hướng vào Sảnh chờ...', true);
                setTimeout(() => {
                    window.location.href = 'index.jsp';
                }, 500);
            } else {
                showAlert((data && data.message) ? data.message : 'Đăng nhập không thành công!', false);
                btn.disabled = false;
            }
        } catch (err) {
            console.error(err);
            showAlert('Lỗi kết nối máy chủ khi đăng nhập!', false);
            btn.disabled = false;
        }
    }

    async function handleRegisterSubmit(e) {
        e.preventDefault();
        const u = document.getElementById('regUsername').value.trim();
        const fn = document.getElementById('regFullName').value.trim();
        const p = document.getElementById('regPassword').value.trim();
        const em = document.getElementById('regEmail').value.trim();
        const av = document.getElementById('regAvatarUrl').value;
        const btn = document.getElementById('btnRegSubmit');
        btn.disabled = true;

        try {
            const body = new URLSearchParams({
                action: 'register',
                username: u,
                fullName: fn,
                password: p,
                email: em,
                avatarUrl: av
            });

            const resp = await fetch('api/auth', {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
                body: body.toString()
            });

            const text = await resp.text();
            let data = null;
            try {
                data = JSON.parse(text);
            } catch (jsonErr) {
                console.error("Server response:", text);
                showAlert('Lỗi phản hồi máy chủ (' + resp.status + ')! Vui lòng thử lại.', false);
                btn.disabled = false;
                return;
            }

            if (data && data.success) {
                showAlert('Đăng ký tài khoản thành công! Đang chuyển hướng...', true);
                setTimeout(() => {
                    window.location.href = 'index.jsp';
                }, 600);
            } else {
                showAlert((data && data.message) ? data.message : 'Đăng ký không thành công!', false);
                btn.disabled = false;
            }
        } catch (err) {
            console.error(err);
            showAlert('Lỗi kết nối máy chủ khi đăng ký!', false);
            btn.disabled = false;
        }
    }

    function toggleTheme() {
        const cur = document.documentElement.getAttribute('data-theme');
        const target = cur === 'dark' ? 'light' : 'dark';
        document.documentElement.setAttribute('data-theme', target);
        localStorage.setItem('udp_theme', target);
    }

    const savedTheme = localStorage.getItem('udp_theme');
    if (savedTheme) {
        document.documentElement.setAttribute('data-theme', savedTheme);
    }
</script>

</body>
</html>
