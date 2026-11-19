package multichat_udp;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/** Diem khoi chay; mac dinh mo GUI, tham so cli mo console. */
public class Multichat_UDP {

    // Diem bat dau; chon giao dien cua so hoac dong lenh.
    public static void main(String[] args) {
        if (args.length > 0 && "cli".equalsIgnoreCase(args[0])) {
            // Chay CLI va khong mo cua so GUI.
            new MultichatCLI().start(args);
            return;
        }

        // Chay giao dien do hoa GUI
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        // Dua cong viec cap nhat GUI ve luong Swing.
        SwingUtilities.invokeLater(() -> {
            MultichatUI ui = new MultichatUI();
            // Hien cua so chat.
            ui.setVisible(true);
        });
    }
}
