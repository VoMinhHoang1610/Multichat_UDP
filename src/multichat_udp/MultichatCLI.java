package multichat_udp;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Set;

/** Giao dien dong lenh dung chung service UDP voi GUI. */
public class MultichatCLI implements ChatListener {

    // Bo xu ly ket noi, gui va nhan UDP.
    private final MulticastChatService chatService;

    // Tao bo xu ly mang cho che do dong lenh.
    public MultichatCLI() {
        this.chatService = new MulticastChatService();
        this.chatService.addListener(this);
    }

    // Doc tham so, ket noi va xu ly tung dong lenh CLI.
    public void start(String[] args) {
        String username = args.length > 1 ? args[1] : "User_" + (int)(Math.random() * 900 + 100);
        String groupIp = args.length > 2 ? args[2] : MulticastChatService.DEFAULT_GROUP_IP;
        int port = args.length > 3 ? Integer.parseInt(args[3]) : MulticastChatService.DEFAULT_PORT;

        System.out.println("==================================================");
        System.out.println("          MULTICHAT UDP (CONSOLE CLI)             ");
        System.out.println("==================================================");
        System.out.println("Đang kết nối tới nhóm Multicast " + groupIp + ":" + port + " ...");

        try {
            // CLI tham gia nhom va de service tu chon card.
            chatService.connect(username, groupIp, port, null);
            System.out.println("-> Đã tham gia nhóm thành công với tên: [" + username + "]");
            System.out.println("-> Hướng dẫn:");
            System.out.println("   - Nhập nội dung và Enter để gửi tin nhắn cho nhóm");
            System.out.println("   - /w <Tên> <Nội dung> để thì thầm (gửi riêng)");
            System.out.println("   - /users để xem danh sách thành viên online");
            System.out.println("   - 'exit' để rời nhóm và thoát chương trình");
            System.out.println("--------------------------------------------------");

            // Doc van ban nguoi dung go vao console.
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            String line;
            // Doc tung dong den khi dau vao dong.
            while ((line = reader.readLine()) != null) {
                // Bo khoang trang hai dau dong lenh.
                line = line.trim();
                // Hai lenh nay ket thuc vong doc ban phim.
                if (line.equalsIgnoreCase("exit") || line.equalsIgnoreCase("quit")) {
                    break;
                }

                // Bo qua dong trong.
                if (line.isEmpty()) {
                    continue;
                }

                // Hien moi in tieu de, chua in danh sach thanh vien.
                if (line.equalsIgnoreCase("/users")) {
                    System.out.println("[Thành viên online hiện tại]");
                    continue;
                }

                // Nhan dien lenh gui tin rieng.
                if (line.startsWith("/w ") || line.startsWith("/whisper ")) {
                    // Tach thanh lenh, ten nhan va phan noi dung con lai.
                    String[] parts = line.split("\\s+", 3);
                    // Chi gui khi co du ten va noi dung.
                    if (parts.length >= 3) {
                        // Gui rieng theo ten va noi dung tach tu lenh.
                        chatService.sendWhisper(parts[1], parts[2]);
                    } else {
                        System.out.println("Cú pháp: /w <Tên> <Nội dung>");
                    }
                    continue;
                }

                // Gui dong van ban cho ca nhom.
                chatService.sendMessage(line);
            }

            // Bao roi nhom va don tai nguyen mang.
            chatService.disconnect();
            System.out.println("Đã rời nhóm và thoát ứng dụng.");
        } catch (Exception e) {
            System.err.println("Lỗi: " + e.getMessage());
            // Bao roi nhom va don tai nguyen mang.
            chatService.disconnect();
        }
    }

    // Nhan tin tu service de hien len GUI hoac CLI.
    @Override
    public void onMessageReceived(ChatMessage message, boolean isSelf) {
        // Lay gio tao tin de hien tren console.
        String time = message.getFormattedTime();
        switch (message.getType()) {
            // Thong bao co nguoi vua tham gia.
            case JOIN:
                System.out.println(String.format("[%s] [THAM GIA] %s đã vào nhóm.", time, message.getSenderName()));
                break;
            // Thong bao co nguoi vua roi nhom.
            case LEAVE:
                System.out.println(String.format("[%s] [RỜI NHÓM] %s đã thoát.", time, message.getSenderName()));
                break;
            // Tin rieng; can loc dung nguoi nhan.
            case WHISPER:
                if (isSelf) {
                    System.out.println(String.format("[%s] [Thì thầm tới @%s]: %s", time, message.getTargetUser(), message.getContent()));
                } else {
                    System.out.println(String.format("[%s] [Thì thầm từ @%s]: %s", time, message.getSenderName(), message.getContent()));
                }
                break;
            // Tin chung cho tat ca thanh vien.
            case CHAT:
            default:
                if (isSelf) {
                    System.out.println(String.format("[%s] [Tôi]: %s", time, message.getContent()));
                } else {
                    System.out.println(String.format("[%s] [%s]: %s", time, message.getSenderName(), message.getContent()));
                }
                break;
        }
    }

    // Nhan danh sach thanh vien online tu service.
    @Override
    public void onUserListUpdated(Set<String> onlineUsers) {
        // Co the in log khi danh sach thay doi neu can thiet
    }

    // Nhan thay doi trang thai ket noi tu service.
    @Override
    public void onStatusChanged(String status, boolean isConnected) {
        System.out.println("[Trạng thái]: " + status);
    }

    // Nhan loi tu service va thong bao cho nguoi dung.
    @Override
    public void onError(String errorMessage) {
        System.err.println("[Lỗi]: " + errorMessage);
    }
}

