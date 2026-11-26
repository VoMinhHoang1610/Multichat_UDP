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
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no, viewport-fit=cover">
    <title><%= room.getRoomName() %> - UDP Multichat Enterprise</title>
    <!-- Dynamic cache-busting -->
    <link rel="stylesheet" href="css/style.css?v=6.0.<%= System.currentTimeMillis() %>">
    <script src="js/chat.js?v=6.0.<%= System.currentTimeMillis() %>" defer></script>
</head>
<body class="in-chat">

<!-- APP ROOT CONTAINER -->
<div class="chat-app-root">

    <!-- ========================================================
         MAIN CHAT COLUMN (100% on Mobile, Responsive Hub on Desktop)
         ======================================================== -->
    <main class="chat-main-column">

        <!-- 1. TOP NAVBAR (Compact 54px) -->
        <header class="chat-navbar">
            <div class="header-left">
                <!-- Back to Lobby -->
                <a href="index.jsp" class="btn-back-lobby" title="Trở về Sảnh Chờ">
                    <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                        <polyline points="15 18 9 12 15 6"></polyline>
                    </svg>
                    <span class="desktop-back-label">Sảnh Chờ</span>
                </a>

                <!-- Room Identity Info -->
                <div class="room-identity" onclick="toggleSidebarPanel()" title="Nhấp xem chi tiết phòng">
                    <div class="room-avatar-badge">
                        <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                            <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                            <circle cx="9" cy="7" r="4"></circle>
                            <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
                            <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
                        </svg>
                    </div>

                    <div class="room-meta-group">
                        <div class="room-name-row">
                            <h1 class="room-title-heading" title="<%= room.getRoomName() %>"><%= room.getRoomName() %></h1>
                            <span class="room-code-pill" onclick="event.stopPropagation(); copyRoomCode('<%= room.getRoomCode() %>')" title="Nhấp để sao chép mã phòng">
                                #<%= room.getRoomCode() %>
                                <svg xmlns="http://www.w3.org/2000/svg" width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                    <rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect>
                                    <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path>
                                </svg>
                            </span>
                            <% if (isAdmin) { %>
                            <span class="badge badge-admin-crown desktop-only" style="font-size: 9.5px; padding: 1px 5px;">👑 Admin</span>
                            <% } %>
                        </div>

                        <div class="room-sub-row">
                            <span class="pulse-dot"></span>
                            <span id="headerOnlineText"><%= room.getUserCount() %> người trực tuyến</span>
                            <span class="desktop-only">•</span>
                            <span class="desktop-only multicast-pill">Socket: <%= room.getMulticastIp() %>:<%= room.getMulticastPort() %></span>
                        </div>
                    </div>
                </div>
            </div>

            <div class="header-right">
                <!-- Theme toggle button -->
                <button type="button" class="header-action-btn" onclick="toggleTheme()" title="Đổi giao diện Sáng / Tối">
                    <svg xmlns="http://www.w3.org/2000/svg" width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
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

                <!-- Toggle Sidebar Button (Opens drawer on mobile, toggles sidebar on desktop) -->
                <button type="button" class="header-action-btn toggle-sidebar-btn" id="btnToggleSidebar" onclick="toggleSidebarPanel()" title="Thông tin phòng &amp; Thành viên">
                    <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <circle cx="12" cy="12" r="10"></circle>
                        <line x1="12" y1="16" x2="12" y2="12"></line>
                        <line x1="12" y1="8" x2="12.01" y2="8"></line>
                    </svg>
                    <span class="online-counter-badge" id="headerBadgeCount"><%= room.getUserCount() %></span>
                </button>
            </div>
        </header>

        <!-- 2. ROOM LOCKED BANNER (If room is locked) -->
        <div id="roomLockedBanner" style="display: <%= "LOCKED".equalsIgnoreCase(room.getStatus()) ? "block" : "none" %>;" class="room-locked-bar">
            🔒 Phòng chat hiện đang bị Quản trị viên tạm khóa. Bạn chỉ có thể xem nội dung.
        </div>

        <!-- 3. CHAT MESSAGES SCROLL CONTAINER (Bounded width on desktop, 100% on mobile) -->
        <div class="chat-messages-container" id="chatMessagesContainer">
            <div class="chat-messages" id="chatMessages">
                <div class="msg-row system">
                    <div class="msg-bubble">
                        Phòng chat UDP Multicast <strong><%= room.getRoomName() %></strong> đã kết nối. Chào mừng <strong><%= user.getUsername() %></strong>!
                    </div>
                </div>
            </div>
        </div>

        <!-- 4. CHAT COMPOSER DOCK (Centered with max-width on desktop, edge-to-edge on mobile) -->
        <footer class="chat-composer-area">
            <div class="composer-max-container">
                <!-- Whisper Indicator Chip -->
                <div id="whisperIndicator" class="whisper-indicator" style="display: none;">
                    <span class="whisper-text">
                        <svg xmlns="http://www.w3.org/2000/svg" width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                            <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
                            <path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
                        </svg>
                        Đang nhắn riêng cho: <strong id="whisperTargetName"></strong>
                    </span>
                    <button type="button" class="whisper-clear-btn" onclick="clearWhisper()" title="Hủy nhắn riêng">✕</button>
                </div>

                <!-- Floating Composer Dock -->
                <div class="composer-floating-dock">
                    <input type="file" id="fileUploadInput" style="display: none;" onchange="uploadFile(this)">
                    <input type="file" id="folderUploadInput" style="display: none;" webkitdirectory directory multiple onchange="uploadFolder(this)">

                    <!-- Attachment Plus Button -->
                    <button type="button" class="composer-btn" id="btnToggleActionSheet" onclick="toggleComposerActionSheet()" title="Đính kèm tệp, thư mục ZIP, hoặc nhắn riêng">
                        <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                            <line x1="12" y1="5" x2="12" y2="19"></line>
                            <line x1="5" y1="12" x2="19" y2="12"></line>
                        </svg>
                    </button>

                    <!-- Quick Sticker Button -->
                    <button type="button" class="composer-btn" id="btnTogglePicker" onclick="togglePicker()" title="Sticker &amp; Emoji">
                        <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                            <circle cx="12" cy="12" r="10"></circle>
                            <path d="M8 14s1.5 2 4 2 4-2 4-2"></path>
                            <line x1="9" y1="9" x2="9.01" y2="9"></line>
                            <line x1="15" y1="9" x2="15.01" y2="9"></line>
                        </svg>
                    </button>

                    <!-- Desktop Inline Recipient Select (visible on Desktop for quick access) -->
                    <div class="desktop-recipient-wrapper desktop-only">
                        <select id="desktopRecipientSelect" class="desktop-recipient-select" onchange="onDesktopRecipientChange()" title="Chọn người nhận tin nhắn">
                            <option value="">Cả phòng (Tất cả)</option>
                        </select>
                    </div>

                    <!-- Input Textarea Wrapper -->
                    <div class="chat-input-wrapper">
                        <textarea id="chatInput" rows="1" placeholder="Nhập tin nhắn... (Enter để gửi, Shift+Enter để xuống dòng)"></textarea>
                    </div>

                    <!-- Send Button -->
                    <button type="button" class="chat-send-btn" onclick="sendChatMessage()" title="Gửi tin nhắn (Enter)">
                        <svg xmlns="http://www.w3.org/2000/svg" width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                            <line x1="22" y1="2" x2="11" y2="13"></line>
                            <polygon points="22 2 15 22 11 13 2 9 22 2"></polygon>
                        </svg>
                    </button>
                </div>

                <!-- Attachment Action Sheet Popup -->
                <div id="composerActionSheet" class="composer-action-sheet" style="display: none;">
                    <div class="action-sheet-title">TÙY CHỌN ĐÍNH KÈM &amp; GỬI</div>
                    
                    <button type="button" class="action-sheet-item" onclick="triggerFileUpload()">
                        <span class="action-sheet-icon">
                            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                <path d="M21.44 11.05l-9.19 9.19a6 6 0 0 1-8.49-8.49l9.19-9.19a4 4 0 0 1 5.66 5.66l-9.2 9.19a2 2 0 0 1-2.83-2.83l8.49-8.48"></path>
                            </svg>
                        </span>
                        <div class="action-sheet-text">
                            <span class="action-title">Gửi Tệp (File)</span>
                            <span class="action-sub">Tài liệu, hình ảnh, văn bản...</span>
                        </div>
                    </button>

                    <button type="button" class="action-sheet-item" onclick="triggerFolderUpload()">
                        <span class="action-sheet-icon">
                            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                <path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"></path>
                            </svg>
                        </span>
                        <div class="action-sheet-text">
                            <span class="action-title">Gửi Thư Mục (ZIP)</span>
                            <span class="action-sub">Tự động nén toàn bộ thư mục</span>
                        </div>
                    </button>

                    <button type="button" class="action-sheet-item" onclick="triggerPickerPopup()">
                        <span class="action-sheet-icon">
                            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                <circle cx="12" cy="12" r="10"></circle>
                                <path d="M8 14s1.5 2 4 2 4-2 4-2"></path>
                                <line x1="9" y1="9" x2="9.01" y2="9"></line>
                                <line x1="15" y1="9" x2="15.01" y2="9"></line>
                            </svg>
                        </span>
                        <div class="action-sheet-text">
                            <span class="action-title">Sticker &amp; Emoji</span>
                            <span class="action-sub">Hình động mèo Dev &amp; biểu cảm</span>
                        </div>
                    </button>

                    <div class="action-sheet-divider"></div>

                    <div class="action-sheet-recipient-row">
                        <label for="recipientSelect" class="action-sheet-label">GỬI CHO:</label>
                        <select id="recipientSelect" class="recipient-select" onchange="onRecipientChange()">
                            <option value="">Cả phòng (Tất cả)</option>
                        </select>
                    </div>
                </div>

                <!-- Sticker & Emoji Picker Popup -->
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
            </div>
        </footer>
    </main>

    <!-- BACKDROP FOR MOBILE SLIDE-OUT DRAWER -->
    <div class="sidebar-backdrop" id="sidebarBackdrop" onclick="closeSidebarDrawer()"></div>

    <!-- ========================================================
         RIGHT SIDEBAR PANEL (ALWAYS DOCKED ON DESKTOP, DRAWER ON MOBILE)
         ======================================================== -->
    <aside class="chat-sidebar-panel" id="chatSidebarPanel">
        <!-- Panel Header -->
        <div class="panel-header">
            <div class="panel-header-title">
                <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <circle cx="12" cy="12" r="10"></circle>
                    <line x1="12" y1="16" x2="12" y2="12"></line>
                    <line x1="12" y1="8" x2="12.01" y2="8"></line>
                </svg>
                <span>Chi Tiết Phòng Chat</span>
            </div>
            <button type="button" class="panel-close-btn" onclick="closeSidebarDrawer()" title="Thu gọn bảng">✕</button>
        </div>

        <div class="panel-scroll-body">
            <!-- 1. Room Overview Card -->
            <div class="panel-card room-overview-card">
                <div class="room-badge-icon">
                    <svg xmlns="http://www.w3.org/2000/svg" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path>
                    </svg>
                </div>
                <h3 class="panel-room-name" title="<%= room.getRoomName() %>"><%= room.getRoomName() %></h3>
                <div class="panel-room-pills">
                    <span class="panel-code-chip" onclick="copyRoomCode('<%= room.getRoomCode() %>')" title="Nhấp sao chép mã phòng">
                        #<%= room.getRoomCode() %>
                        <svg xmlns="http://www.w3.org/2000/svg" width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                            <rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect>
                            <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path>
                        </svg>
                    </span>
                    <span class="badge <%= "ACTIVE".equals(room.getStatus()) ? "badge-online" : "badge-admin-crown" %>" id="roomStatusBadge">
                        <%= room.getStatus() %>
                    </span>
                </div>

                <div class="panel-network-box">
                    <div class="net-row">
                        <span class="net-label">Multicast IP:</span>
                        <code class="net-val"><%= room.getMulticastIp() %></code>
                    </div>
                    <div class="net-row">
                        <span class="net-label">Cổng (Port):</span>
                        <code class="net-val"><%= room.getMulticastPort() %></code>
                    </div>
                    <div class="net-row">
                        <span class="net-label">Giao thức:</span>
                        <span class="net-val" style="color: var(--primary); font-weight: 700;">UDP Multicast IPv4</span>
                    </div>
                </div>
            </div>

            <!-- 2. Admin Command Deck (Visible only if Admin) -->
            <% if (isAdmin) { %>
            <div class="panel-section admin-deck-section">
                <div class="panel-section-title">
                    <span>QUẢN TRỊ VIÊN PHÒNG</span>
                    <span class="badge badge-admin-crown" style="font-size: 9px; padding: 1px 5px;">👑 Admin Control</span>
                </div>
                <div class="admin-actions-grid">
                    <a href="admin.jsp?room=<%= room.getRoomCode() %>" class="btn btn-secondary btn-sm admin-btn" title="Xem danh sách thành viên chi tiết &amp; phân quyền">
                        <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                            <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                            <circle cx="9" cy="7" r="4"></circle>
                        </svg>
                        <span>Bảng Quản Trị</span>
                    </a>

                    <a href="api/admin?action=export_csv&roomCode=<%= room.getRoomCode() %>" class="btn btn-primary btn-sm admin-btn" target="_blank" title="Xuất file CSV danh sách thành viên">
                        <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
                            <polyline points="7 10 12 15 17 10"></polyline>
                            <line x1="12" y1="15" x2="12" y2="3"></line>
                        </svg>
                        <span>Xuất CSV (Excel)</span>
                    </a>

                    <button type="button" class="btn btn-secondary btn-sm admin-btn" onclick="adminClearChat()" title="Xóa toàn bộ lịch sử tin nhắn trong phòng">
                        <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                            <polyline points="3 6 5 6 21 6"></polyline>
                            <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                        </svg>
                        <span>Xóa Lịch Sử Chat</span>
                    </button>

                    <button type="button" class="btn btn-danger btn-sm admin-btn" onclick="adminToggleLock()" title="Tạm khóa / Mở khóa phòng chat">
                        <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                            <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
                            <path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
                        </svg>
                        <span>Tạm Khóa / Mở Khóa</span>
                    </button>
                </div>
            </div>
            <% } %>

            <!-- 3. Members Directory (Divided into Admins and Members) -->
            <div class="panel-section members-section">
                <div class="panel-section-title">
                    <span>THÀNH VIÊN TRỰC TUYẾN</span>
                    <span class="badge badge-online" id="onlineCount"><%= room.getUserCount() %></span>
                </div>
                <div class="members-container" id="membersList">
                    <!-- Populated dynamically by chat.js -->
                </div>
            </div>
        </div>

        <!-- 4. Current User Deck Footer (Docked at bottom of sidebar) -->
        <div class="panel-footer-user">
            <div class="user-profile-tile" onclick="openProfileModal()" title="Cài đặt hồ sơ cá nhân">
                <div class="avatar-wrapper">
                    <img id="myAvatar" src="<%= user.getAvatarUrl() %>" class="member-avatar" alt="Avatar">
                    <span class="avatar-badge"></span>
                </div>
                <div class="user-meta">
                    <div id="myName" class="user-name-text"><%= user.getUsername() %></div>
                    <div class="user-role-badge">
                        <% if (isAdmin) { %>
                        <span class="badge badge-admin-crown" style="font-size: 9px; padding: 1px 4px;">👑 Quản trị viên</span>
                        <% } else { %>
                        <span class="badge badge-member-mini">Thành viên</span>
                        <% } %>
                    </div>
                </div>
            </div>
            <div class="user-actions">
                <button type="button" class="btn-icon" onclick="openProfileModal()" title="Cài đặt tài khoản" style="width: 32px; height: 32px;">
                    <svg xmlns="http://www.w3.org/2000/svg" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <circle cx="12" cy="12" r="3"></circle>
                        <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"></path>
                    </svg>
                </button>
                <button type="button" class="btn-icon" onclick="leaveRoom()" title="Rời phòng chat" style="width: 32px; height: 32px; color: var(--danger);">
                    <svg xmlns="http://www.w3.org/2000/svg" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path>
                        <polyline points="16 17 21 12 16 7"></polyline>
                        <line x1="21" y1="12" x2="9" y2="12"></line>
                    </svg>
                </button>
            </div>
        </div>
    </aside>

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

