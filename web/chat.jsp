<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="multichat_udp.RoomManager" %>
<%@ page import="multichat_udp.ChatRoom" %>
<%@ page import="multichat_udp.UserProfile" %>
<%@ page import="multichat_udp.Account" %>
<%
    Account account = (Account) session.getAttribute("account");
    if (account == null) {
        response.sendRedirect("login.jsp");
        return;
    }

    String roomCode = request.getParameter("room");
    if (roomCode == null || roomCode.trim().isEmpty()) {
        roomCode = (String) session.getAttribute("roomCode");
    }

    if (roomCode == null) {
        response.sendRedirect("index.jsp");
        return;
    }

    ChatRoom room = RoomManager.getInstance().getRoom(roomCode);
    if (room == null) {
        response.sendRedirect("index.jsp");
        return;
    }

    UserProfile user = (UserProfile) session.getAttribute("user");
    if (user == null) {
        user = new UserProfile(
                account.getUsername(),
                account.getFullName(),
                account.getAvatarUrl(),
                account.getRole(),
                request.getRemoteAddr()
        );
        user.setBio(account.getBio());
        user.setEmail(account.getEmail());
        room.addUser(user);
        session.setAttribute("user", user);
    }

    boolean isAdmin = Boolean.TRUE.equals(session.getAttribute("isAdmin"));
%>
<!DOCTYPE html>
<html lang="vi" data-theme="light">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%= room.getRoomName() %> - UDP Multichat Enterprise</title>
    <!-- Cache-busting CSS query -->
    <link rel="stylesheet" href="css/style.css?v=3.0.<%= System.currentTimeMillis() %>">
    <script src="js/chat.js" defer></script>
</head>
<body class="in-chat">

<!-- Mobile Sidebar Backdrop -->
<div class="sidebar-backdrop" id="sidebarBackdrop" onclick="toggleSidebar(false)"></div>

