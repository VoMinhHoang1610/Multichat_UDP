import java.net.*;
import java.util.*;

public class TestUDP {
    public static void main(String[] args) throws Exception {
        System.setProperty("java.net.preferIPv4Stack", "true");
        InetAddress group = InetAddress.getByName("224.2.2.3");
        int port = 8888;
        
        Enumeration<NetworkInterface> nifs = NetworkInterface.getNetworkInterfaces();
        while (nifs.hasMoreElements()) {
            NetworkInterface ni = nifs.nextElement();
            System.out.println("Interface: " + ni.getName() + " | " + ni.getDisplayName() + " | up=" + ni.isUp() + " | mc=" + ni.supportsMulticast() + " | loop=" + ni.isLoopback());
            Enumeration<InetAddress> addrs = ni.getInetAddresses();
            while (addrs.hasMoreElements()) {
                InetAddress a = addrs.nextElement();
                if (a instanceof Inet4Address) {
                    System.out.println("   IPv4: " + a.getHostAddress());
                }
            }
            if (ni.isUp() && ni.supportsMulticast()) {
                try {
                    MulticastSocket s = new MulticastSocket();
                    s.setNetworkInterface(ni);
                    byte[] buf = "HELLO".getBytes();
                    DatagramPacket p = new DatagramPacket(buf, buf.length, group, port);
                    s.send(p);
                    System.out.println("   -> SEND SUCCESS on " + ni.getName());
                    s.close();
                } catch (Exception ex) {
                    System.out.println("   -> SEND FAILED: " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
                }
            }
        }
    }
}
