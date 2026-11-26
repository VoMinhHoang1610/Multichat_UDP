package multichat_udp;

import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Phong chat UDP Multicast do Admin quan ly, chua danh sach user va lich su tin nhan.
 */
public class ChatRoom implements Serializable {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final int MAX_HISTORY = 1000;

    private String roomCode;
    private String roomName;
    private String description;
    private String adminName;
    private String adminPin;
    private String multicastIp;
    private int multicastPort;
    private int maxUsers;
    private long createdAt;
    private String status; // "ACTIVE" hoac "LOCKED"

    private final ConcurrentHashMap<String, UserProfile> activeUsers = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<ChatMessage> messageHistory = new CopyOnWriteArrayList<>();
    private transient UDPMulticastEngine udpEngine;

    public ChatRoom(String roomCode, String roomName, String description, String adminName,
                    String adminPin, String multicastIp, int multicastPort, int maxUsers) {
        this.roomCode = roomCode != null ? roomCode.trim().toUpperCase() : "DEFAULT";
        this.roomName = (roomName != null && !roomName.trim().isEmpty()) ? roomName.trim() : "Phòng Chat " + this.roomCode;
        this.description = description != null ? description : "Phòng chat nhóm UDP Multicast";
        this.adminName = (adminName != null && !adminName.trim().isEmpty()) ? adminName.trim() : "Admin";
        this.adminPin = (adminPin != null && !adminPin.trim().isEmpty()) ? adminPin.trim() : "123456";
        this.multicastIp = (multicastIp != null && !multicastIp.trim().isEmpty()) ? multicastIp.trim() : "224.2.2.3";
        this.multicastPort = multicastPort > 0 ? multicastPort : 8888;
        this.maxUsers = maxUsers > 0 ? maxUsers : 50;
        this.createdAt = System.currentTimeMillis();
        this.status = "ACTIVE";
    }

    public synchronized void initUDP() throws IOException {
        if (udpEngine == null) {
            udpEngine = new UDPMulticastEngine(this, multicastIp, multicastPort);
            udpEngine.start();
        }
    }

    public synchronized void closeUDP() {
        if (udpEngine != null) {
            udpEngine.stop();
            udpEngine = null;
        }
    }

    public UDPMulticastEngine getUdpEngine() {
        return udpEngine;
    }

    /**
     * Phat tin nhan qua mang UDP Multicast cho tat ca client.
     */
    public void broadcastUDP(ChatMessage msg) throws IOException {
        if (udpEngine != null && udpEngine.isRunning()) {
            udpEngine.sendMessage(msg);
        } else {
            // Neu socket tam thoi chua mo, luu thang vao lich su
            onMessageReceived(msg);
        }
    }

    /**
     * Xu ly khi nhan duoc goi tin tu socket UDP Multicast.
     */
    public void onMessageReceived(ChatMessage msg) {
        if (msg == null) return;

        // Cap nhat thong tin hoat dong cua user
        String senderId = msg.getSenderId();
        if (senderId != null && !senderId.isEmpty() && !senderId.equals("SYSTEM") && !senderId.equals("SERVER")) {
            UserProfile user = activeUsers.get(senderId);
            if (user != null) {
                user.touch();
                if (msg.getType() == MessageType.CHAT || msg.getType() == MessageType.FILE
                        || msg.getType() == MessageType.FOLDER || msg.getType() == MessageType.STICKER
                        || msg.getType() == MessageType.LINK || msg.getType() == MessageType.WHISPER) {
                    user.incrementMessageCount();
                }
            } else {
                // Tu dong them user neu chua co trong danh sach phong
                UserProfile newUser = new UserProfile(senderId, msg.getSenderName(), msg.getAvatarUrl(), "MEMBER", "127.0.0.1");
                activeUsers.put(senderId, newUser);
            }
        }

        // Bo qua heartbeat trong lich su tin nhan de tiet kiem bo nho
        if (msg.getType() == MessageType.HEARTBEAT) {
            return;
        }

        // Xu ly roi nhom
        if (msg.getType() == MessageType.LEAVE && senderId != null) {
            activeUsers.remove(senderId);
        }

        // Them tin nhan vao bo dem lich su
        messageHistory.add(msg);
        if (messageHistory.size() > MAX_HISTORY) {
            messageHistory.remove(0);
        }
    }

    public boolean addUser(UserProfile user) {
        if (activeUsers.size() >= maxUsers) {
            return false;
        }
        activeUsers.put(user.getUserId(), user);
        return true;
    }

    public UserProfile removeUser(String userId) {
        return activeUsers.remove(userId);
    }

    public UserProfile getUser(String userId) {
        return activeUsers.get(userId);
    }

    /**
     * Tim thanh vien trong phong theo userId (uu tien) hoac username (khong phan biet hoa thuong).
     */
    public UserProfile findTargetUser(String targetUserId, String targetUsername) {
        if (targetUserId != null && !targetUserId.trim().isEmpty()) {
            UserProfile byId = activeUsers.get(targetUserId.trim());
            if (byId != null) {
                return byId;
            }
        }
        if (targetUsername != null && !targetUsername.trim().isEmpty()) {
            String trimmed = targetUsername.trim();
            for (UserProfile u : activeUsers.values()) {
                if (u.getUsername() != null && u.getUsername().equalsIgnoreCase(trimmed)) {
                    return u;
                }
                if (u.getUserId() != null && u.getUserId().equalsIgnoreCase(trimmed)) {
                    return u;
                }
            }
        }
        return null;
    }