<div class="chat-app">
    <!-- LEFT SIDEBAR (Slide-out Drawer on Mobile) -->
    <aside class="chat-sidebar" id="chatSidebar">
        <!-- Sidebar Top Header -->
        <div class="sidebar-header">
            <div class="brand-title">
                <div class="brand-icon">
                    <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path>
                    </svg>
                </div>
                <span>UDP Multichat</span>
            </div>
            <div style="display: flex; gap: 6px; align-items: center;">
                <button class="btn-icon" onclick="toggleTheme()" title="Đổi giao diện Sáng / Tối" style="width: 32px; height: 32px;">
                    <svg xmlns="http://www.w3.org/2000/svg" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
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
                <a href="index.jsp" class="btn-icon" title="Quay lại Sảnh Chờ" style="width: 32px; height: 32px;">
                    <svg xmlns="http://www.w3.org/2000/svg" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"></path>
                        <polyline points="9 22 9 12 15 12 15 22"></polyline>
                    </svg>
                </a>
                <button type="button" class="btn-icon sidebar-close-btn" onclick="toggleSidebar(false)" title="Đóng danh sách">
                    <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                        <line x1="18" y1="6" x2="6" y2="18"></line>
                        <line x1="6" y1="6" x2="18" y2="18"></line>
                    </svg>
                </button>
            </div>
        </div>

        <!-- Room Information Card -->
        <div class="room-card">
            <div class="room-card-title">
                <span title="<%= room.getRoomName() %>" style="white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 170px;"><%= room.getRoomName() %></span>
                <span class="room-code-badge" onclick="copyRoomCode('<%= room.getRoomCode() %>')" title="Nhấp để sao chép mã phòng">
                    #<%= room.getRoomCode() %>
                    <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect>
                        <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path>
                    </svg>
                </span>
            </div>
            <div class="room-net-info">
                <span class="pulse-dot"></span>
                <span>Socket: <code><%= room.getMulticastIp() %>:<%= room.getMulticastPort() %></code></span>
            </div>
        </div>

        <!-- Online Members List (Divided into Admins and Members via chat.js) -->
        <div class="sidebar-section-title">
            <span>THÀNH VIÊN TRỰC TUYẾN</span>
            <span class="badge badge-online" id="onlineCount"><%= room.getUserCount() %></span>
        </div>

        <div class="members-container" id="membersList">
            <!-- Dynamic Member items populated by chat.js -->
        </div>

        <!-- Current User Profile Footer -->
        <div class="sidebar-user-footer">
            <div class="user-brief" onclick="openProfileModal()" title="Nhấp để cập nhật thông tin cá nhân">
                <div class="avatar-wrapper">
                    <img id="myAvatar" src="<%= user.getAvatarUrl() %>" class="member-avatar" alt="Avatar">
                    <span class="avatar-badge"></span>
                </div>
                <div style="flex: 1; min-width: 0;">
                    <div id="myName" class="member-name">
                        <%= user.getUsername() %>
                    </div>
                    <div style="display: flex; align-items: center; gap: 4px; margin-top: 2px;">
                        <% if (isAdmin) { %>
                        <span class="badge badge-admin-crown" style="font-size: 9.5px; padding: 1px 5px;">👑 Admin</span>
                        <% } else { %>
                        <span class="badge badge-member-mini">Thành viên</span>
                        <% } %>
                    </div>
                </div>
            </div>

            <button class="btn-icon" onclick="openProfileModal()" title="Cài đặt hồ sơ cá nhân" style="width: 32px; height: 32px;">
                <svg xmlns="http://www.w3.org/2000/svg" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <circle cx="12" cy="12" r="3"></circle>
                    <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"></path>
                </svg>
            </button>

            <button class="btn-icon" onclick="leaveRoom()" title="Rời phòng chat" style="width: 32px; height: 32px; color: var(--danger);">
                <svg xmlns="http://www.w3.org/2000/svg" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path>
                    <polyline points="16 17 21 12 16 7"></polyline>
                    <line x1="21" y1="12" x2="9" y2="12"></line>
                </svg>
            </button>
        </div>
    </aside>

    <!-- MAIN CHAT WORKSPACE -->
    <main class="chat-workspace">
        <!-- Top Navbar -->
        <header class="chat-navbar">
            <div class="navbar-title-group">
                <a href="index.jsp" class="mobile-back-btn" title="Về Sảnh Chờ">
                    <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                        <polyline points="15 18 9 12 15 6"></polyline>
                    </svg>
                </a>
                <div class="navbar-titles-col" style="min-width: 0;">
                    <div style="display: flex; align-items: center; gap: 6px;">
                        <h2 class="room-title-text"><%= room.getRoomName() %></h2>
                        <% if (isAdmin) { %>
                        <span class="badge badge-admin-crown badge-compact">👑 Admin</span>
                        <% } %>
                        <span class="badge <%= "ACTIVE".equals(room.getStatus()) ? "badge-online" : "badge-admin-crown" %> badge-compact" id="roomStatusBadge">
                            <%= room.getStatus() %>
                        </span>
                    </div>
                    <div class="navbar-sub">
                        <span class="pulse-dot"></span>
                        <span>Socket</span>
                        <span>•</span>
                        <span id="roomCodeDisplay" style="font-family: monospace; font-weight: bold; cursor: pointer;" onclick="copyRoomCode('<%= room.getRoomCode() %>')">
                            #<%= room.getRoomCode() %>
                        </span>
                    </div>
                </div>
            </div>

            <!-- NAVBAR ACTIONS (PHÂN QUYỀN RBAC) -->
            <div class="navbar-actions">
                <!-- Mobile Members Drawer Toggle Button -->
                <button type="button" class="btn btn-secondary mobile-members-btn" onclick="toggleSidebar(true)" title="Danh sách thành viên">
                    <svg xmlns="http://www.w3.org/2000/svg" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                        <circle cx="9" cy="7" r="4"></circle>
                        <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
                        <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
                    </svg>
                    <span class="badge badge-online mobile-online-badge" id="mobileOnlineCount"><%= room.getUserCount() %></span>
                </button>
                <% if (isAdmin) { %>
                <button type="button" class="btn btn-secondary" onclick="openAdminModal()" title="Mở bảng điều khiển nhanh phòng chat">
                    <svg xmlns="http://www.w3.org/2000/svg" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path>
                    </svg>
                    Quản Lý Phòng
                </button>

                <a href="admin.jsp?room=<%= room.getRoomCode() %>" class="btn btn-secondary" title="Xem danh sách thành viên chi tiết & quản lý">
                    <svg xmlns="http://www.w3.org/2000/svg" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                        <circle cx="9" cy="7" r="4"></circle>
                        <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
                        <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
                    </svg>
                    Bảng Admin
                </a>

                <a href="api/admin?action=export_csv&roomCode=<%= room.getRoomCode() %>" class="btn btn-primary" title="Tải file CSV danh sách thành viên (Excel UTF-8 BOM)">
                    <svg xmlns="http://www.w3.org/2000/svg" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
                        <polyline points="7 10 12 15 17 10"></polyline>
                        <line x1="12" y1="15" x2="12" y2="3"></line>
                    </svg>
                    Xuất CSV
                </a>
                <% } else { %>
                <a href="index.jsp" class="btn btn-secondary">
                    <svg xmlns="http://www.w3.org/2000/svg" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"></path>
                        <polyline points="9 22 9 12 15 12 15 22"></polyline>
                    </svg>
                    Sảnh Chờ
                </a>
                <% } %>
            </div>
        </header>

        <!-- Room Lock Notice Banner -->
        <div id="roomLockedBanner" style="display: <%= "LOCKED".equalsIgnoreCase(room.getStatus()) ? "block" : "none" %>; padding: 10px 16px; background: #FEE2E2; color: #B91C1C; font-size: 13px; font-weight: 700; text-align: center; border-bottom: 1px solid #FCA5A5;">
            🔒 Phòng chat hiện đang bị Quản trị viên tạm khóa. Bạn chỉ có thể xem nội dung và không thể gửi tin lúc này.
        </div>

        <!-- Messages Stream -->
        <div class="chat-messages" id="chatMessages">
            <div class="msg-row system">
                <div class="msg-bubble">
                    Phòng chat UDP Multicast <strong><%= room.getRoomName() %></strong> đã kết nối. Chào mừng <strong><%= user.getUsername() %></strong>!
                </div>
            </div>
        </div>

        <!-- Floating Modern Input Dock -->
        <footer class="chat-composer-wrapper">
            <div class="composer-floating-dock">
                <div class="composer-toolbar-row">
                    <div class="composer-tools-left">
                        <select id="recipientSelect" class="recipient-select" title="Chọn người nhận tin nhắn">
                            <option value="">Cả phòng (Tất cả)</option>
                        </select>

                        <input type="file" id="fileUploadInput" style="display: none;" onchange="uploadFile(this)">
                        <input type="file" id="folderUploadInput" style="display: none;" webkitdirectory directory multiple onchange="uploadFolder(this)">

                        <button type="button" class="composer-btn" onclick="document.getElementById('fileUploadInput').click()" title="Gửi một tệp đính kèm">
                            <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                <path d="M21.44 11.05l-9.19 9.19a6 6 0 0 1-8.49-8.49l9.19-9.19a4 4 0 0 1 5.66 5.66l-9.2 9.19a2 2 0 0 1-2.83-2.83l8.49-8.48"></path>
                            </svg>
                            Gửi File
                        </button>

                        <button type="button" class="composer-btn" onclick="document.getElementById('folderUploadInput').click()" title="Gửi toàn bộ thư mục (Tự động nén ZIP)">
                            <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                <path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"></path>
                            </svg>
                            Gửi Folder
                        </button>

                        <button type="button" class="composer-btn" id="btnTogglePicker" onclick="togglePicker()" title="Chọn nhãn dán hoặc biểu tượng cảm xúc">
                            <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                <circle cx="12" cy="12" r="10"></circle>
                                <path d="M8 14s1.5 2 4 2 4-2 4-2"></path>
                                <line x1="9" y1="9" x2="9.01" y2="9"></line>
                                <line x1="15" y1="9" x2="15.01" y2="9"></line>
                            </svg>
                            Sticker &amp; Emoji
                        </button>
                    </div>
                </div>

                <div class="input-container">
                    <textarea id="chatInput" class="chat-textarea" placeholder="Nhập tin nhắn... (Enter để gửi, Shift+Enter để xuống dòng)"></textarea>
                    <button type="button" class="chat-send-btn" onclick="sendChatMessage()" title="Gửi tin nhắn (Enter)">
                        <svg xmlns="http://www.w3.org/2000/svg" width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                            <line x1="22" y1="2" x2="11" y2="13"></line>
                            <polygon points="22 2 15 22 11 13 2 9 22 2"></polygon>
                        </svg>
                    </button>
                </div>
            </div>

            <!-- STICKER & EMOJI PICKER POPUP -->
            <div id="pickerPopup" class="picker-popup" style="display: none;">
                <div class="picker-tabs">
                    <button type="button" class="picker-tab active" id="tabBtn-cat" onclick="switchPickerTab('cat')">🐱 Mèo</button>
                    <button type="button" class="picker-tab" id="tabBtn-tech" onclick="switchPickerTab('tech')">🚀 Dev</button>
                    <button type="button" class="picker-tab" id="tabBtn-react" onclick="switchPickerTab('react')">🔥 Cảm xúc</button>
                    <button type="button" class="picker-tab" id="tabBtn-emoji" onclick="switchPickerTab('emoji')">😊 Emoji</button>
                </div>

                <div id="pickerPanel-cat" class="picker-content picker-panel" style="display: grid;">
                    <div class="picker-item" onclick="sendSticker('cat', 'love', 'assets/stickers/cat_love.svg')"><img src="assets/stickers/cat_love.svg" alt="Love"></div>
                    <div class="picker-item" onclick="sendSticker('cat', 'cool', 'assets/stickers/cat_cool.svg')"><img src="assets/stickers/cat_cool.svg" alt="Cool"></div>
                    <div class="picker-item" onclick="sendSticker('cat', 'party', 'assets/stickers/cat_party.svg')"><img src="assets/stickers/cat_party.svg" alt="Party"></div>
                </div>

                <div id="pickerPanel-tech" class="picker-content picker-panel" style="display: none;">
                    <div class="picker-item" onclick="sendSticker('tech', 'rocket', 'assets/stickers/tech_rocket.svg')"><img src="assets/stickers/tech_rocket.svg" alt="Rocket"></div>
                    <div class="picker-item" onclick="sendSticker('tech', 'coffee', 'assets/stickers/tech_coffee.svg')"><img src="assets/stickers/tech_coffee.svg" alt="Coffee"></div>
                    <div class="picker-item" onclick="sendSticker('tech', 'bug', 'assets/stickers/tech_bug.svg')"><img src="assets/stickers/tech_bug.svg" alt="Bug"></div>
                </div>

                <div id="pickerPanel-react" class="picker-content picker-panel" style="display: none;">
                    <div class="picker-item" onclick="sendSticker('react', 'like', 'assets/stickers/react_like.svg')"><img src="assets/stickers/react_like.svg" alt="Like"></div>
                    <div class="picker-item" onclick="sendSticker('react', 'heart', 'assets/stickers/react_heart.svg')"><img src="assets/stickers/react_heart.svg" alt="Heart"></div>
                    <div class="picker-item" onclick="sendSticker('react', 'fire', 'assets/stickers/react_fire.svg')"><img src="assets/stickers/react_fire.svg" alt="Fire"></div>
                </div>

                <div id="pickerPanel-emoji" class="picker-content picker-panel" style="display: none; grid-template-columns: repeat(5, 1fr);">
                    <div class="picker-item" onclick="insertEmoji('😀')">😀</div>
                    <div class="picker-item" onclick="insertEmoji('😂')">😂</div>
                    <div class="picker-item" onclick="insertEmoji('😍')">😍</div>
                    <div class="picker-item" onclick="insertEmoji('😎')">😎</div>
                    <div class="picker-item" onclick="insertEmoji('👍')">👍</div>
                    <div class="picker-item" onclick="insertEmoji('🎉')">🎉</div>
                    <div class="picker-item" onclick="insertEmoji('🔥')">🔥</div>
                    <div class="picker-item" onclick="insertEmoji('🚀')">🚀</div>
                    <div class="picker-item" onclick="insertEmoji('❤️')">❤️</div>
                    <div class="picker-item" onclick="insertEmoji('💯')">💯</div>
                    <div class="picker-item" onclick="insertEmoji('💻')">💻</div>
                    <div class="picker-item" onclick="insertEmoji('☕')">☕</div>
                    <div class="picker-item" onclick="insertEmoji('✨')">✨</div>
                    <div class="picker-item" onclick="insertEmoji('🤝')">🤝</div>
                    <div class="picker-item" onclick="insertEmoji('📚')">📚</div>
                </div>
            </div>
        </footer>
    </main>
