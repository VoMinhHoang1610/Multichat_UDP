package multichat_udp;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Du lieu tin nhan va cach dong goi/giai ma byte UDP theo giao thuc MCUP.
 */
public class ChatMessage implements Serializable {
    public static final int MAGIC_NUMBER = 0x4D435550; // "MCUP"
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");

    private MessageType type;
    private String senderId;
    private String senderName;
    private String content;
    private String targetUser; // Whisper recipient, empty if public
    private long timestamp;
    private String roomCode;
    private String avatarUrl;
    private String extraData; // JSON or metadata for files, stickers, links

    public ChatMessage(MessageType type, String senderId, String senderName, String content,
                       String targetUser, long timestamp) {
        this(type, senderId, senderName, content, targetUser, timestamp, "DEFAULT", "assets/avatars/avatar1.svg", "");
    }

    public ChatMessage(MessageType type, String senderId, String senderName, String content,
                       String targetUser, long timestamp, String roomCode, String avatarUrl, String extraData) {
        this.type = type != null ? type : MessageType.CHAT;
        this.senderId = senderId != null ? senderId : "";
        this.senderName = senderName != null ? senderName : "Ẩn danh";
        this.content = content != null ? content : "";
        this.targetUser = targetUser != null ? targetUser : "";
        this.timestamp = timestamp > 0 ? timestamp : System.currentTimeMillis();
        this.roomCode = (roomCode != null && !roomCode.isEmpty()) ? roomCode : "DEFAULT";
        this.avatarUrl = (avatarUrl != null && !avatarUrl.isEmpty()) ? avatarUrl : "assets/avatars/avatar1.svg";
        this.extraData = extraData != null ? extraData : "";
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getTargetUser() {
        return targetUser;
    }

    public void setTargetUser(String targetUser) {
        this.targetUser = targetUser;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getRoomCode() {
        return roomCode;
    }

    public void setRoomCode(String roomCode) {
        this.roomCode = roomCode;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getExtraData() {
        return extraData;
    }

    public void setExtraData(String extraData) {
        this.extraData = extraData;
    }

    public String getFormattedTime() {
        return TIME_FORMAT.format(new Date(timestamp));
    }

    public boolean isWhisper() {
        return type == MessageType.WHISPER || (targetUser != null && !targetUser.trim().isEmpty());
    }

    /**
     * Trich xuat targetUserId tu extraData (neu co) de dinh danh chinh xac nguoi nhan ngay ca khi doi ten.
     */
    public String getTargetUserId() {
        return extractJsonStringField(extraData, "targetUserId");
    }

    public static String extractJsonStringField(String json, String fieldName) {
        if (json == null || json.isEmpty() || fieldName == null || fieldName.isEmpty()) {
            return "";
        }
        String key = "\"" + fieldName + "\":\"";
        int idx = json.indexOf(key);
        if (idx < 0) return "";
        int start = idx + key.length();
        StringBuilder sb = new StringBuilder();
        boolean escaped = false;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escaped) {
                sb.append(c);
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '"') {
                return sb.toString();
            } else {
                sb.append(c);
            }
        }
        return "";
    }

    /**
     * Ghi cac truong theo giao thuc nhi phan MCUP.
     */
    public byte[] toBytes() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (DataOutputStream dos = new DataOutputStream(baos)) {
            dos.writeInt(MAGIC_NUMBER);
            dos.writeByte(type.getCode());
            dos.writeUTF(senderId);
            dos.writeUTF(senderName);
            dos.writeUTF(content);
            dos.writeUTF(targetUser);
            dos.writeLong(timestamp);
            dos.writeUTF(roomCode);
            dos.writeUTF(avatarUrl);
            dos.writeUTF(extraData);
            dos.flush();
            return baos.toByteArray();
        }
    }

    /**
     * Giai ma mang byte thanh ChatMessage. Tuong thich ca ban MCUP cu va moi.
     */
    public static ChatMessage fromBytes(byte[] data, int offset, int length) {
        if (data == null || length <= 0) {
            return null;
        }

        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(data, offset, length);
            DataInputStream dis = new DataInputStream(bais);

            if (length >= 4) {
                int magic = dis.readInt();
                if (magic == MAGIC_NUMBER) {
                    byte typeCode = dis.readByte();
                    MessageType type = MessageType.fromCode(typeCode);
                    String senderId = dis.readUTF();
                    String senderName = dis.readUTF();
                    String content = dis.readUTF();
                    String targetUser = dis.readUTF();
                    long timestamp = dis.readLong();

                    String roomCode = "DEFAULT";
                    String avatarUrl = "assets/avatars/avatar1.svg";
                    String extraData = "";

                    // Doc tiep cac truong mo rong neu co
                    if (dis.available() > 0) {
                        try {
                            roomCode = dis.readUTF();
                            if (dis.available() > 0) avatarUrl = dis.readUTF();
                            if (dis.available() > 0) extraData = dis.readUTF();
                        } catch (Exception ignored) {
                        }
                    }

                    return new ChatMessage(type, senderId, senderName, content, targetUser,
                            timestamp, roomCode, avatarUrl, extraData);
                }
            }
        } catch (Exception ignored) {
        }

