<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="multichat_udp.RoomManager" %>
<%@ page import="multichat_udp.ChatRoom" %>
<%@ page import="multichat_udp.UserProfile" %>
<%@ page import="multichat_udp.Account" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Collection" %>
<%@ page import="java.util.ArrayList" %>
<%
    Account account = (Account) session.getAttribute("account");
    if (account == null) {
        response.sendRedirect("login.jsp");
        return;
    }

    Boolean isAdminObj = (Boolean) session.getAttribute("isAdmin");
    if (isAdminObj == null || !isAdminObj) {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
%>
<!DOCTYPE html>
<html lang="vi" data-theme="light">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>403 Forbidden - Quyền Truy Cập Bị Từ Chối</title>
    <!-- Cache-busting CSS query -->
    <link rel="stylesheet" href="css/style.css?v=10.0.<%= System.currentTimeMillis() %>">
    <script src="js/notify.js?v=1.0.<%= System.currentTimeMillis() %>" defer></script>
</head>
<body>
<div class="forbidden-container">
    <div class="forbidden-icon">
        <svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="10"></circle>
            <line x1="4.93" y1="4.93" x2="19.07" y2="19.07"></line>
        </svg>
    </div>
    <h1 style="color: var(--danger); font-size: 26px; font-weight: 800; margin-bottom: 8px;">403 FORBIDDEN</h1>
    <h3 style="font-size: 16px; font-weight: 700; margin-bottom: 12px; color: var(--text);">Quyền Truy Cập Bị Từ Chối</h3>
    <p style="color: var(--text-muted); font-size: 13.5px; line-height: 1.6; margin-bottom: 24px;">
        Tài khoản <strong><%= account.getFullName() %> (@<%= account.getUsername() %>)</strong> của bạn có vai trò <strong>MEMBER (Thành Viên)</strong>, không có thẩm quyền truy cập Bảng Điều Khiển Quản Trị Hệ Thống.
    </p>
    <div style="display: flex; gap: 10px; justify-content: center;">
        <a href="index.jsp" class="btn btn-primary" style="padding: 10px 22px;">
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"></path>
                <polyline points="9 22 9 12 15 12 15 22"></polyline>
            </svg>
            Quay Lại Sảnh Chờ
        </a>
    </div>
</div>
</body>
</html>
<%
        return;
    }

    String allFlag = request.getParameter("all");
    String roomCode = request.getParameter("room");
    boolean hasSearchedRoom = (roomCode != null && !roomCode.trim().isEmpty());

    ChatRoom room = null;
    if (!"1".equals(allFlag) && hasSearchedRoom) {
        room = RoomManager.getInstance().getRoom(roomCode.trim());
    }

    Collection<ChatRoom> allRooms = RoomManager.getInstance().getAllRooms();
    List<ChatRoom> roomsList = new ArrayList<>(allRooms);
    roomsList.sort((a, b) -> {
        int cmpUsers = Integer.compare(b.getUserCount(), a.getUserCount());
        if (cmpUsers != 0) return cmpUsers;
        return a.getRoomCode().compareToIgnoreCase(b.getRoomCode());
    });
%>
<!DOCTYPE html>
<html lang="vi" data-theme="light">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Bảng Quản Trị Admin - UDP Multichat Enterprise</title>
    <!-- Cache-busting CSS query -->
    <link rel="stylesheet" href="css/style.css?v=10.0.<%= System.currentTimeMillis() %>">
    <script src="js/notify.js?v=1.0.<%= System.currentTimeMillis() %>" defer></script>
</head>
<body>