</div>

<!-- MODAL CẬP NHẬT THÔNG TIN CÁ NHÂN -->
<div id="profileModal" class="modal-overlay" style="display: none;">
    <div class="modal-box">
        <div class="modal-header">
            <h3>
                <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <circle cx="12" cy="12" r="3"></circle>
                    <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"></path>
                </svg>
                Cập Nhật Thông Tin Cá Nhân
            </h3>
            <button type="button" class="btn-icon" style="width: 32px; height: 32px;" onclick="closeProfileModal()">✕</button>
        </div>
        <form id="profileForm" onsubmit="handleProfileUpdate(event)">
            <div class="modal-body">
                <div class="form-row">
                    <label class="form-label" for="profUsername">TÊN HIỂN THỊ</label>
                    <input type="text" id="profUsername" name="username" value="<%= user.getUsername() %>" required>
                </div>
                <div class="form-row">
                    <label class="form-label" for="profBio">TIỂU SỬ / TRẠNG THÁI</label>
                    <input type="text" id="profBio" name="bio" value="<%= user.getBio() %>" placeholder="Đang online...">
                </div>
                <div class="form-row">
                    <label class="form-label" for="profEmail">EMAIL LIÊN HỆ</label>
                    <input type="email" id="profEmail" name="email" value="<%= user.getEmail() %>" placeholder="example@email.com">
                </div>

                <div class="form-row">
                    <label class="form-label">CHỌN AVATAR MẶC ĐỊNH</label>
                    <div class="avatar-grid">
                        <div class="avatar-choice" onclick="selectProfileAvatar('assets/avatars/avatar1.svg', this)"><img src="assets/avatars/avatar1.svg" alt="A1"></div>
                        <div class="avatar-choice" onclick="selectProfileAvatar('assets/avatars/avatar2.svg', this)"><img src="assets/avatars/avatar2.svg" alt="A2"></div>
                        <div class="avatar-choice" onclick="selectProfileAvatar('assets/avatars/avatar3.svg', this)"><img src="assets/avatars/avatar3.svg" alt="A3"></div>
                        <div class="avatar-choice" onclick="selectProfileAvatar('assets/avatars/avatar4.svg', this)"><img src="assets/avatars/avatar4.svg" alt="A4"></div>
                        <div class="avatar-choice" onclick="selectProfileAvatar('assets/avatars/avatar5.svg', this)"><img src="assets/avatars/avatar5.svg" alt="A5"></div>
                        <div class="avatar-choice" onclick="selectProfileAvatar('assets/avatars/avatar6.svg', this)"><img src="assets/avatars/avatar6.svg" alt="A6"></div>
                        <div class="avatar-choice" onclick="selectProfileAvatar('assets/avatars/avatar7.svg', this)"><img src="assets/avatars/avatar7.svg" alt="A7"></div>
                        <div class="avatar-choice" onclick="selectProfileAvatar('assets/avatars/avatar8.svg', this)"><img src="assets/avatars/avatar8.svg" alt="A8"></div>
                    </div>
                    <input type="hidden" id="profPreset" name="avatarPreset" value="<%= user.getAvatarUrl() %>">
                </div>

                <div class="form-row" style="margin-top: 14px;">
                    <label class="form-label" for="profFile">HOẶC TẢI LÊN ẢNH AVATAR MỚI</label>
                    <input type="file" id="profFile" name="avatarFile" accept="image/*">
                </div>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-secondary" onclick="closeProfileModal()">Hủy</button>
                <button type="submit" class="btn btn-primary">Lưu Thay Đổi</button>
            </div>
        </form>
    </div>
