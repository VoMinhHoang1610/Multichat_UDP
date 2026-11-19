package multichat_udp;

import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/** Quan ly socket UDP Multicast, thanh vien va cac luong nen. */
public class MulticastChatService {
    // Dia chi multicast mac dinh.
    public static final String DEFAULT_GROUP_IP = "224.2.2.3";
    // Cong UDP mac dinh.
    public static final int DEFAULT_PORT = 8888;
    // Nguong im lang 12 giay de xem la offline.
    private static final int USER_TIMEOUT_MS = 12000; // Thoi gian timeout de coi nhu user da offline

    // Ma phien gui de nhan biet tin cua minh.
    private final String senderId;
    // Ten hien thi cua phien hien tai.
    private String username;
    // Chuoi dia chi nhom tu GUI hoac CLI.
    private String groupAddress;
    // Cong UDP cua phong chat.
    private int port;

    // Socket UDP Multicast dung de gui va nhan.
    private MulticastSocket socket;
    // Dia chi IP nhom dang su dung.
    private InetAddress group;
    // Card mang de gui va tham gia nhom.
    private NetworkInterface networkInterface;

    // Co dung/chay; volatile giup cac luong thay gia tri moi.
    private volatile boolean running = false;
    // Luong nen cho goi UDP den.
    private Thread receiverThread;
    // Bo lap lich gui heartbeat.
    private ScheduledExecutorService heartbeatExecutor;

    // Ten thanh vien -> thoi diem hoat dong gan nhat.
    private final ConcurrentHashMap<String, Long> activeUsers = new ConcurrentHashMap<>();
    // Danh sach nhan su kien, ho tro truy cap nhieu luong.
    private final List<ChatListener> listeners = new CopyOnWriteArrayList<>();

    // Tao ma rieng de nhan biet tin do minh gui.
    public MulticastChatService() {
        this.senderId = UUID.randomUUID().toString();
    }

    // Dang ky doi tuong can nhan su kien chat.
    public void addListener(ChatListener listener) {
        listeners.add(listener);
    }

    // Go doi tuong khoi danh sach nhan su kien.
    public void removeListener(ChatListener listener) {
        listeners.remove(listener);
    }

    // Kiem tra service dang chay va socket con mo.
    public boolean isConnected() {
        return running && socket != null && !socket.isClosed();
    }

    // Tra ve ten hien thi cua phien hien tai.
    public String getUsername() {
        return username;
    }

    // Tra ve ma phien cua ben gui.
    public String getSenderId() {
        return senderId;
    }

    // Tra ve dia chi nhom dang su dung.
    public String getGroupAddress() {
        return groupAddress;
    }

    // Tra ve cong UDP dang su dung.
    public int getPort() {
        return port;
    }

    // Tao socket UDP, tham gia nhom va bat tac vu nen.
    public synchronized void connect(String username, String groupAddress, int port, NetworkInterface preferredNetIf) throws IOException {
        // Tu choi ket noi lap khi phien hien tai con mo.
        if (isConnected()) {
            throw new IllegalStateException("Đang kết nối tới nhóm multicast. Vui lòng ngắt kết nối trước.");
        }

        // Tu choi ten rong hoac null.
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Tên người dùng không được để trống.");
        }

        this.username = username.trim();
        this.groupAddress = groupAddress.trim();
        this.port = port;

        // Chuyen chuoi dia chi thanh doi tuong IP.
        this.group = InetAddress.getByName(this.groupAddress);
        // Chi chap nhan dia chi multicast.
        if (!this.group.isMulticastAddress()) {
            throw new IllegalArgumentException("Địa chỉ IP " + this.groupAddress + " không phải là địa chỉ Multicast lớp D (224.0.0.0 - 239.255.255.255)!");
        }

        // Tao socket UDP Multicast va gan vao cong nhan.
        this.socket = new MulticastSocket(this.port);
        // Bat tuy chon tai su dung dia chi socket.
        this.socket.setReuseAddress(true);