<div class="admin-container">
    <div class="header-bar">
        <div>
            <h1 style="font-size: 22px; font-weight: 800; display: flex; align-items: center; gap: 8px;">
                <svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#dc2626" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path>
                </svg>
                BẢNG ĐIỀU KHIỂN QUẢN TRỊ ADMIN (RBAC)
            </h1>
            <p style="font-size: 13px; color: var(--text-muted); margin-top: 4px;">
                Quản trị viên: <strong><%= account.getFullName() %> (@<%= account.getUsername() %>)</strong> • Toàn quyền kiểm soát và xuất báo cáo.
            </p>
        </div>

        <div style="display: flex; gap: 10px; align-items: center;">
            <a href="index.jsp" class="btn btn-secondary">
                <svg xmlns="http://www.w3.org/2000/svg" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"></path>
                    <polyline points="9 22 9 12 15 12 15 22"></polyline>
                </svg>
                Sảnh Chờ
            </a>
            <% if (room != null) { %>
            <a href="admin.jsp?all=1" class="btn btn-secondary" title="Xem danh sách tất cả các phòng">
                <svg xmlns="http://www.w3.org/2000/svg" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <rect x="3" y="3" width="7" height="7"></rect>
                    <rect x="14" y="3" width="7" height="7"></rect>
                    <rect x="14" y="14" width="7" height="7"></rect>
                    <rect x="3" y="14" width="7" height="7"></rect>
                </svg>
                Tất cả phòng
            </a>
            <a href="chat.jsp?room=<%= room.getRoomCode() %>" class="btn btn-secondary">
                <svg xmlns="http://www.w3.org/2000/svg" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path>
                </svg>
                Vào Chat
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
        </div>
    </div>

    <% if (room == null) {
        int totalRooms = roomsList.size();
        int totalOnlineUsers = 0;
        int activeRoomsCount = 0;
        for (ChatRoom r : roomsList) {
            totalOnlineUsers += r.getUserCount();
            if ("ACTIVE".equalsIgnoreCase(r.getStatus())) activeRoomsCount++;
        }
    %>

    <!-- SYSTEM OVERVIEW STATS -->
    <div class="stats-grid admin-overview-stats">
        <div class="stat-card">
            <div class="stat-card-top">
                <span class="stat-label">TỔNG PHÒNG CHAT</span>
                <div class="stat-icon-badge" style="background: var(--primary-light); color: var(--primary);">
                    <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <rect x="3" y="3" width="7" height="7"></rect>
                        <rect x="14" y="3" width="7" height="7"></rect>
                        <rect x="14" y="14" width="7" height="7"></rect>
                        <rect x="3" y="14" width="7" height="7"></rect>
                    </svg>
                </div>
            </div>
            <div class="stat-val" style="color: var(--primary-text);"><%= totalRooms %></div>
        </div>

        <div class="stat-card">
            <div class="stat-card-top">
                <span class="stat-label">ĐANG MỞ / TẠM KHÓA</span>
                <div class="stat-icon-badge" style="background: var(--warning-light); color: var(--warning);">
                    <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
                        <path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
                    </svg>
                </div>
            </div>
            <div class="stat-val"><%= activeRoomsCount %> / <%= totalRooms - activeRoomsCount %></div>
        </div>

        <div class="stat-card">
            <div class="stat-card-top">
                <span class="stat-label">NGƯỜI DÙNG TRỰC TUYẾN</span>
                <div class="stat-icon-badge" style="background: var(--accent-light); color: var(--accent);">
                    <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                        <circle cx="9" cy="7" r="4"></circle>
                        <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
                        <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
                    </svg>
                </div>
            </div>
            <div class="stat-val" style="color: var(--accent-text);"><%= totalOnlineUsers %></div>
        </div>
    </div>

    <!-- SEARCH & FILTER TOOLBAR -->
    <div class="admin-actions-bar">
        <div>
            <div style="font-size: 15.5px; font-weight: 700;">Danh Sách Phòng Chat Đang Tồn Tại</div>
            <div style="font-size: 12.5px; color: var(--text-muted); margin-top: 2px;">Nhấp vào bất kỳ phòng nào để mở bảng quản trị chi tiết, hoặc sử dụng các nút thao tác nhanh bên dưới.</div>
        </div>
        <form method="get" action="admin.jsp" class="admin-search-form">
            <div class="input-group">
                <svg class="input-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <circle cx="11" cy="11" r="8"></circle>
                    <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                </svg>
                <input type="text" id="adminRoomSearchInput" name="room" value="<%= hasSearchedRoom ? roomCode.trim().toUpperCase() : "" %>" placeholder="Tìm tên phòng hoặc nhập mã phòng..." oninput="filterAdminRooms(this.value)" style="font-weight: 600;">
            </div>
            <button type="submit" class="btn btn-primary" style="height: 44px; white-space: nowrap;">Mở theo mã</button>
        </form>
    </div>

    <% if (hasSearchedRoom && !"1".equals(allFlag)) { %>
    <div class="admin-not-found-banner">
        Không tìm thấy phòng có mã <strong>#<%= roomCode.trim().toUpperCase() %></strong>. Vui lòng chọn một phòng đang tồn tại dưới đây.
    </div>
    <% } %>

    <!-- EXISTING ROOMS LIST -->
    <% if (roomsList.isEmpty()) { %>
    <div class="table-card" style="padding: 48px 24px; text-align: center; color: var(--text-muted);">
        Hiện chưa có phòng chat nào trong hệ thống. Hãy tạo phòng mới tại Sảnh Chờ.
    </div>
    <% } else { %>
    <div class="admin-rooms-grid" id="adminRoomsGrid">
        <% for (ChatRoom r : roomsList) {
            boolean isLocked = "LOCKED".equalsIgnoreCase(r.getStatus());
            int msgCount = r.getRecentMessages(1000).size();
            String safeCode = r.getRoomCode();
        %>
        <div class="admin-room-card"
             data-room-search="<%= (r.getRoomCode() + " " + r.getRoomName() + " " + r.getAdminName()).toLowerCase() %>"
             onclick="openRoomAdmin('<%= safeCode %>')">
            <div class="admin-room-card-head">
                <div class="admin-room-title-group">
                    <div class="admin-room-code-row">
                        <span class="room-code-chip">#<%= safeCode %></span>
                        <span class="badge <%= isLocked ? "badge-admin-crown" : "badge-online" %>">
                            <%= isLocked ? "LOCKED" : "ACTIVE" %>
                        </span>
                    </div>
                    <h3 class="admin-room-name"><%= r.getRoomName() %></h3>
                </div>
                <span class="admin-room-enter-hint" title="Đi tới bảng quản trị phòng #<%= safeCode %>">
                    Quản trị
                    <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                        <polyline points="9 18 15 12 9 6"></polyline>
                    </svg>
                </span>
            </div>

            <p class="admin-room-desc"><%= (r.getDescription() != null && !r.getDescription().trim().isEmpty()) ? r.getDescription() : "Phòng chat UDP Multicast" %></p>

            <div class="admin-room-meta">
                <span class="admin-meta-item">
                    <svg xmlns="http://www.w3.org/2000/svg" width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                        <circle cx="9" cy="7" r="4"></circle>
                    </svg>
                    <strong><%= r.getUserCount() %>/<%= r.getMaxUsers() %></strong> online
                </span>
                <span class="admin-meta-item mono">
                    <%= r.getMulticastIp() %>:<%= r.getMulticastPort() %>
                </span>
                <span class="admin-meta-item">
                    <strong><%= msgCount %></strong> tin nhắn
                </span>
            </div>

            <div class="admin-room-actions" onclick="event.stopPropagation();">
                <a href="admin.jsp?room=<%= safeCode %>" class="btn btn-primary btn-sm">
                    Quản trị phòng
                </a>
                <a href="chat.jsp?room=<%= safeCode %>" class="btn btn-secondary btn-sm">
                    Vào Chat
                </a>
                <button type="button" class="btn btn-secondary btn-sm" onclick="toggleRoomLock('<%= safeCode %>')" title="<%= isLocked ? "Mở khóa phòng" : "Tạm khóa phòng" %>">
                    <%= isLocked ? "Mở khóa" : "Khóa phòng" %>
                </button>
                <button type="button" class="btn btn-secondary btn-sm" onclick="clearRoomChat('<%= safeCode %>')" title="Làm sạch lịch sử chat">
                    Xóa chat
                </button>
                <button type="button" class="btn btn-danger-subtle btn-sm" onclick="deleteRoom('<%= safeCode %>')" title="Giải tán và xóa phòng này">
                    Xóa phòng
                </button>
            </div>
        </div>
        <% } %>
    </div>
    <div id="adminRoomsEmptyFilter" class="table-card" style="display: none; padding: 36px; text-align: center; color: var(--text-muted); margin-top: 12px;">
        Không có phòng nào khớp với từ khóa tìm kiếm.
    </div>
    <% } %>
    <% } else { %>

    <!-- 4 MODERN KPI STATS CARDS -->
    <div class="stats-grid">
        <div class="stat-card">
            <div class="stat-card-top">
                <span class="stat-label">MÃ PHÒNG CHAT</span>
                <div class="stat-icon-badge" style="background: var(--primary-light); color: var(--primary);">
                    <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <line x1="4" y1="9" x2="20" y2="9"></line>
                        <line x1="4" y1="15" x2="20" y2="15"></line>
                        <line x1="10" y1="3" x2="8" y2="21"></line>
                        <line x1="16" y1="3" x2="14" y2="21"></line>
                    </svg>
                </div>
            </div>
            <div class="stat-val mono" style="color: var(--primary-text);">#<%= room.getRoomCode() %></div>
        </div>

        <div class="stat-card">
            <div class="stat-card-top">
                <span class="stat-label">SOCKET UDP MULTICAST</span>
                <div class="stat-icon-badge" style="background: var(--whisper-bg); color: var(--whisper);">
                    <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M4.9 19.1C1 15.2 1 8.8 4.9 4.9"></path>
                        <path d="M7.8 16.2c-2.3-2.3-2.3-6.1 0-8.5"></path>
                        <circle cx="12" cy="12" r="2"></circle>
                        <path d="M16.2 7.8c2.3 2.3 2.3 6.1 0 8.5"></path>
                        <path d="M19.1 4.9C23 8.8 23 15.2 19.1 19.1"></path>
                    </svg>
                </div>
            </div>
            <div class="stat-val mono" style="font-size: 16px;">
                <%= room.getMulticastIp() %>:<%= room.getMulticastPort() %>
            </div>
        </div>

        <div class="stat-card">
            <div class="stat-card-top">
                <span class="stat-label">THÀNH VIÊN ONLINE</span>
                <div class="stat-icon-badge" style="background: var(--accent-light); color: var(--accent);">
                    <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                        <circle cx="9" cy="7" r="4"></circle>
                        <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
                        <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
                    </svg>
                </div>
            </div>
            <div class="stat-val" style="color: var(--accent-text);"><%= room.getUserCount() %> / <%= room.getMaxUsers() %></div>
        </div>

        <div class="stat-card">
            <div class="stat-card-top">
                <span class="stat-label">TRẠNG THÁI PHÒNG</span>
                <div class="stat-icon-badge" style="background: var(--warning-light); color: var(--warning);">
                    <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
                        <path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
                    </svg>
                </div>
            </div>
            <div class="stat-val" style="margin-top: 2px;">
                <span class="badge <%= "ACTIVE".equals(room.getStatus()) ? "badge-online" : "badge-admin-crown" %>" id="adminRoomStatus" style="font-size: 13px; padding: 4px 12px;">
                    <%= room.getStatus() %>
                </span>
            </div>
        </div>
    </div>

    <!-- ACTIONS BAR & REPORTS -->
    <div class="admin-actions-bar">
        <div>
            <div style="font-size: 15px; font-weight: 700;">Xuất File Báo Cáo Dữ Liệu &amp; Thao Tác Phòng</div>
            <div style="font-size: 12.5px; color: var(--text-muted); margin-top: 2px;">Tải danh sách thành viên chi tiết (Excel UTF-8 BOM hoặc Bản in PDF) &amp; điều hành phòng</div>
        </div>
        <div class="admin-actions-buttons">
            <a href="api/admin?action=export_csv&roomCode=<%= room.getRoomCode() %>" class="btn btn-primary" target="_blank" title="Tải file Excel CSV">
                <svg xmlns="http://www.w3.org/2000/svg" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
                    <polyline points="7 10 12 15 17 10"></polyline>
                    <line x1="12" y1="15" x2="12" y2="3"></line>
                </svg>
                Xuất File CSV (Excel)
            </a>
            <a href="api/admin?action=export_html&roomCode=<%= room.getRoomCode() %>" class="btn btn-secondary" target="_blank" title="Xem bản in HTML hoặc lưu PDF">
                <svg xmlns="http://www.w3.org/2000/svg" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <polyline points="6 9 6 2 18 2 18 9"></polyline>
                    <path d="M6 18H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2"></path>
                    <rect x="6" y="14" width="12" height="8"></rect>
                </svg>
                Bản In / Lưu PDF
            </a>
            <button type="button" class="btn btn-secondary" onclick="toggleRoomLock()" title="Tạm khóa hoặc mở khóa phòng">
                <svg xmlns="http://www.w3.org/2000/svg" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
                    <path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
                </svg>
                Khóa / Mở Khóa
            </button>
            <button type="button" class="btn btn-danger-subtle" onclick="clearRoomChat()" title="Làm sạch toàn bộ tin nhắn">
                <svg xmlns="http://www.w3.org/2000/svg" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <polyline points="3 6 5 6 21 6"></polyline>
                    <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                </svg>
                Xóa Lịch Sử Chat
            </button>
            <button type="button" class="btn btn-danger-subtle" onclick="deleteRoom('<%= room.getRoomCode() %>', true)" title="Giải tán và xóa phòng chat này">
                Xóa Phòng
            </button>
        </div>
    </div>

    <!-- ENTERPRISE DATA TABLE -->
    <div class="table-card">
        <div style="padding: 16px 22px; border-bottom: 1px solid var(--border); display: flex; justify-content: space-between; align-items: center;">
            <h3 style="font-size: 15px; font-weight: 700;">Danh Sách Người Dùng Trong Phòng (<%= room.getUserCount() %>)</h3>
            <span class="status-dot-wrapper">
                <span class="pulse-dot"></span>
                <span style="font-size: 12px; color: var(--text-muted); font-weight: 600;">Trực tuyến</span>
            </span>
        </div>
        <div class="table-responsive">
        <table>
            <thead>
            <tr>
                <th>Thành viên</th>
                <th>Vai trò</th>
                <th>Địa chỉ IP</th>
                <th>Thời gian tham gia</th>
                <th>Hoạt động gần nhất</th>
                <th>Số tin</th>
                <th>Thao tác Quản trị</th>
            </tr>
            </thead>
            <tbody>
            <%
                List<UserProfile> userList = room.getActiveUsersList();
                for (UserProfile u : userList) {
                    boolean isUserAdmin = "ADMIN".equalsIgnoreCase(u.getRole());
            %>
            <tr>
                <td>
                    <div style="display: flex; align-items: center; gap: 12px;">
                        <img src="<%= u.getAvatarUrl() %>" style="width: 36px; height: 36px; border-radius: 50%; object-fit: cover; border: 1.5px solid var(--border);" alt="avt">
                        <div>
                            <div style="font-weight: 700; display: flex; align-items: center; gap: 6px;">
                                <span><%= u.getUsername() %></span>
                                <% if (isUserAdmin) { %>
                                <span class="badge badge-admin-crown" style="font-size: 9.5px; padding: 1px 5px;">👑 Admin</span>
                                <% } %>
                            </div>
                            <div style="font-size: 11.5px; color: var(--text-muted);"><%= (u.getBio() != null && !u.getBio().isEmpty()) ? u.getBio() : "Đang hoạt động" %></div>
                        </div>
                    </div>
                </td>
                <td>
                    <span class="badge <%= isUserAdmin ? "badge-admin-crown" : "badge-member" %>">
                        <%= u.getRole() %>
                    </span>
                </td>
                <td><code style="background: var(--surface-alt); padding: 2px 6px; border-radius: 4px; font-size: 12px;"><%= u.getIpAddress() %></code></td>
                <td><%= u.getFormattedJoinTime() %></td>
                <td><%= u.getFormattedLastActive() %></td>
                <td><strong><%= u.getMessageCount() %></strong> tin</td>
                <td>
                    <% if (!isUserAdmin) { %>
                    <div style="display: flex; gap: 6px; flex-wrap: nowrap; white-space: nowrap;">
                        <button type="button" class="btn btn-danger-subtle" style="padding: 6px 10px; font-size: 11.5px;" onclick="kickUser('<%= u.getUserId() %>', '<%= u.getUsername() %>')">
                            Mời ra
                        </button>
                        <button type="button" class="btn btn-secondary" style="padding: 6px 10px; font-size: 11.5px;" onclick="muteUser('<%= u.getUserId() %>')">
                            <%= u.isMuted() ? "🔊 Bật tiếng" : "🔇 Tắt tiếng" %>
                        </button>
                    </div>
                    <% } else { %>
                    <span style="font-size: 12px; color: #b45309; font-weight: 700;">👑 Quản trị viên</span>
                    <% } %>
                </td>
            </tr>
            <% } %>

            <% if (userList.isEmpty()) { %>
            <tr>
                <td colspan="7" style="text-align: center; padding: 44px; color: var(--text-muted);">
                    Chưa có thành viên nào tham gia phòng chat.
                </td>
            </tr>
            <% } %>
            </tbody>
        </table>
        </div>
    </div>

    <% } %>
</div>

<script>
    async function kickUser(uid, uname) {
        const ok = await AppNotify.confirm({
            badge: 'Lệnh Quản trị',
            icon: 'shield',
            tone: 'danger',
            title: 'Mời thành viên rời phòng?',
            message: 'Thành viên [' + uname + '] sẽ ngay lập tức bị ngắt kết nối khỏi phòng qua tín hiệu socket.',
            confirmText: 'Mời ra ngay',
            cancelText: 'Giữ lại'
        });
        if (!ok) return;

        const body = new URLSearchParams({
            action: 'kick',
            roomCode: '<%= (room != null) ? room.getRoomCode() : "" %>',
            userId: uid
        });

        try {
            const resp = await fetch('api/admin', {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: body.toString()
            });
            const res = await resp.json();
            if (res.success) {
                AppNotify.defer(res.message || 'Đã mời thành viên rời phòng.', { type: 'success', tag: 'Đã xử lý' });
                window.location.reload();
            } else {
                AppNotify.error(res.message || 'Không thể mời thành viên này.', { tag: 'Từ chối thao tác' });
            }
        } catch (e) {
            AppNotify.error('Không thể gửi lệnh Admin kick qua mạng.', { tag: 'Lỗi kết nối' });
        }
    }

    async function muteUser(uid) {
        const body = new URLSearchParams({
            action: 'mute',
            roomCode: '<%= (room != null) ? room.getRoomCode() : "" %>',
            userId: uid
        });

        try {
            const resp = await fetch('api/admin', {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: body.toString()
            });
            const res = await resp.json();
            if (res.success) {
                AppNotify.defer(res.message, { type: 'info', tag: res.muted ? 'Đã tắt tiếng' : 'Đã bật tiếng' });
                window.location.reload();
            } else {
                AppNotify.error(res.message || 'Không thể thay đổi trạng thái tiếng.', { tag: 'Thao tác lỗi' });
            }
        } catch (e) {
            AppNotify.error('Lỗi thực thi lệnh Admin mute.', { tag: 'Mất tín hiệu' });
        }
    }

    function openRoomAdmin(code) {
        if (!code) return;
        window.location.href = 'admin.jsp?room=' + encodeURIComponent(code);
    }

    function filterAdminRooms(keyword) {
        const q = (keyword || '').trim().toLowerCase();
        const cards = document.querySelectorAll('#adminRoomsGrid .admin-room-card');
        let visibleCount = 0;
        cards.forEach(card => {
            const hay = card.dataset.roomSearch || '';
            const show = !q || hay.includes(q);
            card.style.display = show ? 'flex' : 'none';
            if (show) visibleCount++;
        });
        const emptyEl = document.getElementById('adminRoomsEmptyFilter');
        if (emptyEl) {
            emptyEl.style.display = (cards.length > 0 && visibleCount === 0) ? 'block' : 'none';
        }
    }

    async function toggleRoomLock(targetRoomCode) {
        const code = targetRoomCode || '<%= (room != null) ? room.getRoomCode() : "" %>';
        if (!code) return;
        const body = new URLSearchParams({
            action: 'toggle_lock',
            roomCode: code
        });

        try {
            const resp = await fetch('api/admin', {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: body.toString()
            });
            const res = await resp.json();
            if (res.success) {
                const isLocked = res.status === 'LOCKED';
                AppNotify.defer(
                    isLocked ? 'Phòng #' + code + ' đã tạm khóa — chỉ Admin được gửi tin.' : 'Phòng #' + code + ' đã mở khóa trở lại!',
                    { type: isLocked ? 'warning' : 'success', tag: isLocked ? 'Đã khóa phòng' : 'Đã mở khóa', code: '#' + code }
                );
                window.location.reload();
            } else {
                AppNotify.error(res.message || 'Không thể chuyển trạng thái phòng.', { tag: 'Thất bại' });
            }
        } catch (e) {
            AppNotify.error('Lỗi chuyển trạng thái phòng.', { tag: 'Mất tín hiệu' });
        }
    }

    async function clearRoomChat(targetRoomCode) {
        const code = targetRoomCode || '<%= (room != null) ? room.getRoomCode() : "" %>';
        if (!code) return;
        const ok = await AppNotify.confirm({
            badge: 'Làm sạch phòng #' + code,
            icon: 'danger',
            tone: 'danger',
            title: 'Quét sạch lịch sử trò chuyện?',
            message: 'Toàn bộ tin nhắn trong phòng #' + code + ' sẽ bị xóa trắng và không thể khôi phục.',
            confirmText: 'Xóa sạch ngay',
            cancelText: 'Giữ lại'
        });
        if (!ok) return;

        const body = new URLSearchParams({
            action: 'clear_chat',
            roomCode: code
        });

        try {
            const resp = await fetch('api/admin', {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: body.toString()
            });
            const res = await resp.json();
            if (res.success) {
                AppNotify.defer(res.message || 'Đã làm sạch lịch sử chat.', { type: 'success', tag: 'Phòng sạch tinh tươm', code: '#' + code });
                window.location.reload();
            } else {
                AppNotify.error(res.message || 'Không thể làm sạch lịch sử.', { tag: 'Thất bại' });
            }
        } catch (e) {
            AppNotify.error('Lỗi làm sạch lịch sử chat.', { tag: 'Mất tín hiệu' });
        }
    }

    async function deleteRoom(targetRoomCode, redirectAll) {
        const code = targetRoomCode || '<%= (room != null) ? room.getRoomCode() : "" %>';
        if (!code) return;
        const ok = await AppNotify.confirm({
            badge: 'Giải tán kênh #' + code,
            icon: 'danger',
            tone: 'danger',
            title: 'Xóa vĩnh viễn phòng chat này?',
            message: 'Kênh UDP Multicast của phòng #' + code + ' sẽ đóng hoàn toàn và toàn bộ thành viên bị ngắt kết nối.',
            confirmText: 'Giải tán phòng',
            cancelText: 'Hủy'
        });
        if (!ok) return;

        const body = new URLSearchParams({
            action: 'delete_room',
            roomCode: code
        });

        try {
            const resp = await fetch('api/admin', {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: body.toString()
            });
            const res = await resp.json();
            if (res.success) {
                AppNotify.defer(res.message, { type: 'success', tag: 'Đã giải tán phòng', code: '#' + code });
                if (redirectAll) {
                    window.location.href = 'admin.jsp?all=1';
                } else {
                    window.location.reload();
                }
            } else {
                AppNotify.error(res.message || 'Không thể xóa phòng.', { tag: 'Thất bại' });
            }
        } catch (e) {
            AppNotify.error('Lỗi thực thi lệnh xóa phòng.', { tag: 'Mất tín hiệu' });
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
