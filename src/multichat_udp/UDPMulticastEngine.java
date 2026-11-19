package multichat_udp;

import java.io.IOException;
import java.net.*;
import java.util.Enumeration;
import java.util.concurrent.*;

/**
 * Bo xu ly truyen thong UDP Multicast chuyen dung cho moi phong chat.
 * Dam bao dung ly thuyet Lap trinh Socket UDP va Multicast: DatagramSocket, DatagramPacket, MulticastSocket.
 */
public class UDPMulticastEngine {
    private static final int BUFFER_SIZE = 65535; // Kich thuoc dem UDP toi da
    private static final int HEARTBEAT_INTERVAL_SEC = 5;

    private final String roomCode;
    private final String multicastIp;
    private final int port;
    private final ChatRoom room;

    private MulticastSocket socket;
    private InetAddress groupAddress;
    private NetworkInterface networkInterface;

    private volatile boolean running = false;
    private Thread receiverThread;
    private ScheduledExecutorService heartbeatExecutor;

    public UDPMulticastEngine(ChatRoom room, String multicastIp, int port) {
        this.room = room;
        this.roomCode = room.getRoomCode();
        this.multicastIp = multicastIp;
        this.port = port;
    }

    /**
     * Khoi dong socket UDP Multicast va luong nhan goi tin ngam.
     */
    public synchronized void start() throws IOException {
        if (running) return;

        groupAddress = InetAddress.getByName(multicastIp);
        if (!groupAddress.isMulticastAddress()) {
            throw new IllegalArgumentException("Địa chỉ " + multicastIp + " không phải là địa chỉ Multicast (224.0.0.0 - 239.255.255.255)!");
        }

        // Chon card mang thich hop (LAN, Wi-Fi hoac Loopback)
        networkInterface = detectNetworkInterface();

        // Tao socket Multicast gan vao cong
        socket = new MulticastSocket(port);
        socket.setTimeToLive(32); // Cho phep truyen trong mang LAN

        try {
            socket.setReuseAddress(true);
        } catch (Exception ignored) {
        }

        // Gia nhap nhom multicast
        if (networkInterface != null) {
            try {
                socket.setNetworkInterface(networkInterface);
            } catch (Exception ignored) {
            }
            SocketAddress groupSockAddr = new InetSocketAddress(groupAddress, port);
            socket.joinGroup(groupSockAddr, networkInterface);
        } else {
            // Fallback neu khong xac dinh duoc interface cu the
            SocketAddress groupSockAddr = new InetSocketAddress(groupAddress, port);
            socket.joinGroup(groupSockAddr, null);
        }

        running = true;

        // Luong nhan goi tin UDP lien tuc
        receiverThread = new Thread(this::receiveLoop, "UDP-Receiver-" + roomCode);
        receiverThread.setDaemon(true);
        receiverThread.start();

        // Bo gui nhip tim dinh ky de xac nhan phong van song
        heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "UDP-Heartbeat-" + roomCode);
            t.setDaemon(true);
            return t;
        });
        heartbeatExecutor.scheduleAtFixedRate(this::sendHeartbeat, HEARTBEAT_INTERVAL_SEC, HEARTBEAT_INTERVAL_SEC, TimeUnit.SECONDS);

        System.out.println("[UDPMulticastEngine] Khoi dong phong " + roomCode + " tai " + multicastIp + ":" + port);
    }

    /**
     * Luong lang nghe va nhan goi tin UDP tu mang.
     */
    private void receiveLoop() {
        byte[] buffer = new byte[BUFFER_SIZE];
        while (running && socket != null && !socket.isClosed()) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                // Giai ma thong diep theo giao thuc MCUP
                ChatMessage msg = ChatMessage.fromBytes(packet.getData(), packet.getOffset(), packet.getLength());
                if (msg != null) {
                    // Chi xu ly cac tin nhan thuoc phong nay hoac khong gioi han phong
                    if (msg.getRoomCode() == null || msg.getRoomCode().isEmpty()
                            || msg.getRoomCode().equalsIgnoreCase(roomCode)
                            || msg.getRoomCode().equalsIgnoreCase("DEFAULT")) {

                        room.onMessageReceived(msg);
                    }
                }
            } catch (SocketException se) {
                if (!running) break;
            } catch (Exception e) {
                if (running) {
                    System.err.println("[UDP Error] Loi nhan goi UDP phong " + roomCode + ": " + e.getMessage());
                }
            }
        }
    }

    /**
     * Gui goi tin ChatMessage qua socket UDP Multicast.
     */
    public void sendMessage(ChatMessage msg) throws IOException {
        if (!running || socket == null || socket.isClosed()) {
            throw new IOException("Socket UDP Multicast của phòng chưa hoạt động.");
        }

        byte[] data = msg.toBytes();
        DatagramPacket packet = new DatagramPacket(data, data.length, groupAddress, port);

        // Dong bo gui qua socket de tranh tranh chap da luong
        synchronized (socket) {
            socket.send(packet);
        }
    }

    /**
     * Gui thong diep nhip tim qua UDP.
     */
    private void sendHeartbeat() {
        if (!running || socket == null || socket.isClosed()) return;
        try {
            ChatMessage hb = new ChatMessage(MessageType.HEARTBEAT, "SERVER", "UDP Server",
                    "PULSE", "", System.currentTimeMillis(), roomCode, "", "");
            sendMessage(hb);
        } catch (Exception ignored) {
        }
    }

    /**
     * Dong socket va roi khoi nhom multicast.
     */
    public synchronized void stop() {
        running = false;
        if (heartbeatExecutor != null) {
            heartbeatExecutor.shutdownNow();
        }

        if (socket != null && !socket.isClosed()) {
            try {
                if (groupAddress != null) {
                    SocketAddress groupSockAddr = new InetSocketAddress(groupAddress, port);
                    socket.leaveGroup(groupSockAddr, networkInterface);
                }
            } catch (Exception ignored) {
            }
            socket.close();
        }

        if (receiverThread != null) {
            receiverThread.interrupt();
        }
        System.out.println("[UDPMulticastEngine] Da dung phong " + roomCode);
    }

    public boolean isRunning() {
        return running && socket != null && !socket.isClosed();
    }

    /**
     * Tu dong tim card mang co the phat multicast tot nhat.
     */
    private NetworkInterface detectNetworkInterface() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            NetworkInterface loopback = null;
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (!ni.isUp() || !ni.supportsMulticast()) continue;
                if (ni.isLoopback()) {
                    loopback = ni;
                    continue;
                }
                // Uu tien card Ethernet hoac Wi-Fi dang co dia chi IPv4
                Enumeration<InetAddress> addrs = ni.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        return ni;
                    }
                }
            }
            if (loopback != null) return loopback;
        } catch (Exception ignored) {
        }
        return null;
    }
}
