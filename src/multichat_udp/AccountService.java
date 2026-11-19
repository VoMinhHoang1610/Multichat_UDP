package multichat_udp;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Dịch vụ Quản lý & Xác thực Tài khoản theo mẫu thiết kế Singleton.
 * Lưu trữ danh sách tài khoản trong bộ nhớ RAM (ConcurrentHashMap) và
 * tự động đồng bộ hóa xuống tập tin JSON để bảo toàn dữ liệu khi khởi động lại.
 */
public class AccountService {
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,30}$");
    private static final String DATA_FILE = "data" + File.separator + "accounts.json";
    private static final AccountService INSTANCE = new AccountService();

    private final ConcurrentHashMap<String, Account> accounts = new ConcurrentHashMap<>();

    private AccountService() {
        try {
            loadFromFile();
        } catch (Throwable t) {
            System.err.println("[AccountService] Lỗi khi load dữ liệu ban đầu: " + t.getMessage());
        }
        if (accounts.isEmpty()) {
            initDefaultAccounts();
            try {
                saveToFile();
            } catch (Throwable t) {
                System.err.println("[AccountService] Lỗi khi lưu dữ liệu mặc định: " + t.getMessage());
            }
        }
    }

    public static AccountService getInstance() {
        return INSTANCE;
    }

    /**
     * Khởi tạo danh sách tài khoản mẫu ban đầu nếu hệ thống chưa có dữ liệu.
     */
    private void initDefaultAccounts() {
        // 1. Tai khoan Quan tri vien (ADMIN)
        Account admin = new Account(
                "admin",
                Account.hashPassword("admin123"),
                "Quản Trị Viên Hệ Thống",
                "ADMIN",
                "assets/avatars/admin.svg",
                "Quản trị viên cấp cao của hệ thống Multichat UDP",
                "admin@multichat.enterprise.local",
                System.currentTimeMillis(),
                "ACTIVE"
        );
        accounts.put(admin.getUsername(), admin);

        // 2. Tai khoan Nguoi dung mau (MEMBER A)
        Account user1 = new Account(
                "user1",
                Account.hashPassword("123456"),
                "Thành Viên Nguyễn Văn A",
                "MEMBER",
                "assets/avatars/avatar1.svg",
                "Lập trình viên Java & UDP Multicast",
                "user1@multichat.enterprise.local",
                System.currentTimeMillis(),
                "ACTIVE"
        );
        accounts.put(user1.getUsername(), user1);

        // 3. Tai khoan Nguoi dung mau (MEMBER B)
        Account user2 = new Account(
                "user2",
                Account.hashPassword("123456"),
                "Thành Viên Trần Thị B",
                "MEMBER",
                "assets/avatars/avatar2.svg",
                "Xin chào! Rất vui được tham gia phòng chat",
                "user2@multichat.enterprise.local",
                System.currentTimeMillis(),
                "ACTIVE"
        );
        accounts.put(user2.getUsername(), user2);
    }

    /**
     * Xác thực thông tin đăng nhập của người dùng.
     * @param username Tên đăng nhập
     * @param password Mật khẩu dạng plain-text
     * @return Đối tượng Account nếu hợp lệ và đang ACTIVE, ngược lại null
     */
    public Account authenticate(String username, String password) {
        if (username == null || password == null) return null;
        String normalized = username.trim().toLowerCase();
        Account acc = accounts.get(normalized);
        if (acc == null) return null;

        if (!acc.isActive()) {
            return null; // Tài khoản đang bị khóa
        }

        if (acc.checkPassword(password.trim())) {
            return acc;
        }
        return null;
    }

    /**
     * Đăng ký tài khoản thành viên mới (mặc định luôn gán vai trò MEMBER).
     */
    public synchronized Account register(String username, String password, String fullName,
                                         String email, String avatarUrl) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Tên đăng nhập không được để trống!");
        }
        String normalized = username.trim().toLowerCase();
        if (!USERNAME_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Tên đăng nhập phải từ 3 đến 30 ký tự (chữ cái, số và dấu gạch dưới)!");
        }

        if (isUsernameTaken(normalized)) {
            throw new IllegalArgumentException("Tên đăng nhập '" + normalized + "' đã được sử dụng, vui lòng chọn tên khác!");
        }

        if (password == null || password.trim().length() < 6) {
            throw new IllegalArgumentException("Mật khẩu phải có độ dài từ 6 ký tự trở lên!");
        }

        String displayName = (fullName != null && !fullName.trim().isEmpty()) ? fullName.trim() : normalized;
        String userEmail = (email != null) ? email.trim() : "";
        String userAvatar = (avatarUrl != null && !avatarUrl.trim().isEmpty()) ? avatarUrl.trim() : "assets/avatars/avatar1.svg";

        Account newAcc = new Account(
                normalized,
                Account.hashPassword(password.trim()),
                displayName,
                "MEMBER", // Đăng ký luôn là MEMBER
                userAvatar,
                "Xin chào! Tôi là thành viên mới của hệ thống.",
                userEmail,
                System.currentTimeMillis(),
                "ACTIVE"
        );

        accounts.put(normalized, newAcc);
        saveToFile();
        return newAcc;
    }

    /**
     * Kiểm tra xem tên đăng nhập đã có người sử dụng chưa.
     */
    public boolean isUsernameTaken(String username) {
        if (username == null) return false;
        return accounts.containsKey(username.trim().toLowerCase());
    }

    /**
     * Lấy tài khoản theo tên đăng nhập.
     */
    public Account getAccount(String username) {
        if (username == null) return null;
        return accounts.get(username.trim().toLowerCase());
    }

    /**
     * Cập nhật thông tin tài khoản và tự động lưu xuống file.
     */
    public synchronized void updateAccount(Account account) {
        if (account == null || account.getUsername() == null) return;
        accounts.put(account.getUsername().toLowerCase(), account);
        saveToFile();
    }

    /**
     * Lấy toàn bộ danh sách tài khoản hiện có trong hệ thống.
     */
    public List<Account> getAllAccounts() {
        return new ArrayList<>(accounts.values());
    }

    private static File getDataFile() {
        String catalinaBase = System.getProperty("catalina.base");
        File dir;
        if (catalinaBase != null && !catalinaBase.trim().isEmpty()) {
            dir = new File(catalinaBase, "data");
        } else {
            dir = new File("data");
        }
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return new File(dir, "accounts.json");
    }

    /**
     * Lưu trữ toàn bộ danh sách tài khoản xuống file JSON.
     */
    public synchronized void saveToFile() {
        try {
            File file = getDataFile();
            StringBuilder sb = new StringBuilder("[\n");
            boolean first = true;
            for (Account acc : accounts.values()) {
                if (!first) sb.append(",\n");
                sb.append("  ").append(acc.toJsonFull());
                first = false;
            }
            sb.append("\n]");

            try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
                writer.write(sb.toString());
                writer.flush();
            }
        } catch (Exception e) {
            System.err.println("[AccountService] Lỗi khi lưu danh sách tài khoản: " + e.getMessage());
        }
    }

    /**
     * Đọc danh sách tài khoản từ file JSON.
     */
    public synchronized void loadFromFile() {
        try {
            File file = getDataFile();
            if (!file.exists() || file.length() == 0) {
                return;
            }

            String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8).trim();
            if (!content.startsWith("[") || !content.endsWith("]")) {
                return;
            }

            content = content.substring(1, content.length() - 1).trim();
            if (content.isEmpty()) return;

            // Tách các đối tượng JSON theo khối {...}
            int depth = 0;
            int start = -1;
            for (int i = 0; i < content.length(); i++) {
                char c = content.charAt(i);
                if (c == '{') {
                    if (depth == 0) start = i;
                    depth++;
                } else if (c == '}') {
                    depth--;
                    if (depth == 0 && start != -1) {
                        String objJson = content.substring(start, i + 1);
                        Account acc = Account.fromJson(objJson);
                        if (acc != null && acc.getUsername() != null && !acc.getUsername().isEmpty()) {
                            accounts.put(acc.getUsername(), acc);
                        }
                        start = -1;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[AccountService] Lỗi khi tải tài khoản từ file: " + e.getMessage());
        }
    }
}
