package multichat_udp;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.awt.datatransfer.StringSelection;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.util.Set;

/** Giao dien Swing voi tin nhan trai/phai va cac nut chuc nang. */
public class MultichatUI extends JFrame implements ChatListener {
    // Mau nen chung.
    private static final Color BACKGROUND = new Color(244, 247, 251);
    // Mau chu chinh.
    private static final Color INK = new Color(30, 41, 59);
    // Mau chu phu.
    private static final Color MUTED = new Color(87, 104, 126);
    // Mau nhan va mau tin cua minh.
    private static final Color BLUE = new Color(37, 99, 235);
    // Bo xu ly ket noi, gui va nhan UDP.
    private final MulticastChatService chatService;
    // Cac o nhap ten, IP nhom va cong UDP.
    private JTextField txtUsername, txtGroupIp, txtPort;
    // O soan tin nhieu dong.
    private JTextArea txtInput;
    // Hop chon nguoi nhan.
    private JComboBox<Recipient> recipients;
    // Hop chon card mang hoac tu dong.
    private JComboBox<NetworkInterfaceInfo> interfaces;
    // Du lieu ten thanh vien cho JList.
    private final DefaultListModel<String> memberModel = new DefaultListModel<>();
    // Danh sach thanh vien tren giao dien.
    private JList<String> memberList;
    // Bang ket noi ben trai va vung lam viec chinh.
    private JPanel sidebar, workspace;
    // Cac nut an/hien, luu, xoa va chon nhan rieng.
    private ModernButton btnSidebar, btnSave, btnClear, btnPrivate;
    // Nhan cho biet dang gui chung hay rieng.
    private JLabel lblCompose;
    // Bo dem hoi thoai de xuat ra tep.
    private final StringBuilder chatLog = new StringBuilder();
    // Chan ket noi lap khi dang cho ket qua.
    private boolean connecting;
    // Cac nut tham gia, roi nhom va gui tin.
    private ModernButton btnConnect, btnDisconnect, btnSend;
    // Nhan trang thai, dia chi nhom va so nguoi online.
    private JLabel lblStatus, lblRoom, lblMembers;
    // Khung chua cac hang tin nhan.
    private Transcript transcript;
    // Vung cuon doc cua khung chat.
    private JScrollPane scrollPane;
    // Loi chao khi chua co noi dung.
    private JComponent emptyState;
    // So thanh vien dang online.
    private int onlineCount;

    // Tao giao dien va dang ky nhan su kien tu lop mang.
    public MultichatUI() {
        // Tao bo xu ly mang cho cua so nay.
        chatService = new MulticastChatService();
        // Dang ky cua so nhan tin va trang thai tu service.
        chatService.addListener(this);
        // Dung cac thanh phan giao dien.
        initUI();
        // Gan xu ly dong cua so de don socket.
        setupWindowListeners();
    }

    // Tao nhan voi font va mau chu thong nhat.
    private static JLabel label(String text, int size, int style, Color color) {
        JLabel label = new JLabel(text);
        // Hien chu nguyen van, khong dien giai HTML.
        label.putClientProperty("html.disable", Boolean.TRUE);
        // Dat kieu chu va co chu.
        label.setFont(new Font("Segoe UI", style, size));
        // Dat mau chu hoac mau noi dung.
        label.setForeground(color);
        return label;
    }