        // Fallback neu la chuoi UTF-8 tho tu socket khac
        String rawText = new String(data, offset, length, StandardCharsets.UTF_8).trim();
        return new ChatMessage(MessageType.CHAT, "raw", "UDP Client", rawText, "",
                System.currentTimeMillis(), "DEFAULT", "assets/avatars/avatar1.svg", "");
    }

    // --- Helper Factory Methods ---

    public static ChatMessage createChat(String senderId, String senderName, String content) {
        return createChat(senderId, senderName, "assets/avatars/avatar1.svg", "DEFAULT", content, "");
    }

    public static ChatMessage createWhisper(String senderId, String senderName, String targetUser, String content) {
        return createChat(senderId, senderName, "assets/avatars/avatar1.svg", "DEFAULT", content, targetUser);
    }

    public static ChatMessage createHeartbeat(String senderId, String senderName) {
        return new ChatMessage(MessageType.HEARTBEAT, senderId, senderName, "HEARTBEAT", "",
                System.currentTimeMillis(), "DEFAULT", "assets/avatars/avatar1.svg", "");
    }

    public static ChatMessage createJoin(String senderId, String senderName) {
        return createJoin(senderId, senderName, "assets/avatars/avatar1.svg", "DEFAULT");
    }

    public static ChatMessage createLeave(String senderId, String senderName) {
        return createLeave(senderId, senderName, "assets/avatars/avatar1.svg", "DEFAULT");
    }

    public static ChatMessage createChat(String senderId, String senderName, String avatarUrl,
                                         String roomCode, String content, String targetUser) {
        MessageType t = (targetUser != null && !targetUser.trim().isEmpty()) ? MessageType.WHISPER : MessageType.CHAT;
        return new ChatMessage(t, senderId, senderName, content, targetUser,
                System.currentTimeMillis(), roomCode, avatarUrl, "");
    }

    public static ChatMessage createFile(String senderId, String senderName, String avatarUrl,
                                         String roomCode, String fileName, long fileSize, String fileUrl) {
        return createFile(senderId, senderName, avatarUrl, roomCode, fileName, fileSize, fileUrl, "MEMBER");
    }

    public static ChatMessage createFile(String senderId, String senderName, String avatarUrl,
                                         String roomCode, String fileName, long fileSize, String fileUrl, String role) {
        return createFile(senderId, senderName, avatarUrl, roomCode, fileName, fileSize, fileUrl, role, "", "");
    }

    public static ChatMessage createFile(String senderId, String senderName, String avatarUrl,
                                         String roomCode, String fileName, long fileSize, String fileUrl, String role,
                                         String targetUser, String targetUserId) {
        StringBuilder extra = new StringBuilder();
        extra.append("{\"fileName\":\"").append(escapeJson(fileName))
                .append("\",\"fileSize\":").append(fileSize)
                .append(",\"fileUrl\":\"").append(escapeJson(fileUrl))
                .append("\",\"role\":\"").append(role != null ? escapeJson(role) : "MEMBER").append("\"");
        if (targetUserId != null && !targetUserId.isEmpty()) {
            extra.append(",\"targetUserId\":\"").append(escapeJson(targetUserId)).append("\"");
        }
        extra.append("}");
        String content = "Đã gửi tệp: " + fileName;
        return new ChatMessage(MessageType.FILE, senderId, senderName, content, targetUser != null ? targetUser : "",
                System.currentTimeMillis(), roomCode, avatarUrl, extra.toString());
    }

    public static ChatMessage createFolder(String senderId, String senderName, String avatarUrl,
                                           String roomCode, String folderName, int fileCount, long totalSize, String zipUrl) {
        return createFolder(senderId, senderName, avatarUrl, roomCode, folderName, fileCount, totalSize, zipUrl, "MEMBER");
    }

    public static ChatMessage createFolder(String senderId, String senderName, String avatarUrl,
                                           String roomCode, String folderName, int fileCount, long totalSize, String zipUrl, String role) {
        return createFolder(senderId, senderName, avatarUrl, roomCode, folderName, fileCount, totalSize, zipUrl, role, "", "");
    }

    public static ChatMessage createFolder(String senderId, String senderName, String avatarUrl,
                                           String roomCode, String folderName, int fileCount, long totalSize, String zipUrl, String role,
                                           String targetUser, String targetUserId) {
        StringBuilder extra = new StringBuilder();
        extra.append("{\"folderName\":\"").append(escapeJson(folderName))
                .append("\",\"fileCount\":").append(fileCount)
                .append(",\"totalSize\":").append(totalSize)
                .append(",\"zipUrl\":\"").append(escapeJson(zipUrl))
                .append("\",\"role\":\"").append(role != null ? escapeJson(role) : "MEMBER").append("\"");
        if (targetUserId != null && !targetUserId.isEmpty()) {
            extra.append(",\"targetUserId\":\"").append(escapeJson(targetUserId)).append("\"");
        }
        extra.append("}");
        String content = "Đã gửi thư mục: " + folderName + " (" + fileCount + " tệp)";
        return new ChatMessage(MessageType.FOLDER, senderId, senderName, content, targetUser != null ? targetUser : "",
                System.currentTimeMillis(), roomCode, avatarUrl, extra.toString());
    }

    public static ChatMessage createLink(String senderId, String senderName, String avatarUrl,
                                         String roomCode, String message, String url, String title, String domain) {
        String extra = "{\"url\":\"" + escapeJson(url) + "\",\"title\":\"" + escapeJson(title)
                + "\",\"domain\":\"" + escapeJson(domain) + "\"}";
        return new ChatMessage(MessageType.LINK, senderId, senderName, message, "",
                System.currentTimeMillis(), roomCode, avatarUrl, extra);
    }

    public static ChatMessage createSticker(String senderId, String senderName, String avatarUrl,
                                            String roomCode, String stickerPack, String stickerId, String stickerUrl) {
        String extra = "{\"pack\":\"" + escapeJson(stickerPack) + "\",\"id\":\"" + escapeJson(stickerId)
                + "\",\"stickerUrl\":\"" + escapeJson(stickerUrl) + "\"}";
        return new ChatMessage(MessageType.STICKER, senderId, senderName, "[Sticker]", "",
                System.currentTimeMillis(), roomCode, avatarUrl, extra);
    }

    public static ChatMessage createJoin(String senderId, String senderName, String avatarUrl, String roomCode) {
        return new ChatMessage(MessageType.JOIN, senderId, senderName, senderName + " đã tham gia phòng chat.",
                "", System.currentTimeMillis(), roomCode, avatarUrl, "");
    }

    public static ChatMessage createLeave(String senderId, String senderName, String avatarUrl, String roomCode) {
        return new ChatMessage(MessageType.LEAVE, senderId, senderName, senderName + " đã rời phòng chat.",
                "", System.currentTimeMillis(), roomCode, avatarUrl, "");
    }

    public static ChatMessage createSystem(String roomCode, String content) {
        return new ChatMessage(MessageType.SYSTEM, "SYSTEM", "Hệ thống", content, "",
                System.currentTimeMillis(), roomCode, "assets/avatars/system.svg", "");
    }

    public static ChatMessage createProfileUpdate(String senderId, String senderName, String avatarUrl,
                                                  String roomCode, String bio) {
        String extra = "{\"bio\":\"" + escapeJson(bio) + "\"}";
        return new ChatMessage(MessageType.PROFILE_UPDATE, senderId, senderName,
                senderName + " đã cập nhật thông tin cá nhân.", "",
                System.currentTimeMillis(), roomCode, avatarUrl, extra);
    }

    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"type\":\"").append(type.name()).append("\",");
        sb.append("\"typeCode\":").append(type.getCode()).append(",");
        sb.append("\"senderId\":\"").append(escapeJson(senderId)).append("\",");
        sb.append("\"senderName\":\"").append(escapeJson(senderName)).append("\",");
        sb.append("\"avatarUrl\":\"").append(escapeJson(avatarUrl)).append("\",");
        sb.append("\"roomCode\":\"").append(escapeJson(roomCode)).append("\",");
        sb.append("\"content\":\"").append(escapeJson(content)).append("\",");
        sb.append("\"targetUser\":\"").append(escapeJson(targetUser)).append("\",");
        sb.append("\"targetUserId\":\"").append(escapeJson(getTargetUserId())).append("\",");
        sb.append("\"timestamp\":").append(timestamp).append(",");
        sb.append("\"formattedTime\":\"").append(getFormattedTime()).append("\",");
        sb.append("\"extraData\":").append((extraData != null && !extraData.isEmpty() && extraData.startsWith("{")) ? extraData : ("\"" + escapeJson(extraData) + "\""));
        sb.append("}");
        return sb.toString();
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