</div>

<!-- MODAL QUẢN TRỊ ADMIN NHANH (CHỈ ADMIN MỚI THẤY) -->
<% if (isAdmin) { %>
<div id="adminModal" class="modal-overlay" style="display: none;">
    <div class="modal-box" style="max-width: 520px;">
        <div class="modal-header">
            <h3>
                <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path>
                </svg>
                Quản Trị Phòng Chat - #<%= room.getRoomCode() %>
            </h3>
            <button type="button" class="btn-icon" style="width: 32px; height: 32px;" onclick="closeAdminModal()">✕</button>
        </div>
        <div class="modal-body">
            <div style="padding: 10px 14px; border-radius: var(--radius-md); background: #FEF3C7; color: #92400E; font-size: 13px; font-weight: 700; margin-bottom: 16px; border: 1px solid #FDE68A;">
                👑 Quản trị viên: <strong><%= account.getFullName() %></strong> (Toàn quyền điều hành)
            </div>

            <div style="font-size: 11px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.6px; color: var(--text-muted); margin-bottom: 8px;">
                XUẤT BÁO CÁO DỮ LIỆU PHÒNG
            </div>
            <div style="display: flex; gap: 10px; margin-bottom: 20px;">
                <a href="api/admin?action=export_csv&roomCode=<%= room.getRoomCode() %>" class="btn btn-primary" style="flex: 1;" target="_blank">
                    <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
                        <polyline points="7 10 12 15 17 10"></polyline>
                        <line x1="12" y1="15" x2="12" y2="3"></line>
                    </svg>
                    File CSV (Excel)
                </a>
                <a href="api/admin?action=export_html&roomCode=<%= room.getRoomCode() %>" class="btn btn-secondary" style="flex: 1;" target="_blank">
                    <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <polyline points="6 9 6 2 18 2 18 9"></polyline>
                        <path d="M6 18H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2"></path>
                        <rect x="6" y="14" width="12" height="8"></rect>
                    </svg>
                    Bản In / PDF
                </a>
            </div>

            <div style="font-size: 11px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.6px; color: var(--text-muted); margin-bottom: 8px;">
                THAO TÁC ĐIỀU HÀNH PHÒNG
            </div>
            <div style="display: flex; flex-direction: column; gap: 8px;">
                <a href="admin.jsp?room=<%= room.getRoomCode() %>" class="btn btn-secondary" style="justify-content: flex-start;">
                    <svg xmlns="http://www.w3.org/2000/svg" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                        <circle cx="9" cy="7" r="4"></circle>
                    </svg>
                    Quản lý danh sách thành viên chi tiết
                </a>
                <button type="button" class="btn btn-secondary" style="justify-content: flex-start;" onclick="adminClearChat()">
                    <svg xmlns="http://www.w3.org/2000/svg" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <polyline points="3 6 5 6 21 6"></polyline>
                        <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                    </svg>
                    Làm sạch lịch sử trò chuyện
                </button>
                <button type="button" class="btn btn-danger" style="justify-content: flex-start;" onclick="adminToggleLock()">
                    <svg xmlns="http://www.w3.org/2000/svg" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
                        <path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
                    </svg>
                    Tạm khóa / Mở khóa phòng chat
                </button>
            </div>
        </div>
    </div>
</div>
<% } %>

