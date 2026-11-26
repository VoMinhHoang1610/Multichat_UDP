<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="multichat_udp.RoomManager" %>
<%@ page import="multichat_udp.ChatRoom" %>
<%@ page import="multichat_udp.Account" %>
<%@ page import="java.util.Collection" %>
<%
    Account account = (Account) session.getAttribute("account");
    if (account == null) {
        response.sendRedirect("login.jsp");
        return;
    }
    boolean isAdmin = Boolean.TRUE.equals(session.getAttribute("isAdmin"));

    RoomManager manager = RoomManager.getInstance();
    Collection<ChatRoom> rooms = manager.getAllRooms();
%>
<!DOCTYPE html>
<html lang="vi" data-theme="light">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Sảnh Chờ Phòng Chat - UDP Multichat Enterprise</title>
    <!-- Cache-busting CSS query -->
    <link rel="stylesheet" href="css/style.css?v=10.0.<%= System.currentTimeMillis() %>">
    <script src="js/notify.js?v=1.0.<%= System.currentTimeMillis() %>" defer></script>
</head>
<body>

<div class="hero-container">

    <!-- USER LOBBY GREETING BAR -->
    <div class="user-lobby-bar">
        <div class="user-lobby-info">
            <div class="user-lobby-avatar-box">
                <img src="<%= account.getAvatarUrl() %>" alt="Avatar" class="user-lobby-avatar">
                <span class="pulse-dot"></span>
            </div>
            <div class="user-lobby-text">
                <h2>
                    <span><%= account.getFullName() %></span>
                    <% if (isAdmin) { %>
                    <span class="badge badge-admin-crown">Quản Trị Viên</span>
                    <% } else { %>
                    <span class="badge badge-member">Thành Viên</span>
                    <% } %>
                </h2>
                <p>
                    <code>@<%= account.getUsername() %></code> • <%= (account.getBio() != null && !account.getBio().isEmpty()) ? account.getBio() : "Đang tham gia hệ thống Multichat UDP Enterprise" %>
                </p>
            </div>
        </div>

        <div class="user-lobby-actions">
            <% if (isAdmin) { %>
            <a href="admin.jsp" class="btn btn-amber" title="Truy cập Bảng Quản Trị Hệ Thống">
                <svg xmlns="http://www.w3.org/2000/svg" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path>
                </svg>
                Dashboard Admin
            </a>
            <% } %>

            <button class="btn-icon" onclick="toggleTheme()" title="Đổi giao diện Sáng / Tối">
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
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

            <button class="btn btn-danger-subtle" onclick="handleLogout()" title="Đăng xuất khỏi tài khoản">
                <svg xmlns="http://www.w3.org/2000/svg" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path>
                    <polyline points="16 17 21 12 16 7"></polyline>
                    <line x1="21" y1="12" x2="9" y2="12"></line>
                </svg>
                Đăng Xuất
            </button>
        </div>
    </div>

    <!-- MAIN LOBBY CARD -->
    <div class="main-card">
        
        <!-- TABS HEADER -->
        <div class="nav-tabs">
            <button type="button" class="nav-tab-btn active" id="tabNavJoin" onclick="switchMainTab('join')">
                <svg xmlns="http://www.w3.org/2000/svg" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4"></path>
                    <polyline points="10 17 15 12 10 7"></polyline>
                    <line x1="15" y1="12" x2="3" y2="12"></line>
                </svg>
                Vào Phòng Chat
            </button>

            <% if (isAdmin) { %>
            <button type="button" class="nav-tab-btn" id="tabNavCreate" onclick="switchMainTab('create')">
                <svg xmlns="http://www.w3.org/2000/svg" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <circle cx="12" cy="12" r="10"></circle>
                    <line x1="12" y1="8" x2="12" y2="16"></line>
                    <line x1="8" y1="12" x2="16" y2="12"></line>
                </svg>
                Khởi Tạo Phòng Mới (Admin)
            </button>
            <% } %>

            <button type="button" class="nav-tab-btn" id="tabNavList" onclick="switchMainTab('list')">
                <svg xmlns="http://www.w3.org/2000/svg" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <rect x="3" y="3" width="7" height="7"></rect>
                    <rect x="14" y="3" width="7" height="7"></rect>
                    <rect x="14" y="14" width="7" height="7"></rect>
                    <rect x="3" y="14" width="7" height="7"></rect>
                </svg>
                Danh Sách Phòng (<%= rooms.size() %>)
            </button>
        </div>

        <!-- TAB 1: VAO PHONG CHAT NHANH -->
        <div id="tab-join" class="tab-content active">
            <form onsubmit="handleJoinSubmit(event)">
                <div class="form-row">
                    <label class="form-label" for="joinRoomCode">CHỌN HOẶC NHẬP MÃ PHÒNG CHAT (ROOM CODE) *</label>
                    <div class="join-inline-row" style="display: flex; gap: 10px;">
                        <div class="input-group" style="flex: 1;">
                            <svg class="input-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                <line x1="4" y1="9" x2="20" y2="9"></line>
                                <line x1="4" y1="15" x2="20" y2="15"></line>
                                <line x1="10" y1="3" x2="8" y2="21"></line>
                                <line x1="16" y1="3" x2="14" y2="21"></line>
                            </svg>
                            <input type="text" id="joinRoomCode" name="roomCode" placeholder="Ví dụ: LTM2026 hoặc CYBER88" required style="font-family: monospace; font-size: 15px; font-weight: bold; text-transform: uppercase;">
                        </div>
                        <button type="submit" class="btn btn-primary" style="height: 44px; padding: 0 22px;">
                            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                <line x1="22" y1="2" x2="11" y2="13"></line>
                                <polygon points="22 2 15 22 11 13 2 9 22 2"></polygon>
                            </svg>
                            Tham Gia Phòng
                        </button>
                    </div>
                    <div class="form-hint">Thông tin người dùng sẽ tự động sử dụng tài khoản: <strong><%= account.getFullName() %> (@<%= account.getUsername() %>)</strong>.</div>
                </div>

                <div style="margin-top: 28px;">
                    <label class="form-label">CÁC PHÒNG CHAT ĐANG MỞ SẴN:</label>
                    <div class="room-grid" style="margin-top: 10px;">
                        <% for (ChatRoom r : rooms) { %>
                        <div class="room-card-item">
                            <div>
                                <div class="room-card-header">
                                    <span class="room-code-tag">#<%= r.getRoomCode() %></span>
                                    <span class="status-dot-wrapper">
                                        <span class="pulse-dot"></span>
                                        <span class="badge <%= "ACTIVE".equals(r.getStatus()) ? "badge-online" : "badge-admin-crown" %>"><%= r.getStatus() %></span>
                                    </span>
                                </div>
                                <h4 class="room-card-title"><%= r.getRoomName() %></h4>
                                <p class="room-card-desc"><%= r.getDescription() %></p>
                            </div>

                            <div>
                                <div class="room-card-meta">
                                    <span>UDP: <%= r.getMulticastIp() %>:<%= r.getMulticastPort() %></span>
                                    <span><%= r.getUserCount() %>/<%= r.getMaxUsers() %> thành viên</span>
                                </div>
                                <div class="room-card-footer">
                                    <button type="button" class="btn btn-primary" style="width: 100%;" onclick="selectAndJoinRoom('<%= r.getRoomCode() %>')">
                                        Tham Gia Phòng
                                        <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                            <polyline points="9 18 15 12 9 6"></polyline>
                                        </svg>
                                    </button>
                                </div>
                            </div>
                        </div>
                        <% } %>
                    </div>
                </div>
            </form>
        </div>

        <!-- TAB 2: KHOI TAO PHONG MOI (ADMIN ONLY) -->
        <% if (isAdmin) { %>
        <div id="tab-create" class="tab-content">
            <form onsubmit="handleCreateSubmit(event)">
                <div class="grid-2">
                    <div class="form-row">
                        <label class="form-label" for="createCode">MÃ PHÒNG (ROOM CODE) *</label>
                        <div class="input-group">
                            <svg class="input-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                <line x1="4" y1="9" x2="20" y2="9"></line>
                                <line x1="4" y1="15" x2="20" y2="15"></line>
                                <line x1="10" y1="3" x2="8" y2="21"></line>
                                <line x1="16" y1="3" x2="14" y2="21"></line>
                            </svg>
                            <input type="text" id="createCode" name="roomCode" placeholder="Ví dụ: LTM888" required style="font-family: monospace; font-weight: bold; text-transform: uppercase;">
                        </div>
                        <div class="form-hint">Mã định danh duy nhất (chữ cái và chữ số).</div>
                    </div>
                    <div class="form-row">
                        <label class="form-label" for="createName">TÊN PHÒNG CHAT *</label>
                        <div class="input-group">
                            <svg class="input-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path>
                            </svg>
                            <input type="text" id="createName" name="roomName" placeholder="Tên phòng chat..." required>
                        </div>
                    </div>
                </div>

                <div class="form-row">
                    <label class="form-label" for="createDesc">MÔ TẢ PHÒNG</label>
                    <input type="text" id="createDesc" name="description" placeholder="Mục đích hoặc thông điệp của phòng...">
                </div>

                <div class="grid-2">
                    <div class="form-row">
                        <label class="form-label" for="createIp">ĐỊA CHỈ UDP MULTICAST (LỚP D) *</label>
                        <input type="text" id="createIp" name="multicastIp" value="224.2.2.3" placeholder="224.0.0.0 - 239.255.255.255" required>
                        <div class="form-hint">Địa chỉ IP nhóm Multicast Socket.</div>
                    </div>
                    <div class="form-row">
                        <label class="form-label" for="createPort">CỔNG UDP (PORT) *</label>
                        <input type="number" id="createPort" name="multicastPort" value="8888" min="1024" max="65535" required>
                    </div>
                </div>

                <div class="form-row">
                    <label class="form-label" for="createMaxUsers">GIỚI HẠN THÀNH VIÊN TỐI ĐA</label>
                    <input type="number" id="createMaxUsers" name="maxUsers" value="50" min="2" max="500">
                </div>

                <button type="submit" id="btnCreateSubmit" class="btn btn-amber" style="width: 100%; height: 46px; font-size: 14px; margin-top: 10px;">
                    <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <circle cx="12" cy="12" r="10"></circle>
                        <line x1="12" y1="8" x2="12" y2="16"></line>
                        <line x1="8" y1="12" x2="16" y2="12"></line>
                    </svg>
                    Khởi Tạo Phòng Chat UDP Mới (Admin)
                </button>
            </form>
        </div>
        <% } %>

        <!-- TAB 3: DANH SACH PHONG CHI TIET -->
        <div id="tab-list" class="tab-content">
            <div class="room-grid">
                <% for (ChatRoom r : rooms) { %>
                <div class="room-card-item">
                    <div>
                        <div class="room-card-header">
                            <span class="room-code-tag">#<%= r.getRoomCode() %></span>
                            <span class="status-dot-wrapper">
                                <span class="pulse-dot"></span>
                                <span class="badge <%= "ACTIVE".equals(r.getStatus()) ? "badge-online" : "badge-admin-crown" %>"><%= r.getStatus() %></span>
                            </span>
                        </div>
                        <h4 class="room-card-title"><%= r.getRoomName() %></h4>
                        <p class="room-card-desc"><%= r.getDescription() %></p>

                        <% if (isAdmin) { %>
                        <div style="margin-top: 12px; padding: 10px 12px; border-radius: var(--radius-sm); background: var(--surface-alt); border: 1px solid var(--border); font-size: 12px; font-family: var(--font-mono); display: flex; flex-direction: column; gap: 4px;">
                            <div>Socket: <strong><%= r.getMulticastIp() %>:<%= r.getMulticastPort() %></strong></div>
                            <div>Admin tạo: <strong><%= r.getAdminName() %></strong></div>
                            <div>Lưu trữ: <strong><%= r.getRecentMessages(1000).size() %></strong> tin nhắn</div>
                        </div>
                        <% } %>
                    </div>

                    <div class="room-card-footer">
                        <button type="button" class="btn btn-primary" style="flex: 1;" onclick="selectAndJoinRoom('<%= r.getRoomCode() %>')">
                            Vào Chat
                        </button>
                        <% if (isAdmin) { %>
                        <a href="admin.jsp?room=<%= r.getRoomCode() %>" class="btn btn-secondary" title="Quản trị phòng chat này">
                            <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                <circle cx="12" cy="12" r="3"></circle>
                                <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"></path>
                            </svg>
                            Quản lý
                        </a>
                        <% } %>
                    </div>
                </div>
                <% } %>
            </div>
        </div>
    </div>
