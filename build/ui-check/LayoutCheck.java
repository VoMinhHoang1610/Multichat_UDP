import multichat_udp.*;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import java.lang.reflect.*;
import java.util.Set;

public class LayoutCheck {
    static MultichatUI ui;
    static FakeService service = new FakeService();
    public static class FakeService extends MulticastChatService {
        boolean connected = true;
        int chats, whispers;
        String target, content;
        @Override public boolean isConnected() { return connected; }
        @Override public String getUsername() { return "An"; }
        @Override public String getGroupAddress() { return "224.2.2.3"; }
        @Override public int getPort() { return 8888; }
        @Override public void sendMessage(String text) { chats++; content = text; }
        @Override public void sendWhisper(String user, String text) { whispers++; target = user; content = text; }
    }
    static Object field(String name) throws Exception {
        Field f = MultichatUI.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(ui);
    }
    static void call(String name, Class<?>[] types, Object... args) throws Exception {
        Method m = MultichatUI.class.getDeclaredMethod(name, types);
        m.setAccessible(true);
        m.invoke(ui, args);
    }
    static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    static void layout(Component c) {
        if (c instanceof Container container) {
            container.doLayout();
            for (Component child : container.getComponents()) layout(child);
        }
    }
    static void capture(int width, int height, String name, boolean sidebar) throws Exception {
        ui.setSize(width, height);
        call("setSidebarVisible", new Class<?>[]{boolean.class}, sidebar);
        for (int i = 0; i < 5; i++) layout(ui);
        JPanel transcript = (JPanel) field("transcript");
        Component left = ((Container)transcript.getComponent(1)).getComponent(0);
        Component right = ((Container)transcript.getComponent(2)).getComponent(0);
        require(left.getX() == 18 && right.getX() > left.getX(), "Bubble alignment");
        require(right.getX() + right.getWidth() <= transcript.getWidth(), "Right bubble overflow");
        for (String buttonName : new String[]{"btnSave", "btnClear", "btnSend", "btnSidebar", "btnConnect", "btnPrivate"}) {
            JButton button = (JButton) field(buttonName);
            if (!sidebar && (buttonName.equals("btnConnect") || buttonName.equals("btnPrivate"))) continue;
            Rectangle r = SwingUtilities.convertRectangle(button.getParent(), button.getBounds(), ui);
            require(r.x >= 0 && r.x + r.width <= width && r.y >= 0 && r.y + r.height <= height, buttonName + " clipped");
        }
        BufferedImage image = new BufferedImage(ui.getContentPane().getWidth(), ui.getContentPane().getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        ui.getContentPane().printAll(g);
        g.dispose();
        ImageIO.write(image, "png", new File("build/ui-check/" + name + ".png"));
        System.out.println(name + ": bubble alignment / toolbar bounds passed");
    }
    public static void main(String[] args) throws Exception {
        try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                    ui = new MultichatUI();
                    ui.addNotify();
                    Field serviceField = MultichatUI.class.getDeclaredField("chatService");
                    serviceField.setAccessible(true);
                    serviceField.set(ui, service);
                    call("updateUIState", new Class<?>[]{boolean.class}, true);
                    ui.onUserListUpdated(Set.of("An", "Bình", "Chi", "Dũng"));
                    ui.onMessageReceived(ChatMessage.createJoin("other", "Bình"), false);
                    ui.onMessageReceived(ChatMessage.createChat("other", "Bình", "Chào mọi người! Hôm nay cùng trao đổi bài tập nhé."), false);
                    ui.onMessageReceived(ChatMessage.createChat("self", "An", "Được nhé! Mình đã chuẩn bị xong rồi."), true);
                    ui.onMessageReceived(ChatMessage.createChat("other", "Chi", "Mình cũng tham gia. Giao diện mới dễ theo dõi hơn nhiều!"), false);
                    ui.onMessageReceived(ChatMessage.createWhisper("self", "An", "Bình", "Mình gửi riêng phần ghi chú cho bạn nhé."), true);
                } catch (Exception e) { throw new RuntimeException(e); }
            });
            SwingUtilities.invokeAndWait(() -> {
                try {
                    capture(1120, 860, "desktop", true);
                    capture(780, 700, "compact", false);
                    JComboBox<?> recipients = (JComboBox<?>) field("recipients");
                    require(recipients.getItemCount() == 4, "Recipient list must exclude self");
                    JList<?> members = (JList<?>) field("memberList");
                    members.setSelectedValue("Bình", true);
                    ((JButton)field("btnPrivate")).doClick();
                    require(recipients.getSelectedItem().toString().equals("Riêng: Bình"), "Private recipient selection");
                    JTextArea input = (JTextArea)field("txtInput");
                    input.setText("Xin chào Bình\nDòng thứ hai");
                    ((JButton)field("btnSend")).doClick();
                    require(service.whispers == 1 && service.target.equals("Bình") && service.content.contains("\n"), "Private multiline routing");
                    require(input.getText().isEmpty(), "Composer not cleared");
                    require(!((JButton)field("btnSend")).isEnabled(), "Blank send should be disabled");
                    recipients.setSelectedIndex(0);
                    input.setText("Chào cả nhóm");
                    input.getActionMap().get("send").actionPerformed(null);
                    require(service.chats == 1 && service.whispers == 1, "Group routing");
                    Object enter = input.getInputMap().get(KeyStroke.getKeyStroke("shift ENTER"));
                    require("insert-break".equals(enter), "Shift Enter binding");
                    // Retain selected target across heartbeat updates.
                    recipients.setSelectedIndex(1);
                    call("refreshMembers", new Class<?>[]{Set.class}, Set.of("An", "Bình", "Chi", "Dũng"));
                    require(recipients.getSelectedItem().toString().equals("Riêng: Bình"), "Recipient lost during heartbeat");
                    call("refreshMembers", new Class<?>[]{Set.class}, Set.of("An", "Chi"));
                    require(recipients.getSelectedIndex() == 0, "Offline recipient not reset");
                    call("clearConversation", new Class<?>[]{});
                    require(((StringBuilder)field("chatLog")).length() == 0, "Clear log");
                    require(!((JButton)field("btnSave")).isEnabled(), "Save empty conversation");
                    service.connected = false;
                    call("updateUIState", new Class<?>[]{boolean.class}, false);
                    require(recipients.getItemCount() == 1 && members.getModel().getSize() == 0, "Disconnect state");
                    require(!input.isEnabled(), "Disconnected composer");
                    System.out.println("PASS: members, private/group routing, multiline, heartbeat, offline recipient, clear, disconnect");
                } catch (Exception e) { throw new RuntimeException(e); }
            });
        } finally {
            if (ui != null) SwingUtilities.invokeAndWait(() -> ui.dispose());
        }
    }
}