    // Tao cua so va sap xep cac vung chuc nang.
    private void initUI() {
        setTitle("Multichat • Trò chuyện nhóm");
        // Tu xu ly dong de gui LEAVE truoc khi thoat.
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(760, 620));
        setSize(1120, 800);
        // Dat cua so o giua man hinh.
        setLocationRelativeTo(null);
        JPanel root = new JPanel(new BorderLayout(0, 18));
        // Dat mau nen cho thanh phan.
        root.setBackground(BACKGROUND);
        // Dat vien hoac khoang dem cho thanh phan.
        root.setBorder(new EmptyBorder(20, 22, 18, 22));
        root.add(createTopPanel(), BorderLayout.NORTH);
        workspace = new JPanel(new BorderLayout(18, 0));
        // De nen trong suot cho khung cha hoac nen tu ve.
        workspace.setOpaque(false);
        sidebar = createSidebar();
        workspace.add(sidebar, BorderLayout.WEST);
        JPanel conversation = new JPanel(new BorderLayout(0, 12));
        // De nen trong suot cho khung cha hoac nen tu ve.
        conversation.setOpaque(false);
        conversation.add(createCenterPanel(), BorderLayout.CENTER);
        conversation.add(createBottomContainer(), BorderLayout.SOUTH);
        workspace.add(conversation, BorderLayout.CENTER);
        root.add(workspace, BorderLayout.CENTER);
        setContentPane(root);
        updateUIState(false);
        addComponentListener(new ComponentAdapter() {
            // Tinh lai bo cuc khi kich thuoc vung hien thi doi.
            @Override public void componentResized(ComponentEvent e) {
                // An bang ben neu cua so hep hon 1000 diem anh.
                setSidebarVisible(getWidth() >= 1000);
            }
        });
    }

    // Tao logo, nut an/hien bang ket noi va huong dan.
    private JPanel createTopPanel() {
        JPanel heading = new JPanel(new BorderLayout(16, 0));
        // De nen trong suot cho khung cha hoac nen tu ve.
        heading.setOpaque(false);
        JPanel brand = new JPanel(new BorderLayout(12, 0));
        // De nen trong suot cho khung cha hoac nen tu ve.
        brand.setOpaque(false);
        JLabel logo = label("M", 24, Font.BOLD, Color.WHITE);
        logo.setHorizontalAlignment(SwingConstants.CENTER);
        JPanel tile = new RoundedPanel(BLUE);
        // Chon cach sap xep cac thanh phan con.
        tile.setLayout(new BorderLayout());
        // De xuat kich thuoc cho bo sap xep giao dien.
        tile.setPreferredSize(new Dimension(48, 48));
        tile.add(logo);
        brand.add(tile, BorderLayout.WEST);
        JPanel words = new JPanel(new GridLayout(2, 1));
        // De nen trong suot cho khung cha hoac nen tu ve.
        words.setOpaque(false);
        words.add(label("Multichat", 24, Font.BOLD, INK));
        words.add(label("Một nhóm chung. Nhiều câu chuyện.", 12, Font.PLAIN, MUTED));
        brand.add(words, BorderLayout.CENTER);
        heading.add(brand, BorderLayout.WEST);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
        // De nen trong suot cho khung cha hoac nen tu ve.
        actions.setOpaque(false);
        btnSidebar = secondaryButton("Ẩn bảng kết nối");
        // Bam nut thi goi setSidebarVisible().
        btnSidebar.addActionListener(e -> setSidebarVisible(!sidebar.isVisible()));
        ModernButton help = secondaryButton("Hướng dẫn");
        // Bam nut thi goi showHelp().
        help.addActionListener(e -> showHelp());
        actions.add(btnSidebar);
        actions.add(help);
        heading.add(actions, BorderLayout.EAST);
        return heading;
    }

    // Tao nut phu voi mau nen nhe.
    private static ModernButton secondaryButton(String text) {
        return new ModernButton(text, new Color(227, 234, 245), new Color(51, 70, 103));
    }

    // An/hien bang ket noi va cap nhat bo cuc.
    private void setSidebarVisible(boolean visible) {
        sidebar.setVisible(visible);
        btnSidebar.setText(visible ? "Ẩn bảng kết nối" : "Kết nối & thành viên");
        // Tinh lai kich thuoc va bo cuc.
        workspace.revalidate();
    }

    // Tao o ket noi, chon card mang va danh sach thanh vien.
    private JPanel createSidebar() {
        JPanel panel = new RoundedPanel(Color.WHITE);
        // De xuat kich thuoc cho bo sap xep giao dien.
        panel.setPreferredSize(new Dimension(250, 0));
        // Chon cach sap xep cac thanh phan con.
        panel.setLayout(new BorderLayout(0, 18));
        // Dat vien hoac khoang dem cho thanh phan.
        panel.setBorder(new EmptyBorder(18, 16, 16, 16));
        JPanel settings = new JPanel(new GridBagLayout());
        // De nen trong suot cho khung cha hoac nen tu ve.
        settings.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 12, 0);
        int row = 0;
        c.gridy = row++;
        settings.add(label("KẾT NỐI NHÓM", 12, Font.BOLD, MUTED), c);
        txtUsername = new JTextField("User_" + (int) (Math.random() * 900 + 100));
        txtGroupIp = new JTextField(MulticastChatService.DEFAULT_GROUP_IP);
        txtPort = new JTextField(String.valueOf(MulticastChatService.DEFAULT_PORT));
        JTextField[] inputs = {txtUsername, txtGroupIp, txtPort};
        String[] names = {"Tên hiển thị", "Địa chỉ nhóm", "Cổng"};
        for (int i = 0; i < inputs.length; i++) {
            styleInput(inputs[i]);
            // Dat ten cho cong cu ho tro doc man hinh.
            inputs[i].getAccessibleContext().setAccessibleName(names[i]);
            JPanel field = new JPanel(new BorderLayout(0, 5));
            // De nen trong suot cho khung cha hoac nen tu ve.
            field.setOpaque(false);
            field.add(label(names[i], 12, Font.BOLD, MUTED), BorderLayout.NORTH);
            field.add(inputs[i], BorderLayout.CENTER);
            c.gridy = row++;
            settings.add(field, c);
        }
        interfaces = new JComboBox<>(NetworkInterfaceInfo.getAvailableInterfaces().toArray(new NetworkInterfaceInfo[0]));
        styleCombo(interfaces);
        // De xuat kich thuoc cho bo sap xep giao dien.
        interfaces.setPreferredSize(new Dimension(180, 34));
        // Hien goi y khi re chuot len thanh phan.
        interfaces.setToolTipText("Tự động chọn hoặc chỉ định card mạng LAN / Wi-Fi");
        // Dat ten cho cong cu ho tro doc man hinh.
        interfaces.getAccessibleContext().setAccessibleName("Card mạng");
        c.gridy = row++;
        settings.add(interfaces, c);
        btnConnect = new ModernButton("Tham gia nhóm", BLUE, Color.WHITE);
        // Bam nut thi goi actionConnect().
        btnConnect.addActionListener(e -> actionConnect());
        c.gridy = row++;
        settings.add(btnConnect, c);
        btnDisconnect = secondaryButton("Rời nhóm");
        // Bam nut thi goi actionDisconnect().
        btnDisconnect.addActionListener(e -> actionDisconnect());
        c.gridy = row++;
        c.insets = new Insets(0, 0, 0, 0);
        settings.add(btnDisconnect, c);
        panel.add(settings, BorderLayout.NORTH);

        JPanel members = new JPanel(new BorderLayout(0, 8));
        // De nen trong suot cho khung cha hoac nen tu ve.
        members.setOpaque(false);
        members.add(label("THÀNH VIÊN ONLINE", 12, Font.BOLD, MUTED), BorderLayout.NORTH);
        memberList = new JList<>(memberModel);
        memberList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        memberList.setFixedCellHeight(42);
        memberList.setCellRenderer(new DefaultListCellRenderer() {
            // Dinh dang tung dong trong danh sach.
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focus) {
                JLabel cell = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focus);
                // Hien chu nguyen van, khong dien giai HTML.
                cell.putClientProperty("html.disable", Boolean.TRUE);
                cell.setText("●  " + value + (value.equals(chatService.getUsername()) ? " (bạn)" : ""));
                // Dat kieu chu va co chu.
                cell.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                // Dat vien hoac khoang dem cho thanh phan.
                cell.setBorder(new EmptyBorder(6, 8, 6, 8));
                // Dat mau nen cho thanh phan.
                cell.setBackground(selected ? new Color(230, 239, 255) : Color.WHITE);
                // Dat mau chu hoac mau noi dung.
                cell.setForeground(selected ? BLUE : INK);
                return cell;
            }
        });
        // Cap nhat nut nhan rieng khi doi ten dang chon.
        memberList.addListSelectionListener(e -> updatePrivateButton());
        // Theo doi thao tac chuot tren danh sach.
        memberList.addMouseListener(new MouseAdapter() {
            // Xu ly nhap dup de chon nguoi nhan rieng.
            @Override public void mouseClicked(MouseEvent e) { if (e.getClickCount() == 2) selectPrivateRecipient(); }
        });
        JScrollPane memberScroll = new JScrollPane(memberList);
        // Dat vien hoac khoang dem cho thanh phan.
        memberScroll.setBorder(BorderFactory.createEmptyBorder());
        members.add(memberScroll, BorderLayout.CENTER);
        btnPrivate = secondaryButton("Nhắn riêng");
        // Hien goi y khi re chuot len thanh phan.
        btnPrivate.setToolTipText("Chọn thành viên rồi nhấn để gửi tin riêng");
        // Bam nut thi goi selectPrivateRecipient().
        btnPrivate.addActionListener(e -> selectPrivateRecipient());
        members.add(btnPrivate, BorderLayout.SOUTH);
        panel.add(members, BorderLayout.CENTER);
        return panel;
    }

    // Dinh dang hop chon va tu ve mui ten.
    private static void styleCombo(JComboBox<?> combo) {
        // Dat kieu chu va co chu.
        combo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        // Dat mau chu hoac mau noi dung.
        combo.setForeground(INK);
        // Dat mau nen cho thanh phan.
        combo.setBackground(new Color(244, 247, 251));
        // Dat vien hoac khoang dem cho thanh phan.
        combo.setBorder(BorderFactory.createLineBorder(new Color(218, 226, 238)));
        combo.setUI(new javax.swing.plaf.basic.BasicComboBoxUI() {
            // Tao nut mo danh sach cua hop chon.
            @Override protected JButton createArrowButton() {
                JButton arrow = new JButton() {
                    // Tu ve thanh phan thay cho cach ve mac dinh.
                    @Override protected void paintComponent(Graphics graphics) {
                        super.paintComponent(graphics);
                        Graphics2D g = (Graphics2D) graphics.create();
                        // Lam min duong ve hoac chu.
                        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        int x = getWidth() / 2, y = getHeight() / 2;
                        g.setColor(MUTED);
                        g.fillPolygon(new int[]{x - 4, x + 4, x}, new int[]{y - 2, y - 2, y + 3}, 3);
                        // Giai phong ban sao doi tuong ve.
                        g.dispose();
                    }
                };
                // Dat kieu chu va co chu.
                arrow.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                // Dat mau chu hoac mau noi dung.
                arrow.setForeground(MUTED);
                // Dat mau nen cho thanh phan.
                arrow.setBackground(new Color(235, 241, 250));
                // Dat vien hoac khoang dem cho thanh phan.
                arrow.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
                arrow.setFocusPainted(false);
                return arrow;
            }
        });
        combo.setRenderer(new DefaultListCellRenderer() {
            // Dinh dang tung dong trong danh sach.
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focus) {
                JLabel cell = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focus);
                // Hien chu nguyen van, khong dien giai HTML.
                cell.putClientProperty("html.disable", Boolean.TRUE);
                // Dat vien hoac khoang dem cho thanh phan.
                cell.setBorder(new EmptyBorder(5, 7, 5, 7));
                // Dat mau chu hoac mau noi dung.
                cell.setForeground(INK);
                // Dat mau nen cho thanh phan.
                cell.setBackground(selected ? new Color(225, 235, 254) : new Color(244, 247, 251));
                return cell;
            }
        });
    }

    // Dinh dang o nhap ten, dia chi nhom va cong.
    private static void styleInput(JTextField input) {
        // Dat kieu chu va co chu.
        input.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        // Dat mau chu hoac mau noi dung.
        input.setForeground(INK);
        input.setDisabledTextColor(MUTED);
        // Dat mau nen cho thanh phan.
        input.setBackground(Color.WHITE);
        input.setCaretColor(BLUE);
        // Dat vien hoac khoang dem cho thanh phan.
        input.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 219, 231)),
                new EmptyBorder(9, 10, 9, 10)));
        // Gioi han kich thuoc nho nhat.
        input.setMinimumSize(new Dimension(40, 38));
    }

    // Tao thanh cong cu va khung hien thi chat.
    private JPanel createCenterPanel() {
        JPanel center = new RoundedPanel(Color.WHITE);
        // Chon cach sap xep cac thanh phan con.
        center.setLayout(new BorderLayout());
        JPanel header = new JPanel(new BorderLayout(12, 0));
        // De nen trong suot cho khung cha hoac nen tu ve.
        header.setOpaque(false);
        // Dat vien hoac khoang dem cho thanh phan.
        header.setBorder(new EmptyBorder(14, 18, 14, 18));
        lblRoom = label("Cuộc trò chuyện nhóm", 16, Font.BOLD, INK);
        lblMembers = label("Chưa tham gia", 12, Font.PLAIN, MUTED);
        header.add(lblRoom, BorderLayout.WEST);
        header.add(lblMembers, BorderLayout.EAST);
        JPanel top = new JPanel(new BorderLayout());
        // De nen trong suot cho khung cha hoac nen tu ve.
        top.setOpaque(false);
        top.add(header, BorderLayout.NORTH);
        JPanel tools = new JPanel(new GridLayout(1, 3, 8, 0));
        // De nen trong suot cho khung cha hoac nen tu ve.
        tools.setOpaque(false);
        // Dat vien hoac khoang dem cho thanh phan.
        tools.setBorder(new EmptyBorder(0, 18, 12, 18));
        btnSave = secondaryButton("Lưu hội thoại");
        // Bam nut thi goi saveConversation().
        btnSave.addActionListener(e -> saveConversation());
        btnClear = secondaryButton("Xóa trên máy này");
        // Bat hoac khoa thao tac theo dieu kien.
        btnSave.setEnabled(false);
        // Bat hoac khoa thao tac theo dieu kien.
        btnClear.setEnabled(false);
        // Dang ky viec can lam khi bam nut.
        btnClear.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(this,
                    "Xóa nội dung đang hiển thị trên máy này?\nTin nhắn trên máy khác không bị ảnh hưởng.",
                    "Xóa hội thoại", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) clearConversation();
        });
        ModernButton latest = secondaryButton("Tin mới nhất ↓");
        // Bam nut thi goi scrollToLatest().
        latest.addActionListener(e -> scrollToLatest());
        tools.add(btnSave);
        tools.add(btnClear);
        tools.add(latest);
        top.add(tools, BorderLayout.CENTER);
        center.add(top, BorderLayout.NORTH);
        transcript = new Transcript();
        scrollPane = new JScrollPane(transcript);
        // Dat vien hoac khoang dem cho thanh phan.
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        // Dat mau nen cho thanh phan.
        scrollPane.getViewport().setBackground(BACKGROUND);
        scrollPane.getVerticalScrollBar().setUnitIncrement(24);
        // De xuat kich thuoc cho bo sap xep giao dien.
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(10, 0));
        scrollPane.getVerticalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            // Dat mau thanh cuon va ranh cuon.
            @Override protected void configureScrollBarColors() {
                thumbColor = new Color(182, 195, 213);
                trackColor = BACKGROUND;
            }
            // Bo nut mui ten o dau thanh cuon.
            @Override protected JButton createDecreaseButton(int orientation) { return hiddenButton(); }
            // Bo nut mui ten o cuoi thanh cuon.
            @Override protected JButton createIncreaseButton(int orientation) { return hiddenButton(); }
            // Tao nut rong de an mui ten cuon mac dinh.
            private JButton hiddenButton() {
                JButton button = new JButton();
                // De xuat kich thuoc cho bo sap xep giao dien.
                button.setPreferredSize(new Dimension(0, 0));
                return button;
            }
            // Ve phan keo cua thanh cuon voi goc bo tron.
            @Override protected void paintThumb(Graphics graphics, JComponent component, Rectangle bounds) {
                Graphics2D g = (Graphics2D) graphics.create();
                // Lam min duong ve hoac chu.
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(thumbColor);
                // Ve nen hinh chu nhat bo goc.
                g.fillRoundRect(bounds.x + 2, bounds.y, Math.max(4, bounds.width - 4), bounds.height, 8, 8);
                // Giai phong ban sao doi tuong ve.
                g.dispose();
            }
        });
        scrollPane.addComponentListener(new ComponentAdapter() {
            // Tinh lai bo cuc khi kich thuoc vung hien thi doi.
            @Override public void componentResized(ComponentEvent event) {
                // Tinh lai kich thuoc va bo cuc.
                transcript.revalidate();
            }
        });
        center.add(scrollPane, BorderLayout.CENTER);
        // Hien loi chao cho khung chat rong.
        showEmptyState();
        return center;
    }

    // Hien loi chao khi khung chat chua co noi dung.
    private void showEmptyState() {
        JPanel empty = new JPanel(new GridLayout(0, 1, 0, 8));
        // De nen trong suot cho khung cha hoac nen tu ve.
        empty.setOpaque(false);
        // Dat vien hoac khoang dem cho thanh phan.
        empty.setBorder(new EmptyBorder(36, 18, 36, 18));
        JLabel icon = label("Xin chào!", 28, Font.BOLD, BLUE);
        JLabel title = label("Câu chuyện bắt đầu từ một lời chào", 16, Font.BOLD, INK);
        JLabel hint = label("Tham gia nhóm, rồi gửi tin nhắn đầu tiên của bạn.", 12, Font.PLAIN, MUTED);
        for (JLabel line : new JLabel[]{icon, title, hint}) {
            line.setHorizontalAlignment(SwingConstants.CENTER);
            empty.add(line);
        }
        empty.setMaximumSize(new Dimension(Integer.MAX_VALUE, 220));
        emptyState = empty;
        transcript.add(empty);
    }

    // Tao o chon nguoi nhan, soan tin va nut gui.
    private JPanel createBottomContainer() {
        JPanel bottom = new JPanel(new BorderLayout(0, 8));
        // De nen trong suot cho khung cha hoac nen tu ve.
        bottom.setOpaque(false);
        JPanel card = new RoundedPanel(Color.WHITE);
        // Chon cach sap xep cac thanh phan con.
        card.setLayout(new BorderLayout(0, 10));
        // Dat vien hoac khoang dem cho thanh phan.
        card.setBorder(new EmptyBorder(12, 14, 12, 14));
        JPanel recipientRow = new JPanel(new BorderLayout(8, 0));
        // De nen trong suot cho khung cha hoac nen tu ve.
        recipientRow.setOpaque(false);
        recipients = new JComboBox<>();
        // Them lua chon gui cho ca nhom.
        recipients.addItem(new Recipient(null));
        styleCombo(recipients);
        // De xuat kich thuoc cho bo sap xep giao dien.
        recipients.setPreferredSize(new Dimension(175, 28));
        // Dat ten cho cong cu ho tro doc man hinh.
        recipients.getAccessibleContext().setAccessibleName("Người nhận tin nhắn");
        // Bam nut thi goi updateComposeLabel().
        recipients.addActionListener(e -> updateComposeLabel());
        recipientRow.add(recipients, BorderLayout.WEST);
        lblCompose = label("Gửi lời chào tới cả nhóm", 12, Font.PLAIN, MUTED);
        recipientRow.add(lblCompose, BorderLayout.CENTER);
        card.add(recipientRow, BorderLayout.NORTH);
        JPanel composer = new JPanel(new BorderLayout(10, 0));
        // De nen trong suot cho khung cha hoac nen tu ve.
        composer.setOpaque(false);
        txtInput = new JTextArea(2, 10);
        // Dat kieu chu va co chu.
        txtInput.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        // Dat mau chu hoac mau noi dung.
        txtInput.setForeground(INK);
        txtInput.setDisabledTextColor(MUTED);
        txtInput.setLineWrap(true);
        txtInput.setWrapStyleWord(true);
        // Dat vien hoac khoang dem cho thanh phan.
        txtInput.setBorder(new EmptyBorder(8, 10, 8, 10));
        // Dat ten cho cong cu ho tro doc man hinh.
        txtInput.getAccessibleContext().setAccessibleName("Nội dung tin nhắn");
        // Hien goi y khi re chuot len thanh phan.
        txtInput.setToolTipText("Enter để gửi · Shift + Enter để xuống dòng");
        // Gan phim tat: Enter gui, Shift + Enter xuong dong.
        txtInput.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "send");
        // Gan hanh dong phim tat voi xu ly gui tin.
        txtInput.getActionMap().put("send", new AbstractAction() {
            // Xu ly khi nguoi dung kich hoat hanh dong.
            @Override public void actionPerformed(ActionEvent e) { actionSendMessage(); }
        });
        // Gan phim tat: Enter gui, Shift + Enter xuong dong.
        txtInput.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK), "insert-break");
        // Theo doi noi dung de bat/tat nut gui.
        txtInput.getDocument().addDocumentListener(new DocumentListener() {
            // Cap nhat nut gui khi them ky tu.
            public void insertUpdate(DocumentEvent e) { updateSendButton(); }
            // Cap nhat nut gui khi xoa ky tu.
            public void removeUpdate(DocumentEvent e) { updateSendButton(); }
            // Cap nhat nut gui khi tai lieu doi thuoc tinh.
            public void changedUpdate(DocumentEvent e) { updateSendButton(); }
        });
        JScrollPane inputScroll = new JScrollPane(txtInput);
        // Dat vien hoac khoang dem cho thanh phan.
        inputScroll.setBorder(BorderFactory.createLineBorder(new Color(218, 226, 238)));
        inputScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        btnSend = new ModernButton("Gửi tin →", BLUE, Color.WHITE);
        // Bam nut thi goi actionSendMessage().
        btnSend.addActionListener(e -> actionSendMessage());
        composer.add(inputScroll, BorderLayout.CENTER);
        composer.add(btnSend, BorderLayout.EAST);
        card.add(composer, BorderLayout.CENTER);
        bottom.add(card, BorderLayout.CENTER);
        lblStatus = label("", 12, Font.PLAIN, MUTED);
        JPanel footer = new JPanel(new BorderLayout());
        // De nen trong suot cho khung cha hoac nen tu ve.
        footer.setOpaque(false);
        footer.add(lblStatus, BorderLayout.WEST);
        footer.add(label("Enter gửi · Shift + Enter xuống dòng", 11, Font.PLAIN, MUTED), BorderLayout.EAST);
        bottom.add(footer, BorderLayout.SOUTH);
        return bottom;
    }

    // Dang ky don ket noi khi dong cua so.
    private void setupWindowListeners() {
        addWindowListener(new WindowAdapter() {
            // Roi nhom, dong cua so va ket thuc ung dung.
            @Override
            public void windowClosing(WindowEvent e) {
                if (chatService.isConnected()) {
                    // Bao roi nhom va don tai nguyen mang.
                    chatService.disconnect();
                }
                // Giai phong tai nguyen cua cua so.
                dispose();
                // Ket thuc tien trinh Java cua ung dung.
                System.exit(0);
            }
        });
    }

    // Kiem tra du lieu roi ket noi mang o luong nen.
    private void actionConnect() {
        // Bo qua neu dang ket noi hoac da tham gia nhom.
        if (connecting || chatService.isConnected()) return;
        String username = txtUsername.getText().trim();
        String groupIp = txtGroupIp.getText().trim();
        String portStr = txtPort.getText().trim();

        if (username.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập tên người dùng!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            txtUsername.requestFocus();
            return;
        }

        int port;
        try {
            // Doi cong tu chuoi sang so; chuoi sai se gay loi.
            port = Integer.parseInt(portStr);
            // GUI chi chap nhan cong trong khoang nay.
            if (port < 1024 || port > 65535) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Port phải là số nguyên từ 1024 đến 65535!", "Lỗi nhập liệu", JOptionPane.ERROR_MESSAGE);
            txtPort.requestFocus();
            return;
        }

        // Lay card dang chon; tuy chon tu dong chua card null.
        NetworkInterfaceInfo net = (NetworkInterfaceInfo) interfaces.getSelectedItem();
        // Khoa nut de tranh bam ket noi nhieu lan.
        connecting = true;
        updateUIState(false);
        // Bao tac vu ket noi dang chay.
        btnConnect.setText("Đang kết nối…");
        lblStatus.setText("● Đang tham gia nhóm…");
        // Chay ket noi o nen de cua so khong bi dung.
        new SwingWorker<Void, Void>() {
            // Chay ket noi o nen de giao dien van phan hoi.
            @Override protected Void doInBackground() throws Exception {
                // Goi service tham gia nhom voi card da chon.
                chatService.connect(username, groupIp, port, net == null ? null : net.getNetworkInterface());
                return null;
            }
            // Nhan ket qua ket noi va cap nhat GUI tren luong Swing.
            @Override protected void done() {
                // Ket thuc trang thai dang cho ket noi.
                connecting = false;
                btnConnect.setText("Tham gia nhóm");
                try {
                    // Lay ket qua tac vu; nem loi neu ket noi that bai.
                    get();
                    updateUIState(true);
                    appendSystemMessage("Đã tham gia nhóm " + groupIp + ":" + port + " với tên " + username);
                    // Dua con tro lai o soan tin.
                    txtInput.requestFocusInWindow();
                } catch (Exception ex) {
                    updateUIState(chatService.isConnected());
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    JOptionPane.showMessageDialog(MultichatUI.this, "Không thể kết nối:\n" + cause.getMessage(), "Lỗi kết nối", JOptionPane.ERROR_MESSAGE);
                }
            }
        // Bat dau tac vu SwingWorker.
        }.execute();
    }

    // Roi nhom va khoa cac thanh phan gui tin.
    private void actionDisconnect() {
        if (chatService.isConnected()) {
            // Bao roi nhom va don tai nguyen mang.
            chatService.disconnect();
            updateUIState(false);
            appendSystemMessage("Bạn đã rời khỏi nhóm chat.");
        }
    }

    // Lay noi dung va gui chung hoac rieng theo nguoi nhan.
    private void actionSendMessage() {
        if (!chatService.isConnected()) {
            JOptionPane.showMessageDialog(this, "Bạn cần tham gia nhóm trước khi gửi tin nhắn!", "Chưa kết nối", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Lay noi dung va bo khoang trang hai dau.
        String content = txtInput.getText().trim();
        // Bo qua tin rong.
        if (content.isEmpty()) {
            return;
        }

        try {
            // Lay nguoi nhan dang chon.
            Recipient recipient = (Recipient) recipients.getSelectedItem();
            // Co ten nguoi nhan thi gui rieng.
            if (recipient != null && recipient.username != null) {
                // Khong gui rieng neu ten nay khong con online.
                if (!memberModel.contains(recipient.username)) {
                    appendSystemMessage("Người nhận không còn online. Vui lòng chọn lại.");
                    return;
                }
                // Gui WHISPER kem ten nguoi nhan rieng.
                chatService.sendWhisper(recipient.username, content);
            } else {
                // Gui CHAT cho ca nhom multicast.
                chatService.sendMessage(content);
            }
            // Xoa o soan sau khi gui.
            txtInput.setText("");
            // Dua con tro lai o soan tin.
            txtInput.requestFocusInWindow();
            // Cuon toi cuoi cuoc tro chuyen.
            scrollToLatest();
        } catch (IOException ex) {
            appendSystemMessage("Lỗi gửi tin: " + ex.getMessage());
        }
    }

    // Bat/tat cac o va nut theo trang thai ket noi.
    private void updateUIState(boolean connected) {
        // Bat hoac khoa thao tac theo dieu kien.
        txtUsername.setEnabled(!connected && !connecting);
        // Bat hoac khoa thao tac theo dieu kien.
        txtGroupIp.setEnabled(!connected && !connecting);
        // Bat hoac khoa thao tac theo dieu kien.
        txtPort.setEnabled(!connected && !connecting);
        // Bat hoac khoa thao tac theo dieu kien.
        btnConnect.setEnabled(!connected && !connecting);
        // Bat hoac khoa thao tac theo dieu kien.
        btnDisconnect.setEnabled(connected && !connecting);
        // Bat hoac khoa thao tac theo dieu kien.
        txtInput.setEnabled(connected);
        // Bat hoac khoa thao tac theo dieu kien.
        interfaces.setEnabled(!connected && !connecting);
        // Bat hoac khoa thao tac theo dieu kien.
        recipients.setEnabled(connected);
        updateSendButton();
        // Don danh sach cu khi ngat ket noi.
        if (!connected) {
            onlineCount = 0;
            // Xoa cac ten cu truoc khi cap nhat.
            memberModel.clear();
            // Xoa cac lua chon nguoi nhan cu.
            recipients.removeAllItems();
            // Them lua chon gui cho ca nhom.
            recipients.addItem(new Recipient(null));
        }
        updatePrivateButton();
        lblRoom.setText(connected ? "Nhóm " + chatService.getGroupAddress() + ":" + chatService.getPort() : "Cuộc trò chuyện nhóm");
        lblStatus.setText(connected ? "● Đã kết nối · " + chatService.getUsername() : "● Chưa kết nối");
        // Dat mau chu hoac mau noi dung.
        lblStatus.setForeground(connected ? new Color(21, 128, 61) : MUTED);
        lblMembers.setText(connected ? onlineCount + " thành viên online" : "Chưa tham gia");
    }

    // Them hang chat; chi tu cuon neu dang gan cuoi.
    private void addRow(JComponent row) {
        // Lay thanh cuon doc cua khung chat.
        JScrollBar bar = scrollPane.getVerticalScrollBar();
        // Chi tu cuon neu dang doc gan cuoi khung chat.
        boolean atBottom = bar.getValue() + bar.getVisibleAmount() >= bar.getMaximum() - 48;
        // Go loi chao khi bat dau co noi dung.
        if (emptyState != null) {
            transcript.remove(emptyState);
            // Danh dau loi chao da duoc go.
            emptyState = null;
        }
        // Them hang tin vao cuoi khung chat.
        transcript.add(row);
        // Bat hoac khoa thao tac theo dieu kien.
        btnSave.setEnabled(true);
        // Bat hoac khoa thao tac theo dieu kien.
        btnClear.setEnabled(true);
        // Tinh lai kich thuoc va bo cuc.
        transcript.revalidate();
        // Ve lai phan giao dien vua thay doi.
        transcript.repaint();
        // Doi bo cuc xong roi tu cuon toi tin moi.
        if (atBottom) SwingUtilities.invokeLater(() -> bar.setValue(bar.getMaximum()));
    }

    // Luu va hien thong bao he thong tren luong Swing.
    private void appendSystemMessage(String text) {
        // Dua cong viec cap nhat GUI ve luong Swing.
        SwingUtilities.invokeLater(() -> {
            // Ghi thong bao cuc bo vao bo dem de luu tep.
            chatLog.append("[Hệ thống] ").append(text).append('\n');
            addRow(new SystemRow(text));
        });
    }

    // Nhan tin tu service de hien len GUI hoac CLI.
    @Override public void onMessageReceived(ChatMessage message, boolean isSelf) {
        // Dua cong viec cap nhat GUI ve luong Swing.
        SwingUtilities.invokeLater(() -> {
            // Khong ghi goi bao song vao lich su chat.
            if (message.getType() != MessageType.HEARTBEAT) {
                // Luu gio va ten gui vao lich su tren may nay.
                chatLog.append('[').append(message.getFormattedTime()).append("] ")
                       .append(message.getSenderName());
                // Danh dau tin rieng trong tep lich su.
                if (message.getType() == MessageType.WHISPER) chatLog.append(" [Riêng tới ").append(message.getTargetUser()).append(']');
                // Luu noi dung va ket thuc mot dong lich su.
                chatLog.append(": ").append(message.getContent()).append('\n');
            }
            switch (message.getType()) {
                // Thong bao co nguoi vua tham gia.
                case JOIN:
                    addRow(new SystemRow(message.getSenderName() + " đã tham gia nhóm · " + message.getFormattedTime()));
                    break;
                // Thong bao co nguoi vua roi nhom.
                case LEAVE:
                    addRow(new SystemRow(message.getSenderName() + " đã rời nhóm · " + message.getFormattedTime()));
                    break;
                // Goi bao ben gui van online.
                case HEARTBEAT:
                    break;
                default:
                    // Tao bong bong; isSelf quyet dinh trai hay phai.
                    addRow(new MessageRow(message, isSelf));
            }
        });
    }

    // Nhan danh sach thanh vien online tu service.
    @Override public void onUserListUpdated(Set<String> onlineUsers) {
        // Dua cong viec cap nhat GUI ve luong Swing.
        SwingUtilities.invokeLater(() -> {
            if (chatService.isConnected()) {
                // Dong bo thanh vien va o chon nguoi nhan.
                refreshMembers(onlineUsers);
                // Dem thanh vien de hien len tieu de.
                onlineCount = onlineUsers.size();
                lblMembers.setText(onlineCount + " thành viên online");
                // Hien goi y khi re chuot len thanh phan.
                lblMembers.setToolTipText(String.join(", ", onlineUsers));
            }
        });
    }

    // Nhan thay doi trang thai ket noi tu service.
    @Override public void onStatusChanged(String status, boolean connected) {
        SwingUtilities.invokeLater(() -> updateUIState(connected));
    }

    // Nhan loi tu service va thong bao cho nguoi dung.
    @Override public void onError(String error) {
        appendSystemMessage("Lỗi: " + error);
    }

    // Cap nhat thanh vien va giu nguoi nhan neu con online.
    private void refreshMembers(Set<String> users) {
        String selectedMember = memberList.getSelectedValue();
        Recipient selectedRecipient = (Recipient) recipients.getSelectedItem();
        // Giu nguoi nhan truoc khi tao lai danh sach.
        String target = selectedRecipient == null ? null : selectedRecipient.username;
        // Xoa cac ten cu truoc khi cap nhat.
        memberModel.clear();
        // Xoa cac lua chon nguoi nhan cu.
        recipients.removeAllItems();
        // Them lua chon gui cho ca nhom.
        recipients.addItem(new Recipient(null));
        // Sap xep ten de danh sach de tim.
        for (String user : new java.util.TreeSet<>(users)) {
            // Them ten vao danh sach hien thi.
            memberModel.addElement(user);
            // Khong them chinh minh vao lua chon nhan rieng.
            if (!user.equals(chatService.getUsername())) {
                Recipient recipient = new Recipient(user);
                recipients.addItem(recipient);
                // Khoi phuc nguoi nhan neu van online.
                if (user.equals(target)) recipients.setSelectedItem(recipient);
            }
        }
        if (selectedMember != null && users.contains(selectedMember)) memberList.setSelectedValue(selectedMember, true);
        // Nguoi nhan offline thi chuyen ve ca nhom.
        if (target != null && !users.contains(target)) {
            appendSystemMessage(target + " đã offline. Đã chuyển người nhận về cả nhóm.");
        }
        updatePrivateButton();
    }

    // Chi bat nhan rieng khi chon mot thanh vien khac.
    private void updatePrivateButton() {
        if (btnPrivate != null) {
            // Lay ten dang chon trong danh sach thanh vien.
            String user = memberList.getSelectedValue();
            // Bat hoac khoa thao tac theo dieu kien.
            btnPrivate.setEnabled(chatService.isConnected() && user != null && !user.equals(chatService.getUsername()));
        }
    }

    // Chon nguoi nhan theo ten dang chon trong danh sach.
    private void selectPrivateRecipient() {
        // Lay ten dang chon trong danh sach thanh vien.
        String user = memberList.getSelectedValue();
        if (!chatService.isConnected() || user == null || user.equals(chatService.getUsername())) return;
        for (int i = 0; i < recipients.getItemCount(); i++) {
            if (user.equals(recipients.getItemAt(i).username)) {
                // Chuyen o nguoi nhan sang thanh vien da chon.
                recipients.setSelectedIndex(i);
                // Dua con tro lai o soan tin.
                txtInput.requestFocusInWindow();
                break;
            }
        }
    }

    // Hien che do gui chung hoac rieng.
    private void updateComposeLabel() {
        if (lblCompose == null) return;
        // Lay nguoi nhan dang chon.
        Recipient recipient = (Recipient) recipients.getSelectedItem();
        // Phan biet gui rieng voi gui ca nhom.
        boolean privateMessage = recipient != null && recipient.username != null;
        lblCompose.setText(privateMessage ? "Đang nhắn riêng" : "Gửi tới tất cả thành viên");
        // Dat mau chu hoac mau noi dung.
        lblCompose.setForeground(privateMessage ? BLUE : MUTED);
        // Hien goi y khi re chuot len thanh phan.
        recipients.setToolTipText(privateMessage
                ? "Tin riêng được lọc theo tên; nội dung vẫn truyền qua nhóm và chưa mã hóa."
                : "Mọi người trong nhóm đều thấy tin nhắn");
    }

    // Chi bat nut gui khi da ket noi va co noi dung.
    private void updateSendButton() {
        // Bat hoac khoa thao tac theo dieu kien.
        if (btnSend != null) btnSend.setEnabled(chatService.isConnected() && !txtInput.getText().trim().isEmpty());
    }

    // Dua thanh cuon xuong cuoi sau khi bo cuc cap nhat.
    private void scrollToLatest() {
        SwingUtilities.invokeLater(() -> scrollPane.getVerticalScrollBar().setValue(scrollPane.getVerticalScrollBar().getMaximum()));
    }

    // Xoa chat tren may nay, khong gui yeu cau len mang.
    private void clearConversation() {
        // Xoa cac hang chat tren may nay.
        transcript.removeAll();
        // Xoa noi dung trong bo dem lich su.
        chatLog.setLength(0);
        // Hien loi chao cho khung chat rong.
        showEmptyState();
        // Bat hoac khoa thao tac theo dieu kien.
        btnSave.setEnabled(false);
        // Bat hoac khoa thao tac theo dieu kien.
        btnClear.setEnabled(false);
        // Tinh lai kich thuoc va bo cuc.
        transcript.revalidate();
        // Ve lai phan giao dien vua thay doi.
        transcript.repaint();
    }

    // Chon noi luu va ghi hoi thoai ra tep UTF-8.
    private void saveConversation() {
        // Mo hop chon duong dan luu tep.
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Lưu cuộc trò chuyện");
        chooser.setSelectedFile(new java.io.File("Multichat-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".txt"));
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Văn bản UTF-8 (*.txt)", "txt"));
        // Dung neu nguoi dung huy chon tep.
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        java.io.File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase(java.util.Locale.ROOT).endsWith(".txt")) file = new java.io.File(file.getPath() + ".txt");
        if (file.exists() && JOptionPane.showConfirmDialog(this, "Tệp đã tồn tại. Bạn muốn ghi đè?", "Lưu hội thoại", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        try {
            // Ghi hoi thoai bang UTF-8 de giu dung tieng Viet.
            Files.writeString(file.toPath(), chatLog.toString(), StandardCharsets.UTF_8);
            JOptionPane.showMessageDialog(this, "Đã lưu hội thoại vào:\n" + file.getAbsolutePath(), "Lưu thành công", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Không thể lưu tệp:\n" + e.getMessage(), "Lỗi lưu hội thoại", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Mo hop thoai huong dan su dung.
    private void showHelp() {
        JOptionPane.showMessageDialog(this,
                "1. Nhập tên, địa chỉ nhóm và cổng rồi chọn Tham gia nhóm.\n"
              + "2. Các máy muốn chat cùng nhau phải dùng cùng nhóm và cổng.\n"
              + "3. Chọn người nhận: Cả nhóm hoặc một thành viên online.\n"
              + "4. Enter để gửi; Shift + Enter để xuống dòng.\n"
              + "5. Nhấp phải tin nhắn để sao chép nội dung.\n"
              + "6. Lưu hội thoại ra tệp .txt; Xóa chỉ xóa nội dung trên máy này.\n\n"
              + "Tin riêng hiện chỉ lọc theo tên, chưa có mã hóa nội dung.",
                "Sử dụng Multichat", JOptionPane.INFORMATION_MESSAGE);
    }

    private static class Recipient {
        // Ten nguoi nhan; null tuong ung Ca nhom.
        final String username;
        // Luu ten nguoi nhan; null nghia la gui ca nhom.
        Recipient(String username) { this.username = username; }
        // Hien Ca nhom hoac Rieng: ten trong hop chon.
        @Override public String toString() { return username == null ? "Cả nhóm" : "Riêng: " + username; }
    }

    private static class RoundedPanel extends JPanel {
        // Mau nen duoc dung khi tu ve khung.
        private final Color fill;
        // Tao khung nen bo goc voi mau duoc truyen vao.
        RoundedPanel(Color fill) { this.fill = fill; setOpaque(false); }
        // Tu ve thanh phan thay cho cach ve mac dinh.
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            // Lam min duong ve hoac chu.
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(fill);
            // Ve nen hinh chu nhat bo goc.
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
            // Giai phong ban sao doi tuong ve.
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    private static class Transcript extends JPanel implements Scrollable {
        // Xep cac hang tin nhan theo chieu doc.
        Transcript() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBackground(BACKGROUND);
            setBorder(new EmptyBorder(10, 0, 12, 0));
        }
        // De xuat kich thuoc ban dau cua vung chat.
        public Dimension getPreferredScrollableViewportSize() { return new Dimension(600, 300); }
        // So diem anh cuon cho moi buoc nho.
        public int getScrollableUnitIncrement(Rectangle r, int orientation, int direction) { return 24; }
        // Cuon gan mot trang cho moi buoc lon.
        public int getScrollableBlockIncrement(Rectangle r, int orientation, int direction) { return Math.max(24, r.height - 24); }
        // Cho khung chat bam theo chieu rong vung cuon.
        public boolean getScrollableTracksViewportWidth() { return true; }
        // Cho noi dung cao hon vung hien thi de cuon doc.
        public boolean getScrollableTracksViewportHeight() { return false; }
    }

    /** Chieu cao hang duoc tinh lai theo chieu rong viewport de noi dung luon xuong dong. */
    private static class MessageRow extends JPanel {
        // Khung nen bo goc cua mot tin nhan.
        private final RoundedPanel bubble;
        // Noi dung tin dang van ban thuan.
        private final JTextArea body;
        // Nhan ten nguoi gui va gio tao tin.
        private final JLabel author, time;
        // true neu day la tin do chinh phien nay gui.
        private final boolean self;
        // Tao bong bong tin; self cho biet tin cua minh.
        MessageRow(ChatMessage message, boolean self) {
            this.self = self;
            setOpaque(false);
            setLayout(null);
            // Tin cua minh nen xanh; tin nguoi khac nen trang.
            bubble = new RoundedPanel(self ? BLUE : Color.WHITE);
            // Chon cach sap xep cac thanh phan con.
            bubble.setLayout(new BorderLayout(0, 5));
            // Dat vien hoac khoang dem cho thanh phan.
            bubble.setBorder(new EmptyBorder(10, 14, 10, 14));
            String name = self ? "Bạn" : message.getSenderName();
            boolean isAdminMsg = message.getExtraData() != null && message.getExtraData().contains("\"role\":\"ADMIN\"");
            if (isAdminMsg) {
                name = "👑 " + name + " [Admin]";
            }
            if (message.getType() == MessageType.WHISPER) {
                name += self ? " · Riêng tới " + message.getTargetUser() : " · Tin nhắn riêng";
            }
            author = label(name, 12, Font.BOLD, self ? new Color(224, 234, 255) : (isAdminMsg ? new Color(217, 119, 6) : BLUE));
            // Noi dung la van ban thuan, khong dien giai HTML tu nguoi gui.
            body = new JTextArea(message.getContent());
            // Dat kieu chu va co chu.
            body.setFont(new Font("Segoe UI", Font.PLAIN, 15));
            // Dat mau chu hoac mau noi dung.
            body.setForeground(self ? Color.WHITE : INK);
            // De nen trong suot cho khung cha hoac nen tu ve.
            body.setOpaque(false);
            // Cho doc va sao chep, khong cho sua tin da nhan.
            body.setEditable(false);
            // Tu xuong dong neu noi dung dai hon bong bong.
            body.setLineWrap(true);
            // Uu tien ngat dong theo tu.
            body.setWrapStyleWord(true);
            // Dat vien hoac khoang dem cho thanh phan.
            body.setBorder(null);
            JPopupMenu menu = new JPopupMenu();
            JMenuItem copy = new JMenuItem("Sao chép tin nhắn");
            // Dang ky viec can lam khi bam nut.
            copy.addActionListener(e -> {
                try {
                    // Sao chep toan bo noi dung tin vao clipboard.
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(body.getText()), null);
                } catch (IllegalStateException ex) {
                    Toolkit.getDefaultToolkit().beep();
                }
            });
            menu.add(copy);
            // Gan menu sao chep cho thao tac nhap phai.
            body.setComponentPopupMenu(menu);
            time = label(message.getFormattedTime(), 11, Font.PLAIN, self ? new Color(224, 234, 255) : MUTED);
            time.setHorizontalAlignment(SwingConstants.RIGHT);
            bubble.add(author, BorderLayout.NORTH);
            bubble.add(body, BorderLayout.CENTER);
            bubble.add(time, BorderLayout.SOUTH);
            add(bubble);
        }
        // Tinh kich thuoc bong bong theo noi dung va chieu rong.
        private Dimension bubbleSize() {
            int available = getParent() == null ? 600 : getParent().getWidth();
            // Gioi han bong bong de con khoang trong hai ben.
            int maxWidth = Math.max(100, (int) ((available - 40) * 0.74));
            // Do chieu rong chu de tinh kich thuoc bong bong.
            FontMetrics fm = body.getFontMetrics(body.getFont());
            int natural = 0;
            for (String line : body.getText().split("\n", -1)) natural = Math.max(natural, fm.stringWidth(line));
            int width = Math.min(maxWidth, Math.max(140, Math.max(natural, author.getPreferredSize().width) + 30));
            // Dat chieu rong de tinh chieu cao sau xuong dong.
            body.setSize(Math.max(1, width - 28), Integer.MAX_VALUE);
            int height = body.getPreferredSize().height + author.getPreferredSize().height + time.getPreferredSize().height + 30;
            return new Dimension(width, height);
        }
        // Bao kich thuoc phu hop cho bo sap xep giao dien.
        @Override public Dimension getPreferredSize() {
            Dimension size = bubbleSize();
            return new Dimension(100, size.height + 12);
        }
        // Gioi han chieu cao hang de tranh bi keo gian.
        @Override public Dimension getMaximumSize() { return new Dimension(Integer.MAX_VALUE, getPreferredSize().height); }
        // Dat tin cua minh ben phai, tin nguoi khac ben trai.
        @Override public void doLayout() {
            Dimension size = bubbleSize();
            // Tin cua minh can phai; tin nguoi khac can trai.
            bubble.setBounds(self ? getWidth() - size.width - 18 : 18, 4, size.width, size.height);
        }
    }

    private static class SystemRow extends JPanel {
        private final JTextPane notice = new JTextPane();
        // Tao hang thong bao he thong can giua.
        SystemRow(String message) {
            setOpaque(false);
            setLayout(new BorderLayout());
            setBorder(new EmptyBorder(8, 32, 8, 32));
            notice.setText(message);
            // Dat kieu chu va co chu.
            notice.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            // Dat mau chu hoac mau noi dung.
            notice.setForeground(MUTED);
            // De nen trong suot cho khung cha hoac nen tu ve.
            notice.setOpaque(false);
            notice.setEditable(false);
            javax.swing.text.SimpleAttributeSet attrs = new javax.swing.text.SimpleAttributeSet();
            javax.swing.text.StyleConstants.setAlignment(attrs, javax.swing.text.StyleConstants.ALIGN_CENTER);
            // Ap dung can giua cho thong bao he thong.
            notice.setParagraphAttributes(attrs, false);
            add(notice, BorderLayout.CENTER);
        }
        // Bao kich thuoc phu hop cho bo sap xep giao dien.
        @Override public Dimension getPreferredSize() {
            int width = getParent() == null ? 600 : getParent().getWidth();
            notice.setSize(Math.max(60, width - 64), Integer.MAX_VALUE);
            return new Dimension(100, notice.getPreferredSize().height + 16);
        }
        // Gioi han chieu cao hang de tranh bi keo gian.
        @Override public Dimension getMaximumSize() { return new Dimension(Integer.MAX_VALUE, getPreferredSize().height); }
    }

    public static class ModernButton extends JButton {
        // Mau nen khi nut chua duoc tuong tac.
        private final Color normalBg;
        // Mau nen khi re chuot len nut.
        private final Color hoverBg;
        // Mau nen khi dang nhan nut.
        private final Color pressedBg;
        // Mau chu cua nut dang hoat dong.
        private final Color normalFg;

        // Tao nut co mau nen, hover va trang thai nhan.
        public ModernButton(String text, Color bg, Color fg) {
            super(text);
            this.normalBg = bg;
            this.hoverBg = brighten(bg, 0.15f);
            this.pressedBg = bg.darker();
            this.normalFg = fg;

            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setFont(new Font("Segoe UI", Font.BOLD, 12));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setMargin(new Insets(6, 14, 6, 14));
        }

        // Pha them trang de tao mau khi re chuot.
        private static Color brighten(Color c, float fraction) {
            int r = Math.min(255, (int) (c.getRed() + (255 - c.getRed()) * fraction));
            int g = Math.min(255, (int) (c.getGreen() + (255 - c.getGreen()) * fraction));
            int b = Math.min(255, (int) (c.getBlue() + (255 - c.getBlue()) * fraction));
            return new Color(r, g, b);
        }

        // Tu ve thanh phan thay cho cach ve mac dinh.
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            // Lam min duong ve hoac chu.
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            // Lam min duong ve hoac chu.
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();

            // Ve nut bi khoa bang mau xam.
            if (!isEnabled()) {
                // Trang thai vo hieu hoa: Nen xam nhat, vien ro, chu xam dam doc ro rang
                g2.setColor(new Color(233, 236, 239));
                // Ve nen hinh chu nhat bo goc.
                g2.fillRoundRect(0, 0, width, height, 6, 6);
                g2.setColor(new Color(206, 212, 218));
                g2.drawRoundRect(0, 0, width - 1, height - 1, 6, 6);
                g2.setColor(new Color(108, 117, 125));
            } else {
                // Dung mau dam khi dang nhan nut.
                if (getModel().isPressed()) {
                    g2.setColor(pressedBg);
                // Dung mau sang khi chuot nam tren nut.
                } else if (getModel().isRollover()) {
                    g2.setColor(hoverBg);
                } else {
                    g2.setColor(normalBg);
                }
                // Ve nen hinh chu nhat bo goc.
                g2.fillRoundRect(0, 0, width, height, 6, 6);
                g2.setColor(normalFg);
            }

            // Do chu de can giua noi dung tren nut.
            FontMetrics fm = g2.getFontMetrics();
            int textX = (width - fm.stringWidth(getText())) / 2;
            int textY = (height + fm.getAscent() - fm.getDescent()) / 2;
            g2.drawString(getText(), textX, textY);
            // Ve vien de biet nut duoc chon bang ban phim.
            if (isFocusOwner()) {
                g2.setColor(normalFg);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(3, 3, width - 7, height - 7, 5, 5);
            }
            // Giai phong ban sao doi tuong ve.
            g2.dispose();
        }

        // Bao kich thuoc phu hop cho bo sap xep giao dien.
        @Override
        public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            return new Dimension(d.width + 24, Math.max(32, d.height));
        }
    }
}
