from pathlib import Path
import re, json
root=Path('src/multichat_udp')
lex=re.compile(r'"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'|//[^\n]*|/\*[\s\S]*?\*/')
def code_only(s):
    return ''.join(lex.sub(lambda m: '' if m.group().startswith(('//','/*')) else m.group(),s).split())
originals=json.loads(Path('build/comment-check/originals.json').read_text(encoding='utf-8'))
summary={
'ChatListener':'Hop dong su kien giua service va GUI/CLI.',
'ChatMessage':'Du lieu tin nhan va cach dong goi/giai ma byte UDP.',
'MessageType':'Ma loai thong diep trong giao thuc chat.',
'MulticastChatService':'Quan ly socket UDP Multicast, thanh vien va cac luong nen.',
'MultichatUI':'Giao dien Swing voi tin nhan trai/phai va cac nut chuc nang.',
'MultichatCLI':'Giao dien dong lenh dung chung service UDP voi GUI.',
'Multichat_UDP':'Diem khoi chay; mac dinh mo GUI, tham so cli mo console.',
'NetworkInterfaceInfo':'Luu thong tin va liet ke card mang ho tro multicast.',
}
extra={
'private JTextField txtUsername, txtGroupIp, txtPort;':'Cac o nhap ten, IP nhom va cong UDP.',
'private JTextArea txtInput;':'O soan tin nhieu dong.',
'private JPanel sidebar, workspace;':'Bang ket noi ben trai va vung lam viec chinh.',
'private ModernButton btnSidebar, btnSave, btnClear, btnPrivate;':'Cac nut an/hien, luu, xoa va chon nhan rieng.',
'private ModernButton btnConnect, btnDisconnect, btnSend;':'Cac nut tham gia, roi nhom va gui tin.',
'private JLabel lblStatus, lblRoom, lblMembers;':'Nhan trang thai, dia chi nhom va so nguoi online.',
'JOIN((byte) 1),':'Bao vua tham gia nhom.',
'LEAVE((byte) 2),':'Bao vua roi nhom.',
'CHAT((byte) 3),':'Tin nhan chung.',
'HEARTBEAT((byte) 4),':'Tin bao van dang hoat dong.',
'WHISPER((byte) 5);':'Tin rieng kem ten nguoi nhan.',
'private String targetUser; // Dung khi gui tin nhan rieng (Whisper)':'Ten nhan rieng; rong neu gui chung.',
'private final boolean self;':'true neu day la tin do chinh phien nay gui.',
'private final RoundedPanel bubble;':'Khung nen bo goc cua mot tin nhan.',
'private final JTextArea body;':'Noi dung tin dang van ban thuan.',
'private final JLabel author, time;':'Nhan ten nguoi gui va gio tao tin.',
'private final Color fill;':'Mau nen duoc dung khi tu ve khung.',
'private final Color normalBg;':'Mau nen khi nut chua duoc tuong tac.',
'private final Color hoverBg;':'Mau nen khi re chuot len nut.',
'private final Color pressedBg;':'Mau nen khi dang nhan nut.',
'private final Color normalFg;':'Mau chu cua nut dang hoat dong.',
'final String username;':'Ten nguoi nhan; null tuong ung Ca nhom.',
'line = line.trim();':'Bo khoang trang hai dau dong lenh.',
'if (line.isEmpty()) {':'Bo qua dong trong.',
'String time = message.getFormattedTime();':'Lay gio tao tin de hien tren console.',
'String sender = message.getSenderName();':'Lay ten gui de cap nhat trang thai online.',
'NetworkInterfaceInfo net = (NetworkInterfaceInfo) interfaces.getSelectedItem();':'Lay card mang dang duoc chon tren giao dien.',
'btnConnect.setText("Đang kết nối…");':'Bao tac vu ket noi dang chay.',
'chatLog.append("[Hệ thống] ").append(text).append(\'\\n\');':'Ghi thong bao cuc bo vao bo dem de luu tep.',
'chatLog.append(\'[\').append(message.getFormattedTime()).append("] ")':'Luu gio va ten gui vao lich su tren may nay.',
'if (message.getType() == MessageType.WHISPER) chatLog.append(" [Riêng tới ").append(message.getTargetUser()).append(\']\');':'Danh dau tin rieng trong tep lich su.',
'chatLog.append(": ").append(message.getContent()).append(\'\\n\');':'Luu noi dung va ket thuc mot dong lich su.',
'JScrollBar bar = scrollPane.getVerticalScrollBar();':'Lay thanh cuon doc cua khung chat.',
'FontMetrics fm = body.getFontMetrics(body.getFont());':'Do chieu rong chu de tinh kich thuoc bong bong.',
'FontMetrics fm = g2.getFontMetrics();':'Do chu de can giua noi dung tren nut.',
'Thread t = new Thread(r, "Multicast-Heartbeat-Thread");':'Dat ten cho luong chay tac vu heartbeat.',
't.setDaemon(true);':'Khong giu JVM song chi vi luong heartbeat.',
'boolean listChanged = false;':'Danh dau co thanh vien bi xoa hay khong.',
'NetworkInterface loopbackCandidate = null;':'Giu card noi bo lam phuong an du phong.',
'while (interfaces.hasMoreElements()) {':'Duyet lan luot cac card mang tren may.',
'while (addresses.hasMoreElements()) {':'Duyet cac dia chi IP cua card dang xet.',
'NetworkInterface ni = interfaces.nextElement();':'Lay card mang tiep theo.',
'InetAddress addr = addresses.nextElement();':'Lay dia chi IP tiep theo cua card.',
'return loopbackCandidate;':'Dung card noi bo du phong, hoac null neu khong co.',
'list.add(new NetworkInterfaceInfo(null, "Tự động", "", false));':'Them lua chon de service tu tim card mang.',
'list.add(new NetworkInterfaceInfo(ni, name, addr.getHostAddress(), ni.isLoopback()));':'Them card kem ten, IPv4 va co danh dau noi bo.',
'if (networkInterface == null) {':'null la lua chon tu dong, khong phai card cu the.',
'NetworkInterfaceInfo net = (NetworkInterfaceInfo) interfaces.getSelectedItem();':'Lay card dang chon; tuy chon tu dong chua card null.',
'chatService.connect(username, groupIp, port, net == null ? null : net.getNetworkInterface());':'Goi service tham gia nhom voi card da chon.',
'chatService.connect(username, groupIp, port, null);':'CLI tham gia nhom va de service tu chon card.',
'get();':'Lay ket qua SwingWorker; nem loi neu tac vu that bai.',
'System.exit(0);':'Ket thuc tien trinh Java cua ung dung.',
'dispose();':'Giai phong tai nguyen cua cua so.',
}
for path in root.glob('*.java'):
    s=path.read_text(encoding='utf-8')
    # Rut gon JavaDoc cu neu ngay sau da co comment phuong thuc moi.
    s=re.sub(r'/\*\*(?:(?!\*/)[\s\S])*\*/(?=\s*//)', '', s)
    # Rut gon JavaDoc cap lop, khong thay noi dung chuoi.
    m=re.search(r'/\*\*[\s\S]*?\*/',s)
    if m and 'public class' in s[m.end():m.end()+100]:
        s=s[:m.start()]+'/** '+summary[path.stem]+' */'+s[m.end():]
    if path.stem in ('MessageType',):
        s=s.replace('public enum MessageType', '/** '+summary[path.stem]+' */\npublic enum MessageType',1)
    out=[]
    for line in s.splitlines():
        t=line.strip()
        # Gop chu thich lien tiep de tranh lap lai cung y.
        if t.startswith('//') and out and out[-1].strip().startswith('//'):
            out.pop()
        if t in extra and not (out and out[-1].strip().startswith('//')):
            out.append(line[:len(line)-len(line.lstrip())]+'// '+extra[t])
        if t.startswith('String[] parts = line.split('):
            out.append(line[:len(line)-len(line.lstrip())]+'// Tach thanh lenh, ten nhan va phan noi dung con lai.')
        if 'byte[] buffer = new byte[65535];' in line:
            line=line.split('//')[0].rstrip()+' // Bo dem nhan; khong phai gioi han noi dung chat.'
        if '} catch (SocketException se)' in t and out and out[-1].strip().startswith('//'):
            out.pop()
            line=line+' // Socket dong hoac loi thi dung luong nhan.'
        if '} catch (IOException | RuntimeException ex)' in t and out and out[-1].strip().startswith('//'):
            out.pop()
            line=line+' // Don socket neu ket noi that bai giua chung.'
        out.append(line)
    s='\n'.join(out)+'\n'
    # Gom dong trong do go bo chu thich cu.
    s=re.sub(r'\n[ \t]*\n[ \t]*\n','\n\n',s)
    assert code_only(originals[path.name])==code_only(s),path.name
    assert all(m.group().isascii() for m in lex.finditer(s) if m.group().startswith(('//','/*'))),path.name
    path.write_text(s,encoding='utf-8')
print('PASS: 8 files contain ASCII comments; executable code and UI strings unchanged')