</div>

<script>
    function switchMainTab(tab) {
        document.querySelectorAll('.nav-tab-btn').forEach(b => b.classList.remove('active'));
        document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));

        const targetBtn = document.getElementById(tab === 'join' ? 'tabNavJoin' : (tab === 'create' ? 'tabNavCreate' : 'tabNavList'));
        const targetContent = document.getElementById('tab-' + tab);

        if (targetBtn) targetBtn.classList.add('active');
        if (targetContent) targetContent.classList.add('active');
    }

    async function selectAndJoinRoom(code) {
        document.getElementById('joinRoomCode').value = code;
        await executeJoin(code);
    }

    async function handleJoinSubmit(e) {
        e.preventDefault();
        const code = document.getElementById('joinRoomCode').value.trim();
        await executeJoin(code);
    }

    async function executeJoin(roomCode) {
        if (!roomCode) {
            AppNotify.warning('Bạn quên nhập mã phòng rồi — thử #LTM2026 xem sao!', { tag: 'Thiếu mã phòng' });
            return;
        }

        try {
            const body = new URLSearchParams({
                action: 'join',
                roomCode: roomCode
            });

            const resp = await fetch('api/room', {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
                body: body.toString()
            });

            const data = await resp.json();
            if (data.success) {
                AppNotify.defer('Đã bắt được sóng phòng #' + data.roomCode + '!', { type: 'success', tag: 'Kết nối thành công', code: '#' + data.roomCode });
                window.location.href = 'chat.jsp?room=' + encodeURIComponent(data.roomCode);
            } else {
                AppNotify.error(data.message || 'Không thể vào phòng chat!', { tag: 'Không thể tham gia' });
            }
        } catch (err) {
            AppNotify.error('Sóng UDP đang chập chờn — kiểm tra kết nối rồi thử lại nhé.', { tag: 'Mất tín hiệu' });
        }
    }

    async function handleCreateSubmit(e) {
        e.preventDefault();
        const code = document.getElementById('createCode').value.trim();
        const name = document.getElementById('createName').value.trim();
        const desc = document.getElementById('createDesc').value.trim();
        const ip = document.getElementById('createIp').value.trim();
        const port = document.getElementById('createPort').value.trim();
        const maxUsers = document.getElementById('createMaxUsers').value.trim();

        try {
            const body = new URLSearchParams({
                action: 'create',
                roomCode: code,
                roomName: name,
                description: desc,
                multicastIp: ip,
                multicastPort: port,
                maxUsers: maxUsers
            });

            const resp = await fetch('api/room', {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
                body: body.toString()
            });

            const data = await resp.json();
            if (data.success) {
                AppNotify.defer('Phòng #' + data.roomCode + ' đã lên sóng thành công!', { type: 'success', tag: 'Khởi tạo hoàn tất', code: '#' + data.roomCode });
                window.location.href = 'chat.jsp?room=' + encodeURIComponent(data.roomCode);
            } else {
                AppNotify.error(data.message || 'Không thể tạo phòng chat.', { tag: 'Khởi tạo thất bại' });
            }
        } catch (err) {
            AppNotify.error('Lỗi kết nối khi khởi tạo kênh UDP mới.', { tag: 'Mất tín hiệu' });
        }
    }

    async function handleLogout() {
        const ok = await AppNotify.confirm({
            badge: 'Phiên đăng nhập',
            icon: 'leave',
            tone: 'warning',
            title: 'Tạm biệt, hẹn gặp lại?',
            message: 'Bạn có chắc muốn ngắt kết nối và đăng xuất khỏi tài khoản hiện tại?',
            confirmText: 'Đăng xuất',
            cancelText: 'Ở lại'
        });
        if (!ok) return;
        await fetch('api/auth?action=logout', { method: 'POST' });
        window.location.href = 'login.jsp';
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
