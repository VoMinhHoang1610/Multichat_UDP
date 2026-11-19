package multichat_udp;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Thong tin ca nhan va trang thai hoat dong cua thanh vien trong phong chat.
 */
public class UserProfile implements Serializable {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private String userId;
    private String username;
    private String avatarUrl;
    private String bio;
    private String email;
    private String role; // "ADMIN" hoac "MEMBER"
    private String ipAddress;
    private long joinTime;
    private long lastActiveTime;
    private int messageCount;
    private boolean muted;

    public UserProfile(String userId, String username, String avatarUrl, String role, String ipAddress) {
        this.userId = userId != null ? userId : java.util.UUID.randomUUID().toString();
        this.username = username != null ? username : "NguoiDung";
        this.avatarUrl = (avatarUrl != null && !avatarUrl.isEmpty()) ? avatarUrl : "assets/avatars/avatar1.svg";
        this.bio = "Xin chào! Tôi đang tham gia UDP Multichat.";
        this.email = "";
        this.role = role != null ? role : "MEMBER";
        this.ipAddress = ipAddress != null ? ipAddress : "127.0.0.1";
        this.joinTime = System.currentTimeMillis();
        this.lastActiveTime = System.currentTimeMillis();
        this.messageCount = 0;
        this.muted = false;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public long getJoinTime() {
        return joinTime;
    }

    public void setJoinTime(long joinTime) {
        this.joinTime = joinTime;
    }

    public long getLastActiveTime() {
        return lastActiveTime;
    }

    public void touch() {
        this.lastActiveTime = System.currentTimeMillis();
    }

    public int getMessageCount() {
        return messageCount;
    }

    public void incrementMessageCount() {
        this.messageCount++;
    }

    public boolean isMuted() {
        return muted;
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
    }

    public String getFormattedJoinTime() {
        return DATE_FORMAT.format(new Date(joinTime));
    }

    public String getFormattedLastActive() {
        return DATE_FORMAT.format(new Date(lastActiveTime));
    }

    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"userId\":\"").append(escapeJson(userId)).append("\",");
        sb.append("\"username\":\"").append(escapeJson(username)).append("\",");
        sb.append("\"avatarUrl\":\"").append(escapeJson(avatarUrl)).append("\",");
        sb.append("\"bio\":\"").append(escapeJson(bio)).append("\",");
        sb.append("\"email\":\"").append(escapeJson(email)).append("\",");
        sb.append("\"role\":\"").append(escapeJson(role)).append("\",");
        sb.append("\"ipAddress\":\"").append(escapeJson(ipAddress)).append("\",");
        sb.append("\"joinTime\":").append(joinTime).append(",");
        sb.append("\"lastActiveTime\":").append(lastActiveTime).append(",");
        sb.append("\"formattedJoinTime\":\"").append(getFormattedJoinTime()).append("\",");
        sb.append("\"formattedLastActive\":\"").append(getFormattedLastActive()).append("\",");
        sb.append("\"messageCount\":").append(messageCount).append(",");
        sb.append("\"muted\":").append(muted);
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
