package multichat_udp.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import multichat_udp.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * Servlet cung cấp API nhận/gửi tin nhắn thời gian thực và đồng bộ UDP Multicast.
 * Tích hợp kiểm tra quyền (Muted, Room Locked) và đóng gói role (ADMIN / MEMBER) vào UDP payload.
 */
@WebServlet(name = "ChatApiServlet", urlPatterns = {"/api/chat"})
public class ChatApiServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        String roomCode = req.getParameter("roomCode");
        String action = req.getParameter("action");
        if (action == null) action = "poll";

        if (roomCode == null || roomCode.trim().isEmpty()) {
            HttpSession session = req.getSession(false);
            if (session != null) {
                roomCode = (String) session.getAttribute("roomCode");
            }
        }

        ChatRoom room = RoomManager.getInstance().getRoom(roomCode);
        if (room == null) {
            out.print("{\"success\":false,\"message\":\"Phòng chat không tồn tại.\"}");
            return;
        }

        // Cap nhat hoat dong cua user trong session
        HttpSession session = req.getSession(false);
        UserProfile currentUser = (session != null) ? (UserProfile) session.getAttribute("user") : null;
        boolean isKicked = false;
        boolean isMuted = false;

        if (currentUser != null) {
            currentUser.touch();
            UserProfile roomUser = room.getUser(currentUser.getUserId());
            if (roomUser != null) {
                roomUser.touch();
                isMuted = roomUser.isMuted();
                // Dong bo lai ten hien thi moi nhat tu roomUser neu co
                currentUser.setUsername(roomUser.getUsername());
            } else {
                // Thanh vien khong con trong danh sach phong (co the da bi Admin kick)
                isKicked = true;
            }
        }

        if ("poll".equalsIgnoreCase(action)) {
            if (currentUser == null) {
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                out.print("{\"success\":false,\"message\":\"Chưa đăng nhập hoặc phiên làm việc đã hết hạn.\"}");
                return;
            }

            long since = 0;
            String sinceStr = req.getParameter("since");
            if (sinceStr != null) {
                try {
                    since = Long.parseLong(sinceStr.trim());
                } catch (Exception ignored) {
                }
            }

            UserProfile viewer = isKicked ? null : currentUser;
            List<ChatMessage> newMessages = room.getMessagesSince(since, viewer);
            List<UserProfile> members = room.getActiveUsersList();

            StringBuilder sb = new StringBuilder();
            sb.append("{\"success\":true,");
            sb.append("\"serverTime\":").append(System.currentTimeMillis()).append(",");
            sb.append("\"userCount\":").append(room.getUserCount()).append(",");
            sb.append("\"roomStatus\":\"").append(escapeJson(room.getStatus())).append("\",");
            sb.append("\"isKicked\":").append(isKicked).append(",");
            sb.append("\"isMuted\":").append(isMuted).append(",");

            // Danh sach tin nhan moi (da loc quyen xem tin nhan rieng)
            sb.append("\"messages\":[");
            boolean firstMsg = true;
            for (ChatMessage m : newMessages) {
                if (!firstMsg) sb.append(",");
                sb.append(m.toJson());
                firstMsg = false;
            }
            sb.append("],");

            // Danh sach thanh vien dang online
            sb.append("\"members\":[");
            boolean firstUser = true;
            for (UserProfile u : members) {
                if (!firstUser) sb.append(",");
                sb.append(u.toJson());
                firstUser = false;
            }
            sb.append("],");
            sb.append("\"reactions\":").append(isKicked ? "{}" : room.getAllReactionsJson(currentUser.getUserId()));

            sb.append("}");
            out.print(sb.toString());

        } else if ("history".equalsIgnoreCase(action)) {
            if (currentUser == null) {
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                out.print("{\"success\":false,\"message\":\"Chưa đăng nhập hoặc phiên làm việc đã hết hạn.\"}");
                return;
            }

            UserProfile viewer = isKicked ? null : currentUser;
            List<ChatMessage> history = room.getRecentMessages(50, viewer);
            StringBuilder sb = new StringBuilder();
            sb.append("{\"success\":true,\"messages\":[");
            boolean first = true;
            for (ChatMessage m : history) {
                if (!first) sb.append(",");
                sb.append(m.toJson());
                first = false;
            }
            sb.append("],\"reactions\":").append(isKicked ? "{}" : room.getAllReactionsJson(currentUser.getUserId())).append("}");
            out.print(sb.toString());

        } else if ("members".equalsIgnoreCase(action)) {
            List<UserProfile> members = room.getActiveUsersList();
            StringBuilder sb = new StringBuilder();
            sb.append("{\"success\":true,\"members\":[");
            boolean first = true;
            for (UserProfile u : members) {
                if (!first) sb.append(",");
                sb.append(u.toJson());
                first = false;
            }
            sb.append("]}");
            out.print(sb.toString());
        } else {
            out.print("{\"success\":false,\"message\":\"Hành động không hợp lệ.\"}");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"success\":false,\"message\":\"Chưa đăng nhập hoặc phiên làm việc đã hết hạn.\"}");
            return;
        }

        UserProfile user = (UserProfile) session.getAttribute("user");
        String roomCode = (String) session.getAttribute("roomCode");
        boolean isAdmin = Boolean.TRUE.equals(session.getAttribute("isAdmin"));
        String role = (session.getAttribute("role") != null) ? (String) session.getAttribute("role") : user.getRole();

        ChatRoom room = RoomManager.getInstance().getRoom(roomCode);
        if (room == null) {
            out.print("{\"success\":false,\"message\":\"Phòng chat không tồn tại.\"}");
            return;
        }

        // 1. Kiem tra phong tam khoa
        if ("LOCKED".equalsIgnoreCase(room.getStatus()) && !isAdmin) {
            out.print("{\"success\":false,\"message\":\"Phòng chat hiện đang bị tạm khóa bởi Admin. Bạn không thể gửi tin lúc này.\"}");
            return;
        }

        // 2. Kiem tra nguoi dung con trong phong hay bi tat tieng (mute)
        UserProfile roomUser = room.getUser(user.getUserId());
        if (roomUser == null) {
            out.print("{\"success\":false,\"message\":\"Bạn không còn trong phòng chat này.\"}");
            return;
        }
        if (roomUser.isMuted()) {
            out.print("{\"success\":false,\"message\":\"Bạn đã bị Admin tắt tiếng trong phòng này!\"}");
            return;
        }

        String action = req.getParameter("action");
        if ("recall".equalsIgnoreCase(action)) {
            long targetTs = parseLongSafe(req.getParameter("targetTimestamp"));
            String targetSenderId = req.getParameter("targetSenderId");
            ChatMessage targetMsg = room.findMessage(targetTs, targetSenderId);
            if (targetMsg == null) {
                out.print("{\"success\":false,\"message\":\"Không tìm thấy tin nhắn cần thu hồi.\"}");
                return;
            }
            if (!user.getUserId().equals(targetMsg.getSenderId()) && !isAdmin) {
                out.print("{\"success\":false,\"message\":\"Bạn chỉ có thể thu hồi tin nhắn của chính mình.\"}");
                return;
            }

            String targetUserForEvent = targetMsg.getTargetUser();
            String targetUserIdForEvent = targetMsg.getTargetUserId();
            room.markRecalled(targetMsg, user.getUsername());

            StringBuilder recallExtra = new StringBuilder();
            recallExtra.append("{\"targetTimestamp\":\"").append(targetTs)
                    .append("\",\"targetSenderId\":\"").append(escapeJson(targetSenderId)).append("\"");
            if (targetUserIdForEvent != null && !targetUserIdForEvent.isEmpty()) {
                recallExtra.append(",\"targetUserId\":\"").append(escapeJson(targetUserIdForEvent)).append("\"");
            }
            recallExtra.append("}");

            ChatMessage recallEvent = new ChatMessage(
                    MessageType.RECALL,
                    user.getUserId(),
                    user.getUsername(),
                    "RECALL",
                    targetUserForEvent,
                    System.currentTimeMillis(),
                    room.getRoomCode(),
                    user.getAvatarUrl(),
                    recallExtra.toString()
            );
            try {
                room.broadcastUDP(recallEvent);
                out.print("{\"success\":true,\"message\":\"Đã thu hồi tin nhắn.\"}");
            } catch (Exception e) {
                out.print("{\"success\":false,\"message\":\"Lỗi thu hồi tin nhắn: " + escapeJson(e.getMessage()) + "\"}");
            }
            return;
        }

        if ("react".equalsIgnoreCase(action)) {
            long targetTs = parseLongSafe(req.getParameter("targetTimestamp"));
            String targetSenderId = req.getParameter("targetSenderId");
            String emoji = req.getParameter("emoji");
            ChatMessage targetMsg = room.findMessage(targetTs, targetSenderId);
            if (targetMsg == null || !ChatRoom.isMessageVisibleTo(targetMsg, user)) {
                out.print("{\"success\":false,\"message\":\"Không tìm thấy tin nhắn hoặc không có quyền thao tác.\"}");
                return;
            }

            room.toggleReaction(targetTs, targetSenderId, user.getUserId(), emoji);
            String targetUserForEvent = targetMsg.getTargetUser();
            String targetUserIdForEvent = targetMsg.getTargetUserId();

            StringBuilder reactExtra = new StringBuilder();
            reactExtra.append("{\"targetTimestamp\":\"").append(targetTs)
                    .append("\",\"targetSenderId\":\"").append(escapeJson(targetSenderId))
                    .append("\",\"emoji\":\"").append(escapeJson(emoji)).append("\"");
            if (targetUserIdForEvent != null && !targetUserIdForEvent.isEmpty()) {
                reactExtra.append(",\"targetUserId\":\"").append(escapeJson(targetUserIdForEvent)).append("\"");
            }
            reactExtra.append("}");

            ChatMessage reactEvent = new ChatMessage(
                    MessageType.REACTION,
                    user.getUserId(),
                    user.getUsername(),
                    "REACTION",
                    targetUserForEvent,
                    System.currentTimeMillis(),
                    room.getRoomCode(),
                    user.getAvatarUrl(),
                    reactExtra.toString()
            );
            try {
                room.broadcastUDP(reactEvent);
                String reactionsJson = room.getMessageReactionsJson(targetTs, targetSenderId, user.getUserId());
                out.print("{\"success\":true,\"reactions\":" + reactionsJson + "}");
            } catch (Exception e) {
                out.print("{\"success\":false,\"message\":\"Lỗi thả cảm xúc: " + escapeJson(e.getMessage()) + "\"}");
            }
            return;
        }

        String typeStr = req.getParameter("type");
        String content = req.getParameter("content");
        String targetUser = req.getParameter("targetUser");
        String targetUserId = req.getParameter("targetUserId");
        String replyToId = req.getParameter("replyToId");
        String replyToSender = req.getParameter("replyToSender");
        String replyToPreview = req.getParameter("replyToPreview");

        if (content == null || content.trim().isEmpty()) {
            out.print("{\"success\":false,\"message\":\"Nội dung tin nhắn không được để trống.\"}");
            return;
        }

        String resolvedTargetName = "";
        String resolvedTargetId = "";
        boolean hasWhisperTarget = (targetUser != null && !targetUser.trim().isEmpty())
                || (targetUserId != null && !targetUserId.trim().isEmpty());

        if (hasWhisperTarget) {
            UserProfile targetProfile = room.findTargetUser(targetUserId, targetUser);
            if (targetProfile == null) {
                out.print("{\"success\":false,\"message\":\"Người nhận tin nhắn riêng không tồn tại hoặc đã rời phòng.\"}");
                return;
            }
            if (targetProfile.getUserId().equals(user.getUserId())) {
                out.print("{\"success\":false,\"message\":\"Bạn không thể tự nhắn riêng cho chính mình.\"}");
                return;
            }
            resolvedTargetName = targetProfile.getUsername();
            resolvedTargetId = targetProfile.getUserId();
        }

        StringBuilder extraSuffix = new StringBuilder();
        if (!resolvedTargetId.isEmpty()) {
            extraSuffix.append(",\"targetUserId\":\"").append(escapeJson(resolvedTargetId)).append("\"");
        }
        if (replyToId != null && !replyToId.trim().isEmpty()) {
            extraSuffix.append(",\"replyToId\":\"").append(escapeJson(replyToId.trim())).append("\"");
            extraSuffix.append(",\"replyToSender\":\"").append(escapeJson(replyToSender != null ? replyToSender.trim() : "")).append("\"");
            extraSuffix.append(",\"replyToPreview\":\"").append(escapeJson(replyToPreview != null ? replyToPreview.trim() : "")).append("\"");
        }
        String extraFields = extraSuffix.toString();

        ChatMessage msg;
        if ("STICKER".equalsIgnoreCase(typeStr)) {
            String pack = req.getParameter("pack");
            String stickerId = req.getParameter("stickerId");
            String stickerUrl = req.getParameter("stickerUrl");
            String extra = "{\"pack\":\"" + escapeJson(pack) + "\",\"id\":\"" + escapeJson(stickerId)
                    + "\",\"stickerUrl\":\"" + escapeJson(stickerUrl) + "\",\"role\":\"" + role + "\""
                    + extraFields + "}";
            msg = new ChatMessage(MessageType.STICKER, user.getUserId(), user.getUsername(), "[Sticker]",
                    resolvedTargetName, System.currentTimeMillis(), room.getRoomCode(), user.getAvatarUrl(), extra);

        } else if ("LINK".equalsIgnoreCase(typeStr)) {
            String url = req.getParameter("url");
            String title = req.getParameter("title");
            String domain = req.getParameter("domain");
            String extra = "{\"url\":\"" + escapeJson(url) + "\",\"title\":\"" + escapeJson(title)
                    + "\",\"domain\":\"" + escapeJson(domain) + "\",\"role\":\"" + role + "\""
                    + extraFields + "}";
            msg = new ChatMessage(MessageType.LINK, user.getUserId(), user.getUsername(), content.trim(),
                    resolvedTargetName, System.currentTimeMillis(), room.getRoomCode(), user.getAvatarUrl(), extra);

        } else {
            // Tin nhan text hoac whisper
            MessageType msgType = !resolvedTargetName.isEmpty() ? MessageType.WHISPER : MessageType.CHAT;
            String extra = "{\"role\":\"" + role + "\"" + extraFields + "}";
            msg = new ChatMessage(msgType, user.getUserId(), user.getUsername(), content.trim(),
                    resolvedTargetName, System.currentTimeMillis(), room.getRoomCode(), user.getAvatarUrl(), extra);
        }

        // PHÁT QUA SOCKET UDP MULTICAST
        try {
            room.broadcastUDP(msg);
            user.incrementMessageCount();
            user.touch();

            out.print("{\"success\":true,\"message\":\"Đã gửi tin nhắn qua UDP Multicast.\",\"messageData\":" + msg.toJson() + "}");
        } catch (Exception e) {
            out.print("{\"success\":false,\"message\":\"Lỗi truyền thông UDP: " + escapeJson(e.getMessage()) + "\"}");
        }
    }

    private long parseLongSafe(String val) {
        if (val == null || val.trim().isEmpty()) return 0L;
        try {
            return Long.parseLong(val.trim());
        } catch (Exception e) {
            return 0L;
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
