package multichat_udp;

import java.util.Set;

/**
 * Giao dien nhan cac su kien chat tu MulticastChatService.
 */
public interface ChatListener {
    // Nhan tin tu service de hien len GUI hoac CLI.
    void onMessageReceived(ChatMessage message, boolean isSelf);
    // Nhan danh sach thanh vien online tu service.
    void onUserListUpdated(Set<String> onlineUsers);
    // Nhan thay doi trang thai ket noi tu service.
    void onStatusChanged(String status, boolean isConnected);
    // Nhan loi tu service va thong bao cho nguoi dung.
    void onError(String errorMessage);
}

