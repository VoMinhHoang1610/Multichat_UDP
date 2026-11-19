package multichat_udp;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Dich vu xuat bao cao thong ke nguoi dung trong phong chat (CSV va HTML/In).
 */
public class ReportService {
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

    /**
     * Xuat file CSV tuong thich Excel co UTF-8 BOM.
     */
    public static byte[] exportUsersCsv(ChatRoom room) {
        StringBuilder sb = new StringBuilder();
        // UTF-8 BOM giup Excel doc tieng Viet co dau chuan xac 100%
        sb.append("\uFEFF");

        // Tieu de bao cao
        sb.append("BÁO CÁO THÀNH VIÊN PHÒNG CHAT UDP MULTICAST\n");
        sb.append("Mã phòng,").append(escapeCsv(room.getRoomCode())).append("\n");
        sb.append("Tên phòng,").append(escapeCsv(room.getRoomName())).append("\n");
        sb.append("Địa chỉ Multicast,").append(room.getMulticastIp()).append(":").append(room.getMulticastPort()).append("\n");
        sb.append("Thời điểm xuất,").append(TIME_FORMAT.format(new Date())).append("\n");
        sb.append("Tổng số thành viên,").append(room.getUserCount()).append("\n\n");

        // Header bang
        sb.append("STT,Mã User,Tên Người Dùng,Vai Trò,Địa Chỉ IP,Thời Gian Vào,Hoạt Động Cuối,Số Tin Nhắn,Trạng Thái,Tiểu Sử\n");

        List<UserProfile> users = room.getActiveUsersList();
        int stt = 1;
        for (UserProfile u : users) {
            sb.append(stt++).append(",");
            sb.append(escapeCsv(u.getUserId())).append(",");
            sb.append(escapeCsv(u.getUsername())).append(",");
            sb.append(escapeCsv(u.getRole())).append(",");
            sb.append(escapeCsv(u.getIpAddress())).append(",");
            sb.append(escapeCsv(u.getFormattedJoinTime())).append(",");
            sb.append(escapeCsv(u.getFormattedLastActive())).append(",");
            sb.append(u.getMessageCount()).append(",");
            sb.append(u.isMuted() ? "Đã tắt tiếng" : "Bình thường").append(",");
            sb.append(escapeCsv(u.getBio())).append("\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Xuat bao cao dinh dang HTML chuyen nghiep, ho tro in an va xuat PDF.
     */
    public static String exportUsersHtml(ChatRoom room) {
        StringBuilder sb = new StringBuilder();
        List<UserProfile> users = room.getActiveUsersList();
        String exportTime = TIME_FORMAT.format(new Date());

        sb.append("<!DOCTYPE html>\n<html lang=\"vi\">\n<head>\n");
        sb.append("<meta charset=\"UTF-8\">\n");
        sb.append("<title>Báo cáo thành viên - ").append(room.getRoomCode()).append("</title>\n");
        sb.append("<style>\n");
        sb.append("body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 40px; color: #1e293b; background: #fff; }\n");
        sb.append(".header { border-bottom: 2px solid #2563eb; padding-bottom: 20px; margin-bottom: 30px; }\n");
        sb.append("h1 { color: #1e3a8a; margin: 0 0 10px 0; font-size: 24px; text-transform: uppercase; }\n");
        sb.append(".meta-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 15px; margin-bottom: 25px; }\n");
        sb.append(".meta-card { background: #f8fafc; border: 1px solid #e2e8f0; padding: 15px; border-radius: 8px; }\n");
        sb.append(".meta-label { font-size: 12px; color: #64748b; font-weight: 600; text-transform: uppercase; }\n");
        sb.append(".meta-value { font-size: 16px; color: #0f172a; font-weight: bold; margin-top: 4px; }\n");
        sb.append("table { width: 100%; border-collapse: collapse; margin-top: 20px; }\n");
        sb.append("th { background: #2563eb; color: #fff; text-align: left; padding: 12px 14px; font-size: 13px; }\n");
        sb.append("td { padding: 10px 14px; border-bottom: 1px solid #e2e8f0; font-size: 13px; }\n");
        sb.append("tr:nth-child(even) { background-color: #f8fafc; }\n");
        sb.append(".badge { display: inline-block; padding: 4px 8px; border-radius: 9999px; font-size: 11px; font-weight: 600; }\n");
        sb.append(".badge-admin { background: #dbeafe; color: #1d4ed8; }\n");
        sb.append(".badge-member { background: #e2e8f0; color: #475569; }\n");
        sb.append(".badge-active { background: #dcfce7; color: #15803d; }\n");
        sb.append(".actions { margin-bottom: 20px; text-align: right; }\n");
        sb.append(".btn-print { background: #2563eb; color: #fff; border: none; padding: 10px 20px; border-radius: 6px; cursor: pointer; font-weight: 600; }\n");
        sb.append("@media print { .actions { display: none; } body { margin: 0; } }\n");
        sb.append(".footer { margin-top: 40px; text-align: right; font-style: italic; color: #64748b; }\n");
        sb.append("</style>\n</head>\n<body>\n");

        sb.append("<div class=\"actions\">\n");
        sb.append("<button class=\"btn-print\" onclick=\"window.print()\">🖨 In báo cáo / Lưu PDF</button>\n");
        sb.append("</div>\n");

        sb.append("<div class=\"header\">\n");
        sb.append("<h1>Báo Cáo Hoạt Động Phòng Chat UDP Multicast</h1>\n");
        sb.append("<div>Hệ thống Lập Trình Mạng - UDP Socket Multichat Enterprise</div>\n");
        sb.append("</div>\n");

        sb.append("<div class=\"meta-grid\">\n");
        sb.append("<div class=\"meta-card\"><div class=\"meta-label\">Phòng Chat</div><div class=\"meta-value\">")
                .append(escapeHtml(room.getRoomName())).append(" (").append(room.getRoomCode()).append(")</div></div>\n");
        sb.append("<div class=\"meta-card\"><div class=\"meta-label\">Địa chỉ UDP Multicast</div><div class=\"meta-value\">")
                .append(room.getMulticastIp()).append(":").append(room.getMulticastPort()).append("</div></div>\n");
        sb.append("<div class=\"meta-card\"><div class=\"meta-label\">Tổng số người online</div><div class=\"meta-value\">")
                .append(users.size()).append(" / ").append(room.getMaxUsers()).append(" người</div></div>\n");
        sb.append("<div class=\"meta-card\"><div class=\"meta-label\">Quản trị viên</div><div class=\"meta-value\">")
                .append(escapeHtml(room.getAdminName())).append("</div></div>\n");
        sb.append("<div class=\"meta-card\"><div class=\"meta-label\">Thời điểm xuất</div><div class=\"meta-value\">")
                .append(exportTime).append("</div></div>\n");
        sb.append("<div class=\"meta-card\"><div class=\"meta-label\">Trạng thái phòng</div><div class=\"meta-value\">")
                .append("<span class=\"badge badge-active\">").append(room.getStatus()).append("</span></div></div>\n");
        sb.append("</div>\n");

        sb.append("<table>\n<thead>\n<tr>\n");
        sb.append("<th>STT</th><th>Tên thành viên</th><th>Vai trò</th><th>Địa chỉ IP</th><th>Thời gian vào</th><th>Hoạt động cuối</th><th>Tin nhắn</th><th>Trạng thái</th>\n");
        sb.append("</tr>\n</thead>\n<tbody>\n");

        int stt = 1;
        for (UserProfile u : users) {
            sb.append("<tr>\n");
            sb.append("<td>").append(stt++).append("</td>\n");
            sb.append("<td><strong>").append(escapeHtml(u.getUsername())).append("</strong></td>\n");
            sb.append("<td><span class=\"badge ").append(u.getRole().equalsIgnoreCase("ADMIN") ? "badge-admin" : "badge-member")
                    .append("\">").append(u.getRole()).append("</span></td>\n");
            sb.append("<td><code>").append(escapeHtml(u.getIpAddress())).append("</code></td>\n");
            sb.append("<td>").append(u.getFormattedJoinTime()).append("</td>\n");
            sb.append("<td>").append(u.getFormattedLastActive()).append("</td>\n");
            sb.append("<td>").append(u.getMessageCount()).append("</td>\n");
            sb.append("<td>").append(u.isMuted() ? "<span style='color:red;'>Đã tắt tiếng</span>" : "<span style='color:green;'>Đang online</span>").append("</td>\n");
            sb.append("</tr>\n");
        }

        if (users.isEmpty()) {
            sb.append("<tr><td colspan=\"8\" style=\"text-align:center; padding:30px; color:#64748b;\">Hiện chưa có thành viên nào trong phòng.</td></tr>\n");
        }

        sb.append("</tbody>\n</table>\n");

        sb.append("<div class=\"footer\">\n");
        sb.append("<p>Người lập báo cáo: <strong>").append(escapeHtml(room.getAdminName())).append("</strong></p>\n");
        sb.append("<p>Xác thực hệ thống UDP Multicast Server</p>\n");
        sb.append("</div>\n");

        sb.append("</body>\n</html>");
        return sb.toString();
    }

    private static String escapeCsv(String s) {
        if (s == null) return "\"\"";
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