<!-- LIGHTBOX MODAL (XEM ẢNH PHÓNG TO) -->
<div id="lightboxModal" class="modal-overlay" style="display: none;" onclick="closeLightbox()">
    <div style="max-width: 90vw; max-height: 90vh; position: relative;">
        <img id="lightboxImage" src="" style="max-width: 100%; max-height: 85vh; border-radius: var(--radius-lg); box-shadow: var(--shadow-card);" alt="Ảnh phóng to">
    </div>
</div>

<script>
    function toggleSidebar(open) {
        const sb = document.getElementById('chatSidebar');
        const bd = document.getElementById('sidebarBackdrop');
        if (!sb) return;
        const willOpen = (open !== undefined) ? open : !sb.classList.contains('open');
        if (willOpen) {
            sb.classList.add('open');
            if (bd) bd.classList.add('open');
        } else {
            sb.classList.remove('open');
            if (bd) bd.classList.remove('open');
        }
    }

    document.addEventListener('DOMContentLoaded', () => {
        const roomData = {
            roomCode: '<%= room.getRoomCode() %>',
            roomName: '<%= room.getRoomName() %>'
        };
        const userData = {
            userId: '<%= user.getUserId() %>',
            username: '<%= user.getUsername() %>',
            avatarUrl: '<%= user.getAvatarUrl() %>',
            role: '<%= user.getRole() %>'
        };
        initChat(roomData, userData);
    });

    function openProfileModal() {
        document.getElementById('profileModal').style.display = 'flex';
    }
    function closeProfileModal() {
        document.getElementById('profileModal').style.display = 'none';
    }

    function selectProfileAvatar(url, el) {
        document.querySelectorAll('#profileModal .avatar-choice').forEach(a => a.classList.remove('selected'));
        el.classList.add('selected');
        document.getElementById('profPreset').value = url;
    }

    async function handleProfileUpdate(e) {
        e.preventDefault();
        const form = document.getElementById('profileForm');
        const formData = new FormData(form);

        try {
            const resp = await fetch('api/profile', {
                method: 'POST',
                body: formData
            });
            const res = await resp.json();
            if (res.success) {
                alert('Cập nhật thông tin thành công!');
                document.getElementById('myName').innerText = res.user.username;
                document.getElementById('myAvatar').src = res.user.avatarUrl;
                currentUser.username = res.user.username;
                currentUser.avatarUrl = res.user.avatarUrl;
                closeProfileModal();
            } else {
                alert(res.message || 'Lỗi cập nhật.');
            }
        } catch (err) {
            alert('Lỗi kết nối khi cập nhật hồ sơ.');
        }
    }

    function openAdminModal() {
        const m = document.getElementById('adminModal');
        if (m) m.style.display = 'flex';
    }
    function closeAdminModal() {
        const m = document.getElementById('adminModal');
        if (m) m.style.display = 'none';
    }

    <% if (isAdmin) { %>
    async function adminClearChat() {
        if (!confirm('Bạn có chắc muốn xóa sạch toàn bộ lịch sử tin nhắn của phòng?')) return;
        const body = new URLSearchParams({
            action: 'clear_chat',
            roomCode: '<%= room.getRoomCode() %>'
        });
        const resp = await fetch('api/admin', { method: 'POST', body: body.toString(), headers: { 'Content-Type': 'application/x-www-form-urlencoded' } });
        const res = await resp.json();
        alert(res.message);
        document.getElementById('chatMessages').innerHTML = '';
        closeAdminModal();
    }

    async function adminToggleLock() {
        const body = new URLSearchParams({
            action: 'toggle_lock',
            roomCode: '<%= room.getRoomCode() %>'
        });
        const resp = await fetch('api/admin', { method: 'POST', body: body.toString(), headers: { 'Content-Type': 'application/x-www-form-urlencoded' } });
        const res = await resp.json();
        alert('Trạng thái phòng hiện tại: ' + res.status);
        const b = document.getElementById('roomStatusBadge');
        if (b) b.innerText = res.status;
        const lockBanner = document.getElementById('roomLockedBanner');
        if (lockBanner) lockBanner.style.display = (res.status === 'LOCKED') ? 'block' : 'none';
        closeAdminModal();
    }
    <% } %>

    async function leaveRoom() {
        if (!confirm('Bạn có muốn rời khỏi phòng chat này và quay về Sảnh chờ?')) return;
        await fetch('api/room', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: 'action=leave'
        });
        window.location.href = 'index.jsp';
    }
</script>

</body>
</html>
