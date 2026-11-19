package multichat_udp;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lớp đối tượng Account đại diện cho tài khoản người dùng trong hệ thống Multichat UDP.
 * Phân quyền dựa trên vai trò: ADMIN (Quản trị viên) hoặc MEMBER (Thành viên).
 */
public class Account implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private String username;
    private String passwordHash;
    private String fullName;
    private String role; // "ADMIN" hoặc "MEMBER"
    private String avatarUrl;
    private String bio;
    private String email;
    private long createdAt;
    private String status; // "ACTIVE" hoặc "BLOCKED"

    public Account() {
        this.createdAt = System.currentTimeMillis();
        this.role = "MEMBER";
        this.status = "ACTIVE";
        this.avatarUrl = "assets/avatars/avatar1.svg";
        this.bio = "";
        this.email = "";
    }

    public Account(String username, String passwordHash, String fullName, String role,
                   String avatarUrl, String bio, String email, long createdAt, String status) {
        this.username = (username != null) ? username.trim().toLowerCase() : "";
        this.passwordHash = (passwordHash != null) ? passwordHash : "";
        this.fullName = (fullName != null && !fullName.trim().isEmpty()) ? fullName.trim() : this.username;
        this.role = ("ADMIN".equalsIgnoreCase(role)) ? "ADMIN" : "MEMBER";
        this.avatarUrl = (avatarUrl != null && !avatarUrl.trim().isEmpty()) ? avatarUrl.trim() : ("ADMIN".equals(this.role) ? "assets/avatars/admin.svg" : "assets/avatars/avatar1.svg");
        this.bio = (bio != null) ? bio.trim() : "";
        this.email = (email != null) ? email.trim() : "";
        this.createdAt = (createdAt > 0) ? createdAt : System.currentTimeMillis();
        this.status = ("BLOCKED".equalsIgnoreCase(status)) ? "BLOCKED" : "ACTIVE";
    }

    /**
     * Mã hóa mật khẩu plain-text sang chuỗi SHA-256 Hex.
     */
    public static String hashPassword(String plainPassword) {
        if (plainPassword == null) return "";
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(plainPassword.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Kiểm tra mật khẩu plain-text có khớp với passwordHash không.
     */
    public boolean checkPassword(String plainPassword) {
        if (plainPassword == null || passwordHash == null) return false;
        String hashed = hashPassword(plainPassword);
        return passwordHash.equalsIgnoreCase(hashed) || passwordHash.equals(plainPassword);
    }

    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(this.role);
    }

    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(this.status);
    }

    // Getters and Setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = (username != null) ? username.trim().toLowerCase() : "";
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = (fullName != null && !fullName.trim().isEmpty()) ? fullName.trim() : this.username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = ("ADMIN".equalsIgnoreCase(role)) ? "ADMIN" : "MEMBER";
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

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = ("BLOCKED".equalsIgnoreCase(status)) ? "BLOCKED" : "ACTIVE";
    }

    public String getFormattedCreatedAt() {
        return DATE_FORMAT.format(new Date(createdAt));
    }

    /**
     * Trả về JSON công khai (không lộ passwordHash) phục vụ API và Client.
     */
    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"username\":\"").append(escapeJson(username)).append("\",");
        sb.append("\"fullName\":\"").append(escapeJson(fullName)).append("\",");
        sb.append("\"role\":\"").append(escapeJson(role)).append("\",");
        sb.append("\"avatarUrl\":\"").append(escapeJson(avatarUrl)).append("\",");
        sb.append("\"bio\":\"").append(escapeJson(bio)).append("\",");
        sb.append("\"email\":\"").append(escapeJson(email)).append("\",");
        sb.append("\"createdAt\":").append(createdAt).append(",");
        sb.append("\"formattedCreatedAt\":\"").append(getFormattedCreatedAt()).append("\",");
        sb.append("\"status\":\"").append(escapeJson(status)).append("\",");
        sb.append("\"isAdmin\":").append(isAdmin());
        sb.append("}");
        return sb.toString();
    }

    /**
     * Trả về JSON đầy đủ bao gồm passwordHash để lưu trữ vào file dữ liệu.
     */
    public String toJsonFull() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"username\":\"").append(escapeJson(username)).append("\",");
        sb.append("\"passwordHash\":\"").append(escapeJson(passwordHash)).append("\",");
        sb.append("\"fullName\":\"").append(escapeJson(fullName)).append("\",");
        sb.append("\"role\":\"").append(escapeJson(role)).append("\",");
        sb.append("\"avatarUrl\":\"").append(escapeJson(avatarUrl)).append("\",");
        sb.append("\"bio\":\"").append(escapeJson(bio)).append("\",");
        sb.append("\"email\":\"").append(escapeJson(email)).append("\",");
        sb.append("\"createdAt\":").append(createdAt).append(",");
        sb.append("\"status\":\"").append(escapeJson(status)).append("\"");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Chuyển đổi JSON string thành đối tượng Account mà không cần thư viện bên ngoài.
     */
    public static Account fromJson(String json) {
        if (json == null || json.trim().isEmpty()) return null;
        try {
            String u = extractField(json, "username");
            String p = extractField(json, "passwordHash");
            String fn = extractField(json, "fullName");
            String r = extractField(json, "role");
            String av = extractField(json, "avatarUrl");
            String b = extractField(json, "bio");
            String em = extractField(json, "email");
            String st = extractField(json, "status");
            String caStr = extractField(json, "createdAt");
            long ca = System.currentTimeMillis();
            if (caStr != null && !caStr.isEmpty()) {
                try {
                    ca = Long.parseLong(caStr);
                } catch (Exception ignored) {
                }
            }
            if (u == null || u.isEmpty()) return null;
            return new Account(u, p, fn, r, av, b, em, ca, st);
        } catch (Exception e) {
            return null;
        }
    }

    private static String extractField(String json, String field) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(field) + "\"\\s*:\\s*(?:\"([^\"]*)\"|([0-9]+))");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            if (matcher.group(1) != null) return matcher.group(1);
            if (matcher.group(2) != null) return matcher.group(2);
        }
        return "";
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
