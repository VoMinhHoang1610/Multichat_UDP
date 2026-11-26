package multichat_udp.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import multichat_udp.*;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

/**
 * Servlet dành riêng cho Quản trị viên (ADMIN) quản lý phòng chat và xuất báo cáo.
 * Bảo mật chặt chẽ bằng RBAC: Chặn đứng truy cập từ Người dùng thường (MEMBER) với mã lỗi 403.
 * Loại bỏ hoàn toàn cơ chế nhập mã PIN tạm bợ.
 */
@WebServlet(name = "AdminServlet", urlPatterns = {"/api/admin"})
public class AdminServlet extends HttpServlet {

    private boolean checkAdminAuthorization(HttpServletRequest req, HttpServletResponse resp, boolean isHtml) throws IOException {
        HttpSession session = req.getSession(false);
        boolean isAdmin = session != null && Boolean.TRUE.equals(session.getAttribute("isAdmin"));

        if (!isAdmin) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            if (isHtml) {
                resp.setContentType("text/html;charset=UTF-8");
                resp.getWriter().print("<div style='text-align:center;padding:50px;font-family:sans-serif;'>"
                        + "<h1 style='color:#dc2626;'>403 Forbidden - Cấm Truy Cập</h1>"
                        + "<p>Bạn không có quyền quản trị viên để thực hiện hành động này.</p>"
                        + "<a href='" + req.getContextPath() + "/index.jsp'>Quay lại Sảnh chờ</a></div>");
            } else {
                resp.setContentType("application/json;charset=UTF-8");
                resp.getWriter().print("{\"success\":false,\"message\":\"403 Forbidden: Bạn không có quyền quản trị viên!\"}");
            }
            return false;
        }
        return true;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = req.getParameter("action");
        boolean isExportHtml = "export_html".equalsIgnoreCase(action);

        if (!checkAdminAuthorization(req, resp, isExportHtml)) {
            return;
        }

        String roomCode = req.getParameter("roomCode");
        if (roomCode == null || roomCode.trim().isEmpty()) {
            HttpSession session = req.getSession(false);
            if (session != null) {
                roomCode = (String) session.getAttribute("roomCode");
            }
        }

        ChatRoom room = RoomManager.getInstance().getRoom(roomCode);
        if (room == null) {
            resp.setContentType("text/plain;charset=UTF-8");
            resp.getWriter().print("Không tìm thấy phòng chat với mã: " + roomCode);
            return;
        }