    /**
     * Kiem tra quyen xem tin nhan: tin cong khai cho tat ca, tin nhan rieng (Whisper) chi cho nguoi gui hoac nguoi nhan.
     */
    public static boolean isMessageVisibleTo(ChatMessage msg, UserProfile viewer) {
        if (msg == null) {
            return false;
        }
        if (!msg.isWhisper()) {
            return true;
        }
        if (viewer == null) {
            return false;
        }
        String viewerId = viewer.getUserId();
        String viewerName = viewer.getUsername();

        // Nguoi gui luon xem duoc tin nhan rieng cua minh
        if (viewerId != null && !viewerId.isEmpty() && viewerId.equals(msg.getSenderId())) {
            return true;
        }

        // Neu tin nhan co dinh kem targetUserId, chi so khop chinh xac theo userId
        String targetId = msg.getTargetUserId();
        if (targetId != null && !targetId.isEmpty()) {
            return viewerId != null && viewerId.equals(targetId);
        }

        // Fallback cho tin nhan tu CLI/Desktop chi co targetUser (username)
        String targetName = msg.getTargetUser();
        if (targetName != null && !targetName.trim().isEmpty()) {
            String trimmedTarget = targetName.trim();
            if (viewerId != null && viewerId.equalsIgnoreCase(trimmedTarget)) {
                return true;
            }
            if (viewerName != null && viewerName.equalsIgnoreCase(trimmedTarget)) {
                return true;
            }
        }
        return false;
    }

    public List<UserProfile> getActiveUsersList() {
        return new ArrayList<>(activeUsers.values());
    }

    public int getUserCount() {
        return activeUsers.size();
    }

    public List<ChatMessage> getMessagesSince(long lastTimestamp) {
        List<ChatMessage> result = new ArrayList<>();
        for (ChatMessage m : messageHistory) {
            if (m.getTimestamp() > lastTimestamp) {
                result.add(m);
            }
        }
        return result;
    }

    public List<ChatMessage> getMessagesSince(long lastTimestamp, UserProfile viewer) {
        List<ChatMessage> result = new ArrayList<>();
        for (ChatMessage m : messageHistory) {
            if (m.getTimestamp() > lastTimestamp && isMessageVisibleTo(m, viewer)) {
                result.add(m);
            }
        }
        return result;
    }

    public List<ChatMessage> getRecentMessages(int count) {
        int size = messageHistory.size();
        if (size <= count) {
            return new ArrayList<>(messageHistory);
        }
        return new ArrayList<>(messageHistory.subList(size - count, size));
    }

    public List<ChatMessage> getRecentMessages(int count, UserProfile viewer) {
        List<ChatMessage> visible = new ArrayList<>();
        for (ChatMessage m : messageHistory) {
            if (isMessageVisibleTo(m, viewer)) {
                visible.add(m);
            }
        }
        int size = visible.size();
        if (size <= count) {
            return visible;
        }
        return new ArrayList<>(visible.subList(size - count, size));
    }

    public void clearHistory() {
        messageHistory.clear();
    }

    public void cleanInactiveUsers(long timeoutMs) {
        long now = System.currentTimeMillis();
        for (Map.Entry<String, UserProfile> entry : activeUsers.entrySet()) {
            if (now - entry.getValue().getLastActiveTime() > timeoutMs) {
                activeUsers.remove(entry.getKey());
            }
        }
    }

    public boolean verifyAdminPin(String pin) {
        return adminPin != null && adminPin.equals(pin);
    }

    // Getters and Setters
    public String getRoomCode() {
        return roomCode;
    }

    public void setRoomCode(String roomCode) {
        this.roomCode = roomCode;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAdminName() {
        return adminName;
    }

    public void setAdminName(String adminName) {
        this.adminName = adminName;
    }

    public String getAdminPin() {
        return adminPin;
    }

    public void setAdminPin(String adminPin) {
        this.adminPin = adminPin;
    }

    public String getMulticastIp() {
        return multicastIp;
    }

    public void setMulticastIp(String multicastIp) {
        this.multicastIp = multicastIp;
    }

    public int getMulticastPort() {
        return multicastPort;
    }

    public void setMulticastPort(int multicastPort) {
        this.multicastPort = multicastPort;
    }

    public int getMaxUsers() {
        return maxUsers;
    }

    public void setMaxUsers(int maxUsers) {
        this.maxUsers = maxUsers;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public String getFormattedCreatedAt() {
        return DATE_FORMAT.format(new Date(createdAt));
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"roomCode\":\"").append(escapeJson(roomCode)).append("\",");
        sb.append("\"roomName\":\"").append(escapeJson(roomName)).append("\",");
        sb.append("\"description\":\"").append(escapeJson(description)).append("\",");
        sb.append("\"adminName\":\"").append(escapeJson(adminName)).append("\",");
        sb.append("\"multicastIp\":\"").append(escapeJson(multicastIp)).append("\",");
        sb.append("\"multicastPort\":").append(multicastPort).append(",");
        sb.append("\"maxUsers\":").append(maxUsers).append(",");
        sb.append("\"userCount\":").append(activeUsers.size()).append(",");
        sb.append("\"messageCount\":").append(messageHistory.size()).append(",");
        sb.append("\"status\":\"").append(escapeJson(status)).append("\",");
        sb.append("\"createdAt\":").append(createdAt).append(",");
        sb.append("\"formattedCreatedAt\":\"").append(getFormattedCreatedAt()).append("\"");
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