<!-- LIGHTBOX MODAL (XEM ẢNH PHÓNG TO) -->
<div id="lightboxModal" class="modal-overlay" style="display: none;" onclick="closeLightbox()">
    <div style="max-width: 90vw; max-height: 90vh; position: relative;">
        <img id="lightboxImage" src="" style="max-width: 100%; max-height: 85vh; border-radius: var(--radius-lg); box-shadow: var(--shadow-card);" alt="Ảnh phóng to">
    </div>
</div>

<script>
    // Responsive Sidebar Toggle
    function toggleSidebarPanel() {
        if (window.innerWidth <= 991) {
            // Mobile: Toggle off-canvas drawer
            const drawer = document.getElementById('chatSidebarPanel');
            const backdrop = document.getElementById('sidebarBackdrop');
            if (!drawer) return;
            const isOpen = drawer.classList.contains('drawer-open');
            if (isOpen) {
                drawer.classList.remove('drawer-open');
                if (backdrop) backdrop.classList.remove('open');
            } else {
                drawer.classList.add('drawer-open');
                if (backdrop) backdrop.classList.add('open');
            }
        } else {
            // Desktop: Toggle collapse/expand of right panel
            const root = document.querySelector('.chat-app-root');
            if (root) {
                root.classList.toggle('sidebar-collapsed');
            }
        }
    }

    function closeSidebarDrawer() {
        const drawer = document.getElementById('chatSidebarPanel');
        const backdrop = document.getElementById('sidebarBackdrop');
        if (drawer) drawer.classList.remove('drawer-open');
        if (backdrop) backdrop.classList.remove('open');
    }

    // Toggle Composer Action Sheet Popup
    function toggleComposerActionSheet(force) {
        const sheet = document.getElementById('composerActionSheet');
        if (!sheet) return;
        const isShown = sheet.style.display === 'flex';
        const willShow = (force !== undefined) ? force : !isShown;
        sheet.style.display = willShow ? 'flex' : 'none';
        if (willShow) {
            const picker = document.getElementById('pickerPopup');
            if (picker) picker.style.display = 'none';
        }
    }

    function triggerFileUpload() {
        toggleComposerActionSheet(false);
        const f = document.getElementById('fileUploadInput');
        if (f) f.click();
    }

    function triggerFolderUpload() {
        toggleComposerActionSheet(false);
        const d = document.getElementById('folderUploadInput');
        if (d) d.click();
    }

    function triggerPickerPopup() {
        toggleComposerActionSheet(false);
        if (typeof togglePicker === 'function') togglePicker(true);
    }

    function updateWhisperState(targetUserId, targetUserName) {
        const ind = document.getElementById('whisperIndicator');
        const nameEl = document.getElementById('whisperTargetName');
        const select = document.getElementById('recipientSelect');
        const dSelect = document.getElementById('desktopRecipientSelect');

        const cleanId = (targetUserId || '').trim();
        let displayName = (targetUserName || '').trim();

        if (cleanId !== '') {
            if (!displayName && select) {
                const opt = Array.from(select.options).find(o => o.value === cleanId);
                if (opt) displayName = opt.dataset.username || opt.text;
            }
            if (!displayName && dSelect) {
                const opt = Array.from(dSelect.options).find(o => o.value === cleanId);
                if (opt) displayName = opt.dataset.username || opt.text;
            }
            if (!displayName) displayName = cleanId;

            if (typeof setWhisperTarget === 'function') {
                setWhisperTarget(cleanId, displayName);
            }
            if (ind) ind.style.display = 'flex';
            if (nameEl) nameEl.innerText = displayName;
            if (select) select.value = cleanId;
            if (dSelect) dSelect.value = cleanId;
        } else {
            if (typeof setWhisperTarget === 'function') {
                setWhisperTarget('', '');
            }
            if (ind) ind.style.display = 'none';
            if (nameEl) nameEl.innerText = '';
            if (select) select.value = '';
            if (dSelect) dSelect.value = '';
        }
    }

    function clearWhisper() {
        updateWhisperState('', '');
    }

    function onRecipientChange() {
        const select = document.getElementById('recipientSelect');
        if (select) {
            const opt = select.options[select.selectedIndex];
            const uname = opt ? (opt.dataset.username || '') : '';
            updateWhisperState(select.value, uname);
            toggleComposerActionSheet(false);
        }
    }

    function onDesktopRecipientChange() {
        const dSelect = document.getElementById('desktopRecipientSelect');
        if (dSelect) {
            const opt = dSelect.options[dSelect.selectedIndex];
            const uname = opt ? (opt.dataset.username || '') : '';
            updateWhisperState(dSelect.value, uname);
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