        if ("export_csv".equalsIgnoreCase(action)) {
            // XUẤT FILE BÁO CÁO CSV (EXCEL UTF-8 BOM)
            byte[] csvData = ReportService.exportUsersCsv(room);

            resp.setContentType("text/csv;charset=UTF-8");
            String fileName = "BaoCao_ThanhVien_" + room.getRoomCode() + "_" + System.currentTimeMillis() + ".csv";
            resp.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
            resp.setContentLength(csvData.length);

            try (OutputStream os = resp.getOutputStream()) {
                os.write(csvData);
                os.flush();
            }

        } else if ("export_html".equalsIgnoreCase(action)) {
            // XUẤT BÁO CÁO HTML / IN TRANG
            String htmlContent = ReportService.exportUsersHtml(room);
            resp.setContentType("text/html;charset=UTF-8");
            resp.getWriter().print(htmlContent);

        } else {
            resp.setContentType("application/json;charset=UTF-8");
            resp.getWriter().print("{\"success\":false,\"message\":\"Hành động không hợp lệ.\"}");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        if (!checkAdminAuthorization(req, resp, false)) {
            return;
        }

        String action = req.getParameter("action");
        String roomCode = req.getParameter("roomCode");

        ChatRoom room = RoomManager.getInstance().getRoom(roomCode);
        if (room == null) {
            out.print("{\"success\":false,\"message\":\"Phòng chat không tồn tại.\"}");
            return;
        }

        HttpSession session = req.getSession(false);
        Account account = (session != null) ? (Account) session.getAttribute("account") : null;
        String adminDisplayName = (account != null) ? account.getFullName() : "Admin";

        if ("auth".equalsIgnoreCase(action)) {
            out.print("{\"success\":true,\"message\":\"Xác thực Quản trị viên thành công!\"}");

        } else if ("kick".equalsIgnoreCase(action)) {
            String targetUserId = req.getParameter("userId");
            UserProfile targetUser = room.getUser(targetUserId);
            if (targetUser != null && "ADMIN".equalsIgnoreCase(targetUser.getRole())) {
                out.print("{\"success\":false,\"message\":\"Không thể mời Quản trị viên ra khỏi phòng.\"}");
                return;
            }
            UserProfile kickedUser = room.kickUser(targetUserId);
            if (kickedUser != null) {
                try {
                    String extra = "{\"role\":\"ADMIN\",\"action\":\"KICK\",\"target\":\"" + escapeJson(kickedUser.getUsername()) + "\"}";
                    ChatMessage sysMsg = new ChatMessage(
                            MessageType.SYSTEM,
                            "SYSTEM",
                            "Hệ thống",
                            "Quản trị viên [" + adminDisplayName + "] đã mời [" + kickedUser.getUsername() + "] ra khỏi phòng.",
                            "",
                            System.currentTimeMillis(),
                            room.getRoomCode(),
                            "assets/avatars/system.svg",
                            extra
                    );
                    room.broadcastUDP(sysMsg);
                } catch (Exception ignored) {
                }
                out.print("{\"success\":true,\"message\":\"Đã mời " + escapeJson(kickedUser.getUsername()) + " ra khỏi phòng.\"}");
            } else {
                out.print("{\"success\":false,\"message\":\"Không tìm thấy thành viên cần mời ra.\"}");
            }

        } else if ("mute".equalsIgnoreCase(action)) {
            String targetUserId = req.getParameter("userId");
            UserProfile user = room.getUser(targetUserId);
            if (user != null) {
                user.setMuted(!user.isMuted());
                String statusMsg = user.isMuted() ? "tắt tiếng" : "bật tiếng lại cho";
                try {
                    String extra = "{\"role\":\"ADMIN\",\"action\":\"MUTE\",\"target\":\"" + escapeJson(user.getUsername()) + "\"}";
                    ChatMessage sysMsg = new ChatMessage(
                            MessageType.SYSTEM,
                            "SYSTEM",
                            "Hệ thống",
                            "Quản trị viên [" + adminDisplayName + "] đã " + statusMsg + " người dùng [" + user.getUsername() + "].",
                            "",
                            System.currentTimeMillis(),
                            room.getRoomCode(),
                            "assets/avatars/system.svg",
                            extra
                    );
                    room.broadcastUDP(sysMsg);
                } catch (Exception ignored) {
                }
                out.print("{\"success\":true,\"message\":\"Đã " + statusMsg + " thành viên " + escapeJson(user.getUsername()) + ".\",\"muted\":" + user.isMuted() + "}");
            } else {
                out.print("{\"success\":false,\"message\":\"Không tìm thấy thành viên cần thao tác.\"}");
            }

        } else if ("clear_chat".equalsIgnoreCase(action)) {
            room.clearHistory();
            try {
                String extra = "{\"role\":\"ADMIN\",\"action\":\"CLEAR_CHAT\"}";
                ChatMessage sysMsg = new ChatMessage(
                        MessageType.SYSTEM,
                        "SYSTEM",
                        "Hệ thống",
                        "Quản trị viên [" + adminDisplayName + "] đã làm sạch toàn bộ lịch sử cuộc trò chuyện.",
                        "",
                        System.currentTimeMillis(),
                        room.getRoomCode(),
                        "assets/avatars/system.svg",
                        extra
                );
                room.broadcastUDP(sysMsg);
            } catch (Exception ignored) {
            }
            out.print("{\"success\":true,\"message\":\"Đã làm sạch lịch sử cuộc trò chuyện.\"}");

        } else if ("toggle_lock".equalsIgnoreCase(action)) {
            String newStatus = "ACTIVE".equalsIgnoreCase(room.getStatus()) ? "LOCKED" : "ACTIVE";
            room.setStatus(newStatus);
            try {
                String extra = "{\"role\":\"ADMIN\",\"action\":\"TOGGLE_LOCK\",\"status\":\"" + newStatus + "\"}";
                ChatMessage sysMsg = new ChatMessage(
                        MessageType.SYSTEM,
                        "SYSTEM",
                        "Hệ thống",
                        "ACTIVE".equals(newStatus)
                                ? "Quản trị viên [" + adminDisplayName + "] đã mở khóa phòng chat."
                                : "Quản trị viên [" + adminDisplayName + "] đã tạm khóa phòng chat.",
                        "",
                        System.currentTimeMillis(),
                        room.getRoomCode(),
                        "assets/avatars/system.svg",
                        extra
                );
                room.broadcastUDP(sysMsg);
            } catch (Exception ignored) {
            }
            out.print("{\"success\":true,\"status\":\"" + newStatus + "\"}");

        } else if ("delete_room".equalsIgnoreCase(action)) {
            boolean deleted = RoomManager.getInstance().deleteRoomByAdmin(room.getRoomCode());
            if (deleted) {
                if (session != null) {
                    String currentSessionRoom = (String) session.getAttribute("roomCode");
                    if (room.getRoomCode().equalsIgnoreCase(currentSessionRoom)) {
                        session.removeAttribute("roomCode");
                    }
                }
                out.print("{\"success\":true,\"message\":\"Đã giải tán và xóa phòng #" + escapeJson(room.getRoomCode()) + ".\"}");
            } else {
                out.print("{\"success\":false,\"message\":\"Không thể xóa phòng chat này.\"}");
            }

        } else {
            out.print("{\"success\":false,\"message\":\"Hành động Quản trị viên không xác định.\"}");
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
