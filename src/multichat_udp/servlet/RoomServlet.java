package multichat_udp.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import multichat_udp.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;

/**
 * Servlet xử lý danh sách phòng, tham gia phòng và khởi tạo phòng chat UDP.
 * Phân quyền dựa trên vai trò tài khoản đăng nhập (ADMIN / MEMBER), loại bỏ hoàn toàn mã PIN phòng.
 */
@WebServlet(name = "RoomServlet", urlPatterns = {"/api/room"})
public class RoomServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = req.getParameter("action");
        if (action == null) action = "list";

        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        RoomManager manager = RoomManager.getInstance();

        if ("list".equalsIgnoreCase(action)) {
            Collection<ChatRoom> allRooms = manager.getAllRooms();
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (ChatRoom r : allRooms) {
                if (!first) sb.append(",");
                sb.append(r.toJson());
                first = false;
            }
            sb.append("]");
            out.print(sb.toString());
        } else if ("info".equalsIgnoreCase(action)) {
            String roomCode = req.getParameter("roomCode");
            ChatRoom room = manager.getRoom(roomCode);
            if (room != null) {
                out.print("{\"success\":true,\"room\":" + room.toJson() + "}");
            } else {
                out.print("{\"success\":false,\"message\":\"Không tìm thấy phòng với mã này.\"}");
            }
        } else {
            out.print("{\"success\":false,\"message\":\"Hành động không hợp lệ.\"}");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        String action = req.getParameter("action");
        RoomManager manager = RoomManager.getInstance();
        HttpSession session = req.getSession(false);

        // ==========================================
        // 1. THAM GIA PHÒNG CHAT (JOIN)
        // ==========================================
        if ("join".equalsIgnoreCase(action) || "login".equalsIgnoreCase(action)) {
            if (session == null || session.getAttribute("account") == null) {
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                out.print("{\"success\":false,\"message\":\"Vui lòng đăng nhập trước khi tham gia phòng chat!\"}");
                return;
            }

            Account account = (Account) session.getAttribute("account");
            String roomCode = req.getParameter("roomCode");

            if (roomCode == null || roomCode.trim().isEmpty()) {
                out.print("{\"success\":false,\"message\":\"Vui lòng nhập mã phòng chat!\"}");
                return;
            }

            String normalizedCode = roomCode.trim().toUpperCase();
            ChatRoom room = manager.getRoom(normalizedCode);

            if (room == null) {
                out.print("{\"success\":false,\"message\":\"Mã phòng " + normalizedCode + " không tồn tại! Vui lòng kiểm tra lại.\"}");
                return;
            }

            // Kiem tra phong bi khoa
            if ("LOCKED".equalsIgnoreCase(room.getStatus()) && !account.isAdmin()) {
                out.print("{\"success\":false,\"message\":\"Phòng chat này hiện đang bị tạm khóa bởi Admin.\"}");
                return;
            }

            // Kiem tra phong day
            if (room.getUserCount() >= room.getMaxUsers() && !account.isAdmin()) {
                out.print("{\"success\":false,\"message\":\"Phòng chat đã đạt giới hạn tối đa (" + room.getMaxUsers() + " người)!\"}");
                return;
            }

            // Kiem tra ma bao mat cua phong (neu phong co thiet lap)
            if (room.hasPasscode()) {
                String passcode = req.getParameter("passcode");
                if (passcode == null || passcode.trim().isEmpty()) {
                    out.print("{\"success\":false,\"needPasscode\":true,\"wrongPasscode\":false,\"roomCode\":\""
                            + room.getRoomCode() + "\",\"roomName\":\"" + escapeJson(room.getRoomName())
                            + "\",\"message\":\"Phòng #" + room.getRoomCode() + " được bảo vệ bằng mã bảo mật.\"}");
                    return;
                }
                if (!room.verifyPasscode(passcode)) {
                    out.print("{\"success\":false,\"needPasscode\":true,\"wrongPasscode\":true,\"roomCode\":\""
                            + room.getRoomCode() + "\",\"roomName\":\"" + escapeJson(room.getRoomName())
                            + "\",\"message\":\"Mã bảo mật không chính xác! Vui lòng kiểm tra lại.\"}");
                    return;
                }
            }

            // Tao thong tin thanh vien dong bo voi tai khoan
            UserProfile profile = new UserProfile(
                    account.getUsername(),
                    account.getFullName(),
                    account.getAvatarUrl(),
                    account.getRole(),
                    req.getRemoteAddr()
            );
            profile.setBio(account.getBio());
            profile.setEmail(account.getEmail());
            room.clearKickedUser(profile.getUserId());
            room.addUser(profile);

            session.setAttribute("user", profile);
            session.setAttribute("roomCode", room.getRoomCode());
            session.setAttribute("verifiedRoom_" + room.getRoomCode(), Boolean.TRUE);
            session.setAttribute("isAdmin", account.isAdmin());
            session.setAttribute("role", account.getRole());

            // Phat thong bao JOIN qua mang UDP Multicast
            try {
                String extra = "{\"role\":\"" + account.getRole() + "\"}";
                ChatMessage joinMsg = new ChatMessage(
                        MessageType.JOIN,
                        account.getUsername(),
                        account.getFullName(),
                        account.getFullName() + " đã tham gia phòng chat.",
                        "",
                        System.currentTimeMillis(),
                        room.getRoomCode(),
                        account.getAvatarUrl(),
                        extra
                );
                room.broadcastUDP(joinMsg);
            } catch (Exception ignored) {
            }

            out.print("{\"success\":true,\"isAdmin\":" + account.isAdmin()
                    + ",\"role\":\"" + account.getRole() + "\""
                    + ",\"roomCode\":\"" + room.getRoomCode() + "\""
                    + ",\"roomName\":\"" + escapeJson(room.getRoomName()) + "\"}");

        // ==========================================
        // 2. KHỞI TẠO PHÒNG MỚI (CHỈ DÀNH CHO ADMIN)
        // ==========================================
        } else if ("create".equalsIgnoreCase(action)) {
            if (session == null || session.getAttribute("account") == null) {
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                out.print("{\"success\":false,\"message\":\"Vui lòng đăng nhập tài khoản Quản trị viên!\"}");
                return;
            }

            Account account = (Account) session.getAttribute("account");
            if (!account.isAdmin()) {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                out.print("{\"success\":false,\"message\":\"403 Forbidden: Chỉ Quản trị viên (ADMIN) mới có quyền khởi tạo phòng chat mới!\"}");
                return;
            }

            String roomCode = req.getParameter("roomCode");
            String roomName = req.getParameter("roomName");
            String description = req.getParameter("description");
            String multicastIp = req.getParameter("multicastIp");
            String portStr = req.getParameter("multicastPort");
            String maxUsersStr = req.getParameter("maxUsers");
            String accessPasscode = req.getParameter("accessPasscode");

            int port = 8888;
            int maxUsers = 50;
            try {
                if (portStr != null) port = Integer.parseInt(portStr.trim());
                if (maxUsersStr != null) maxUsers = Integer.parseInt(maxUsersStr.trim());
            } catch (Exception ignored) {
            }

            try {
                ChatRoom room = manager.createRoom(
                        roomCode,
                        roomName,
                        description,
                        account.getFullName(),
                        "SECURED_BY_RBAC",
                        multicastIp,
                        port,
                        maxUsers,
                        accessPasscode
                );

                UserProfile adminProfile = new UserProfile(
                        account.getUsername(),
                        account.getFullName(),
                        account.getAvatarUrl(),
                        "ADMIN",
                        req.getRemoteAddr()
                );
                adminProfile.setBio(account.getBio());
                adminProfile.setEmail(account.getEmail());
                room.addUser(adminProfile);

                session.setAttribute("user", adminProfile);
                session.setAttribute("roomCode", room.getRoomCode());
                session.setAttribute("verifiedRoom_" + room.getRoomCode(), Boolean.TRUE);
                session.setAttribute("isAdmin", true);
                session.setAttribute("role", "ADMIN");

                ChatMessage initMsg = ChatMessage.createSystem(room.getRoomCode(),
                        "Phòng chat [" + room.getRoomName() + "] đã được khởi tạo bởi Quản trị viên " + account.getFullName() + ".");
                room.broadcastUDP(initMsg);

                out.print("{\"success\":true,\"message\":\"Khởi tạo phòng chat thành công!\",\"roomCode\":\"" + room.getRoomCode() + "\"}");
            } catch (Exception e) {
                out.print("{\"success\":false,\"message\":\"" + escapeJson(e.getMessage()) + "\"}");
            }

        // ==========================================
        // 3. RỜI PHÒNG CHAT (LEAVE)
        // ==========================================
        } else if ("leave".equalsIgnoreCase(action)) {
            if (session != null) {
                UserProfile user = (UserProfile) session.getAttribute("user");
                String roomCode = (String) session.getAttribute("roomCode");

                if (user != null && roomCode != null) {
                    ChatRoom room = manager.getRoom(roomCode);
                    if (room != null) {
                        room.removeUser(user.getUserId());
                        try {
                            String extra = "{\"role\":\"" + user.getRole() + "\"}";
                            ChatMessage leaveMsg = new ChatMessage(
                                    MessageType.LEAVE,
                                    user.getUserId(),
                                    user.getUsername(),
                                    user.getUsername() + " đã rời phòng chat.",
                                    "",
                                    System.currentTimeMillis(),
                                    roomCode,
                                    user.getAvatarUrl(),
                                    extra
                            );
                            room.broadcastUDP(leaveMsg);
                        } catch (Exception ignored) {
                        }
                    }
                }
                session.removeAttribute("roomCode");
                if (roomCode != null) {
                    session.removeAttribute("verifiedRoom_" + roomCode.trim().toUpperCase());
                }
            }
            out.print("{\"success\":true,\"message\":\"Đã rời khỏi phòng chat.\"}");

        } else {
            out.print("{\"success\":false,\"message\":\"Hành động không xác định.\"}");
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
