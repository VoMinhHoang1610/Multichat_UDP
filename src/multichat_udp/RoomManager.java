package multichat_udp;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Quan ly tap trung tat ca cac phong chat UDP trong he thong.
 */
public class RoomManager {
    private static final RoomManager INSTANCE = new RoomManager();
    private static final long USER_TIMEOUT_MS = 25000; // 25 giay khong hoat dong xem la offline

    private final ConcurrentHashMap<String, ChatRoom> rooms = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupExecutor;

    private RoomManager() {
        // Khoi tao phong mac dinh cho he thong
        initDefaultRooms();

        // Bo don dep thanh vien offline dinh ky
        cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "RoomManager-Cleanup");
            t.setDaemon(true);
            return t;
        });
        cleanupExecutor.scheduleAtFixedRate(this::cleanupAllInactiveUsers, 10, 10, TimeUnit.SECONDS);
    }

    public static RoomManager getInstance() {
        return INSTANCE;
    }

    private void initDefaultRooms() {
        try {
            ChatRoom defaultRoom = new ChatRoom(
                    "LTM2026",
                    "Phòng Thảo Luận Lập Trình Mạng",
                    "Phòng chat UDP Multicast chính thức - Môn Lập Trình Mạng",
                    "Admin_Hoang",
                    "123456",
                    "224.2.2.3",
                    8888,
                    100
            );
            defaultRoom.initUDP();
            rooms.put(defaultRoom.getRoomCode(), defaultRoom);

            ChatRoom devRoom = new ChatRoom(
                    "CYBER88",
                    "Phòng Công Nghệ & Gaming",
                    "Kênh chia sẻ file, link tài liệu và thảo luận công nghệ",
                    "Admin",
                    "123456",
                    "224.2.2.4",
                    8889,
                    50
            );
            devRoom.initUDP();
            rooms.put(devRoom.getRoomCode(), devRoom);

        } catch (Exception e) {
            System.err.println("[RoomManager] Loi khoi tao phong mac dinh: " + e.getMessage());
        }
    }

    public ChatRoom getRoom(String roomCode) {
        if (roomCode == null) return null;
        return rooms.get(roomCode.trim().toUpperCase());
    }

    public synchronized ChatRoom createRoom(String roomCode, String roomName, String description,
                                            String adminName, String adminPin, String multicastIp,
                                            int multicastPort, int maxUsers) throws IOException {
        if (roomCode == null || roomCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Mã phòng không được để trống!");
        }

        String normalizedCode = roomCode.trim().toUpperCase();
        if (rooms.containsKey(normalizedCode)) {
            throw new IllegalArgumentException("Mã phòng " + normalizedCode + " đã tồn tại!");
        }

        ChatRoom room = new ChatRoom(normalizedCode, roomName, description, adminName,
                adminPin, multicastIp, multicastPort, maxUsers);
        room.initUDP();
        rooms.put(normalizedCode, room);

        return room;
    }

    public synchronized boolean deleteRoom(String roomCode, String adminPin) {
        if (roomCode == null) return false;
        ChatRoom room = rooms.get(roomCode.trim().toUpperCase());
        if (room != null && room.verifyAdminPin(adminPin)) {
            room.closeUDP();
            rooms.remove(roomCode.trim().toUpperCase());
            return true;
        }
        return false;
    }

    public Collection<ChatRoom> getAllRooms() {
        return rooms.values();
    }

    private void cleanupAllInactiveUsers() {
        for (ChatRoom room : rooms.values()) {
            try {
                room.cleanInactiveUsers(USER_TIMEOUT_MS);
            } catch (Exception ignored) {
            }
        }
    }

    public void shutdown() {
        cleanupExecutor.shutdownNow();
        for (ChatRoom room : rooms.values()) {
            try {
                room.closeUDP();
            } catch (Exception ignored) {
            }
        }
    }
}