        try {
            // Uu tien card mang nguoi dung chon.
            if (preferredNetIf != null) {
                this.networkInterface = preferredNetIf;
            } else {
                // Tu tim card IPv4 phu hop neu chua chon.
                this.networkInterface = autoDetectMulticastInterface();
            }

            if (this.networkInterface != null) {
                try {
                    // Chon card mang de phat multicast.
                    this.socket.setNetworkInterface(this.networkInterface);
                } catch (Exception e) {
                    System.err.println("Không thể đặt network interface: " + e.getMessage());
                }
            }

            // 4. Gia nhap nhom Multicast (joinGroup)
            SocketAddress socketAddress = new InetSocketAddress(this.group, this.port);
            if (this.networkInterface != null) {
                try {
                    // Dang ky nhan UDP cua nhom tren card da chon.
                    this.socket.joinGroup(socketAddress, this.networkInterface);
                } catch (Exception ex) {
                    // Thu API joinGroup cu neu can.
                    joinGroupLegacy(this.socket, this.group);
                }
            } else {
                // Thu API joinGroup cu neu can.
                joinGroupLegacy(this.socket, this.group);
            }

            // Cho phep luong nhan va heartbeat bat dau chay.
            this.running = true;
            this.activeUsers.clear();
            // Danh dau chinh minh dang online.
            this.activeUsers.put(this.username, System.currentTimeMillis());

            // Bat luong nhan UDP rieng, khong chan GUI.
            startReceiverThread();

            // Bat lich gui heartbeat va kiem tra offline.
            startHeartbeatExecutor();

            // 7. Phat goi tin thong bao da tham gia phong
            ChatMessage joinMsg = ChatMessage.createJoin(this.senderId, this.username);
            // Phat JOIN cho cac may trong nhom.
            sendPacket(joinMsg);

            notifyStatusChanged("Đã kết nối vào nhóm " + this.groupAddress + ":" + this.port, true);
            // Bao danh sach online moi cho GUI hoac CLI.
            notifyUserListUpdated();
        } catch (IOException | RuntimeException ex) { // Don socket neu ket noi that bai giua chung.
            if (running) {
                disconnect();
            } else if (socket != null) {
                // Dong socket; lenh receive dang cho se thoat bang loi.
                socket.close();
                // Bo tham chieu den socket da dong.
                socket = null;
            }
            throw ex;
        }
    }

    @SuppressWarnings("deprecation")
    // Thu tham gia nhom bang API Java cu.
    private void joinGroupLegacy(MulticastSocket s, InetAddress grp) throws IOException {
        // Goi API cu de tham gia nhom multicast.
        s.joinGroup(grp);
    }

    @SuppressWarnings("deprecation")
    // Thu roi nhom bang API Java cu.
    private void leaveGroupLegacy(MulticastSocket s, InetAddress grp) throws IOException {
        // Goi API cu de roi nhom multicast.
        s.leaveGroup(grp);
    }

    // Bao roi nhom, dung heartbeat va dong socket UDP.
    public synchronized void disconnect() {
        // Khong can ngat neu service chua chay.
        if (!running) {
            return;
        }
        // Yeu cau luong nhan va heartbeat dung lai.
        running = false;

        // Gui thong bao roi phong truoc khi dong socket
        try {
            ChatMessage leaveMsg = ChatMessage.createLeave(this.senderId, this.username);
            // Gui LEAVE khi socket van con mo.
            sendPacket(leaveMsg);
        } catch (Exception ignored) {
        }

        // Dung heartbeat
        if (heartbeatExecutor != null) {
            // Yeu cau dung heartbeat va huy lich cho.
            heartbeatExecutor.shutdownNow();
            heartbeatExecutor = null;
        }

        // Roi nhom Multicast (leaveGroup)
        if (socket != null && !socket.isClosed()) {
            try {
                SocketAddress socketAddress = new InetSocketAddress(this.group, this.port);
                if (this.networkInterface != null) {
                    try {
                        // Roi nhom tren card mang da tham gia.
                        socket.leaveGroup(socketAddress, this.networkInterface);
                    } catch (Exception ex) {
                        leaveGroupLegacy(socket, this.group);
                    }
                } else {
                    leaveGroupLegacy(socket, this.group);
                }
            } catch (Exception ignored) {
            }

            // Dong socket; lenh receive dang cho se thoat bang loi.
            socket.close();
            // Bo tham chieu den socket da dong.
            socket = null;
        }

        if (receiverThread != null) {
            // Gui yeu cau ngat cho luong nhan.
            receiverThread.interrupt();
            receiverThread = null;
        }

        // Xoa danh sach online khi roi nhom.
        activeUsers.clear();
        notifyStatusChanged("Đã ngắt kết nối khỏi phòng chat.", false);
        // Bao danh sach online moi cho GUI hoac CLI.
        notifyUserListUpdated();
    }

    // Tao thong diep CHAT de gui ca nhom.
    public void sendMessage(String content) throws IOException {
        if (!isConnected()) {
            throw new IOException("Chưa kết nối vào phòng chat.");
        }
        ChatMessage msg = ChatMessage.createChat(this.senderId, this.username, content);
        sendPacket(msg);
    }

    // Tao WHISPER kem ten nguoi nhan rieng.
    public void sendWhisper(String targetUser, String content) throws IOException {
        if (!isConnected()) {
            throw new IOException("Chưa kết nối vào phòng chat.");
        }
        ChatMessage msg = ChatMessage.createWhisper(this.senderId, this.username, targetUser, content);
        sendPacket(msg);
    }

    // Doi tin thanh byte va gui mot goi UDP den nhom.
    private synchronized void sendPacket(ChatMessage message) throws IOException {
        // Bo qua neu socket chua tao hoac da dong.
        if (socket == null || socket.isClosed()) {
            return;
        }
        // Doi thong diep thanh byte truoc khi gui UDP.
        byte[] data = message.toBytes();
        // Tao goi UDP voi noi dung, IP nhom va cong dich.
        DatagramPacket packet = new DatagramPacket(data, data.length, group, port);
        // Gui UDP; chua co xac nhan ben kia da nhan.
        socket.send(packet);
    }

    // Tao luong rieng cho viec cho va nhan goi UDP.
    private void startReceiverThread() {
        // Tao luong nen rieng cho viec nhan UDP.
        receiverThread = new Thread(() -> {
            byte[] buffer = new byte[65535]; // Bo dem nhan; khong phai gioi han noi dung chat.
            // Tao goi rong de socket dien du lieu vao.
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            // Nhan lien tuc khi service va socket con mo.
            while (running && socket != null && !socket.isClosed()) {
                try {
                    // Cho goi UDP den; chi chan luong nhan nay.
                    socket.receive(packet);

                    // Giai ma dung so byte thuc te trong goi nhan.
                    ChatMessage message = ChatMessage.fromBytes(packet.getData(), packet.getOffset(), packet.getLength());
                    if (message != null) {
                        // Phan loai va xu ly thong diep vua nhan.
                        handleIncomingMessage(message);
                    }

                    // Khoi phuc suc chua de nhan du goi dai hon lan truoc.
                    packet.setLength(buffer.length);
                } catch (SocketException se) { // Socket dong hoac loi thi dung luong nhan.
                    // Socket bi dong do nguoi dung bam ngat ket noi
                    break;
                } catch (Exception ex) {
                    if (running) {
                        notifyError("Lỗi nhận dữ liệu: " + ex.getMessage());
                    }
                }
            }
        }, "Multicast-Receiver-Thread");

        // Khong giu JVM song chi vi luong nhan.
        receiverThread.setDaemon(true);
        // Bat dau vong nhan tren luong moi.
        receiverThread.start();
    }

    // Phan loai goi nhan va thong bao cho giao dien.
    private void handleIncomingMessage(ChatMessage message) {
        // So ma phien de nhan biet tin do minh gui.
        boolean isSelf = senderId.equals(message.getSenderId());
        // Lay ten gui de cap nhat trang thai online.
        String sender = message.getSenderName();

        switch (message.getType()) {
            // Thong bao co nguoi vua tham gia.
            case JOIN:
                // Cap nhat thoi diem hoat dong cua nguoi gui.
                activeUsers.put(sender, System.currentTimeMillis());
                // Bao danh sach online moi cho GUI hoac CLI.
                notifyUserListUpdated();
                // Bao tin moi kem co phan biet tin cua minh.
                notifyMessageReceived(message, isSelf);
                // Phan hoi heartbeat neu nguoi khac vua vao nhom.
                if (!isSelf) {
                    try {
                        // Bao cho nhom rang minh van online.
                        sendPacket(ChatMessage.createHeartbeat(this.senderId, this.username));
                    } catch (Exception ignored) {
                    }
                }
                break;

            // Thong bao co nguoi vua roi nhom.
            case LEAVE:
                // Xoa thanh vien vua bao roi nhom.
                activeUsers.remove(sender);
                // Bao danh sach online moi cho GUI hoac CLI.
                notifyUserListUpdated();
                // Bao tin moi kem co phan biet tin cua minh.
                notifyMessageReceived(message, isSelf);
                break;

            // Goi bao ben gui van online.
            case HEARTBEAT:
                // Cap nhat thoi diem hoat dong cua nguoi gui.
                activeUsers.put(sender, System.currentTimeMillis());
                // Bao danh sach online moi cho GUI hoac CLI.
                notifyUserListUpdated();
                break;

            // Tin chung cho tat ca thanh vien.
            case CHAT:
                // Cap nhat thoi diem hoat dong cua nguoi gui.
                activeUsers.put(sender, System.currentTimeMillis());
                // Bao tin moi kem co phan biet tin cua minh.
                notifyMessageReceived(message, isSelf);
                break;

            // Tin rieng; can loc dung nguoi nhan.
            case WHISPER:
                // Cap nhat thoi diem hoat dong cua nguoi gui.
                activeUsers.put(sender, System.currentTimeMillis());
                // Chi hien tin rieng cho nguoi gui hoac ten nhan; chua ma hoa.
                if (isSelf || (username != null && username.equalsIgnoreCase(message.getTargetUser()))) {
                    // Bao tin moi kem co phan biet tin cua minh.
                    notifyMessageReceived(message, isSelf);
                }
                break;
        }
    }

    // Gui tin bao song va don thanh vien qua han.
    private void startHeartbeatExecutor() {
        // Tao bo lap lich dung mot luong heartbeat.
        heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            // Dat ten cho luong chay tac vu heartbeat.
            Thread t = new Thread(r, "Multicast-Heartbeat-Thread");
            // Khong giu JVM song chi vi luong heartbeat.
            t.setDaemon(true);
            return t;
        });

        // Lap viec gui heartbeat va don thanh vien qua han.
        heartbeatExecutor.scheduleAtFixedRate(() -> {
            // Bo qua tac vu neu da roi nhom.
            if (!running) return;

            try {
                // Bao cho nhom rang minh van online.
                sendPacket(ChatMessage.createHeartbeat(this.senderId, this.username));

                // Lay gio hien tai de tinh thoi gian im lang.
                long now = System.currentTimeMillis();
                // Danh dau co thanh vien bi xoa hay khong.
                boolean listChanged = false;
                for (Map.Entry<String, Long> entry : activeUsers.entrySet()) {
                    // Khong tu xoa chinh minh khoi danh sach.
                    if (!entry.getKey().equals(this.username)) {
                        // Qua 12 giay im lang thi xem la offline.
                        if (now - entry.getValue() > USER_TIMEOUT_MS) {
                            // Xoa ten thanh vien qua han.
                            activeUsers.remove(entry.getKey());
                            // Danh dau can bao danh sach moi.
                            listChanged = true;
                        }
                    }
                }
                if (listChanged) {
                    // Bao danh sach online moi cho GUI hoac CLI.
                    notifyUserListUpdated();
                }
            } catch (Exception ignored) {
            }
        // Lan dau sau 2 giay, sau do moi 4 giay.
        }, 2, 4, TimeUnit.SECONDS);
    }

    // Uu tien card IPv4 hoat dong, ho tro multicast.
    private NetworkInterface autoDetectMulticastInterface() {
        try {
            // Lay cac card mang cua may tu he dieu hanh.
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            // Giu card noi bo lam phuong an du phong.
            NetworkInterface loopbackCandidate = null;

            // Duyet lan luot cac card mang tren may.
            while (interfaces.hasMoreElements()) {
                // Lay card mang tiep theo.
                NetworkInterface ni = interfaces.nextElement();
                // Bo card dang tat hoac khong ho tro multicast.
                if (!ni.isUp() || !ni.supportsMulticast()) continue;

                // Lay cac dia chi IP cua card nay.
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                // Duyet cac dia chi IP cua card dang xet.
                while (addresses.hasMoreElements()) {
                    // Lay dia chi IP tiep theo cua card.
                    InetAddress addr = addresses.nextElement();
                    // Chi lay dia chi IPv4.
                    if (addr instanceof Inet4Address) {
                        // Uu tien card LAN/Wi-Fi thay vi card noi bo.
                        if (!ni.isLoopback() && !addr.isLoopbackAddress()) {
                            return ni; // Uu tien card mang LAN / Wi-Fi dang hoat dong
                        }
                        if (loopbackCandidate == null) {
                            loopbackCandidate = ni;
                        }
                    }
                }
            }
            // Dung card noi bo du phong, hoac null neu khong co.
            return loopbackCandidate;
        } catch (Exception e) {
            return null;
        }
    }

    // Goi callback bao tin moi cho tung listener.
    private void notifyMessageReceived(ChatMessage message, boolean isSelf) {
        // Goi cac doi tuong da dang ky nhan su kien.
        for (ChatListener listener : listeners) {
            // Chuyen tin moi sang GUI hoac CLI.
            listener.onMessageReceived(message, isSelf);
        }
    }

    // Tao ban chup danh sach online roi bao cac listener.
    private void notifyUserListUpdated() {
        // Tao danh sach sap xep, khong cho listener sua truc tiep.
        Set<String> users = Collections.unmodifiableSet(new TreeSet<>(activeUsers.keySet()));
        // Goi cac doi tuong da dang ky nhan su kien.
        for (ChatListener listener : listeners) {
            // Chuyen ban chup danh sach thanh vien sang giao dien.
            listener.onUserListUpdated(users);
        }
    }

    // Bao trang thai ket noi cho cac listener.
    private void notifyStatusChanged(String status, boolean isConnected) {
        // Goi cac doi tuong da dang ky nhan su kien.
        for (ChatListener listener : listeners) {
            // Chuyen trang thai ket noi sang giao dien.
            listener.onStatusChanged(status, isConnected);
        }
    }

    // Bao loi mang cho cac listener.
    private void notifyError(String error) {
        // Goi cac doi tuong da dang ky nhan su kien.
        for (ChatListener listener : listeners) {
            // Chuyen thong bao loi sang giao dien.
            listener.onError(error);
        }
    }
}

