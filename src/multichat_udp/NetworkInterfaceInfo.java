package multichat_udp;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/** Luu thong tin va liet ke card mang ho tro multicast. */
public class NetworkInterfaceInfo {
    // Card mang de gui va tham gia nhom.
    private final NetworkInterface networkInterface;
    // Ten card mang de hien thi.
    private final String displayName;
    // Dia chi IPv4 cua card mang.
    private final String ipv4Address;
    // Danh dau card chi giao tiep noi bo may.
    private final boolean isLoopback;

    // Luu card mang va thong tin hien trong hop chon.
    public NetworkInterfaceInfo(NetworkInterface networkInterface, String displayName, String ipv4Address, boolean isLoopback) {
        this.networkInterface = networkInterface;
        this.displayName = displayName;
        this.ipv4Address = ipv4Address;
        this.isLoopback = isLoopback;
    }

    // Tra card mang thuc te de truyen cho socket.
    public NetworkInterface getNetworkInterface() {
        return networkInterface;
    }

    // Tra dia chi IPv4 cua card mang.
    public String getIpv4Address() {
        return ipv4Address;
    }

    // Cho biet day co phai card noi bo may hay khong.
    public boolean isLoopback() {
        return isLoopback;
    }

    // Tra chuoi hien ten card mang trong hop chon.
    @Override
    public String toString() {
        // null la lua chon tu dong, khong phai card cu the.
        if (networkInterface == null) {
            return "Tự động chọn (Auto Detect)";
        }
        String type = isLoopback ? "[Loopback]" : "[Mạng LAN/Wi-Fi]";
        return type + " " + displayName + " (" + ipv4Address + ")";
    }

    // Quet card dang chay, ho tro multicast va co IPv4.
    public static List<NetworkInterfaceInfo> getAvailableInterfaces() {
        List<NetworkInterfaceInfo> list = new ArrayList<>();
        // Tuy chon mac dinh Auto Detect
        list.add(new NetworkInterfaceInfo(null, "Tự động", "", false));

        try {
            // Lay cac card mang cua may tu he dieu hanh.
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            // Duyet lan luot cac card mang tren may.
            while (interfaces.hasMoreElements()) {
                // Lay card mang tiep theo.
                NetworkInterface ni = interfaces.nextElement();
                // Bo card dang tat hoac khong ho tro multicast.
                if (!ni.isUp() || !ni.supportsMulticast()) {
                    continue;
                }

                // Lay cac dia chi IP cua card nay.
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                // Duyet cac dia chi IP cua card dang xet.
                while (addresses.hasMoreElements()) {
                    // Lay dia chi IP tiep theo cua card.
                    InetAddress addr = addresses.nextElement();
                    // Chi lay dia chi IPv4.
                    if (addr instanceof Inet4Address) {
                        String name = ni.getDisplayName() != null ? ni.getDisplayName() : ni.getName();
                        // Them card kem ten, IPv4 va co danh dau noi bo.
                        list.add(new NetworkInterfaceInfo(ni, name, addr.getHostAddress(), ni.isLoopback()));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Lỗi quét card mạng: " + e.getMessage());
        }

        return list;
    }
}

