from pathlib import Path
import re, unicodedata, json

root = Path('src/multichat_udp')
lexer = re.compile(r'"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'|//[^\n]*|/\*[\s\S]*?\*/')
def ascii_text(text):
    return unicodedata.normalize('NFD', text.replace('đ', 'd').replace('Đ', 'D')).encode('ascii', 'ignore').decode('ascii')
def code_only(text):
    return ''.join(lexer.sub(lambda m: '' if m.group().startswith(('//','/*')) else m.group(), text).split())

methods = {
'main':'Diem bat dau; chon giao dien cua so hoac dong lenh.',
'MultichatUI':'Tao giao dien va dang ky nhan su kien tu lop mang.',
'MultichatCLI':'Tao bo xu ly mang cho che do dong lenh.',
'MulticastChatService':'Tao ma rieng de nhan biet tin do minh gui.',
'ChatMessage':'Luu thong tin tin nhan va thay null bang gia tri mac dinh.',
'NetworkInterfaceInfo':'Luu card mang va thong tin hien trong hop chon.',
'MessageType':'Gan ma byte cho tung loai thong diep.',
'label':'Tao nhan voi font va mau chu thong nhat.',
'initUI':'Tao cua so va sap xep cac vung chuc nang.',
'createTopPanel':'Tao logo, nut an/hien bang ket noi va huong dan.',
'secondaryButton':'Tao nut phu voi mau nen nhe.',
'setSidebarVisible':'An/hien bang ket noi va cap nhat bo cuc.',
'createSidebar':'Tao o ket noi, chon card mang va danh sach thanh vien.',
'styleCombo':'Dinh dang hop chon va tu ve mui ten.',
'createArrowButton':'Tao nut mo danh sach cua hop chon.',
'getListCellRendererComponent':'Dinh dang tung dong trong danh sach.',
'styleInput':'Dinh dang o nhap ten, dia chi nhom va cong.',
'createCenterPanel':'Tao thanh cong cu va khung hien thi chat.',
'configureScrollBarColors':'Dat mau thanh cuon va ranh cuon.',
'createDecreaseButton':'Bo nut mui ten o dau thanh cuon.',
'createIncreaseButton':'Bo nut mui ten o cuoi thanh cuon.',
'hiddenButton':'Tao nut rong de an mui ten cuon mac dinh.',
'paintThumb':'Ve phan keo cua thanh cuon voi goc bo tron.',
'showEmptyState':'Hien loi chao khi khung chat chua co noi dung.',
'createBottomContainer':'Tao o chon nguoi nhan, soan tin va nut gui.',
'setupWindowListeners':'Dang ky don ket noi khi dong cua so.',
'windowClosing':'Roi nhom, dong cua so va ket thuc ung dung.',
'actionConnect':'Kiem tra du lieu roi ket noi mang o luong nen.',
'doInBackground':'Chay ket noi o nen de giao dien van phan hoi.',
'done':'Nhan ket qua ket noi va cap nhat GUI tren luong Swing.',
'actionDisconnect':'Roi nhom va khoa cac thanh phan gui tin.',
'actionSendMessage':'Lay noi dung va gui chung hoac rieng theo nguoi nhan.',
'updateUIState':'Bat/tat cac o va nut theo trang thai ket noi.',
'addRow':'Them hang chat; chi tu cuon neu dang gan cuoi.',
'appendSystemMessage':'Luu va hien thong bao he thong tren luong Swing.',
'onMessageReceived':'Nhan tin tu service de hien len GUI hoac CLI.',
'onUserListUpdated':'Nhan danh sach thanh vien online tu service.',
'onStatusChanged':'Nhan thay doi trang thai ket noi tu service.',
'onError':'Nhan loi tu service va thong bao cho nguoi dung.',
'refreshMembers':'Cap nhat thanh vien va giu nguoi nhan neu con online.',
'updatePrivateButton':'Chi bat nhan rieng khi chon mot thanh vien khac.',
'selectPrivateRecipient':'Chon nguoi nhan theo ten dang chon trong danh sach.',
'updateComposeLabel':'Hien che do gui chung hoac rieng.',
'updateSendButton':'Chi bat nut gui khi da ket noi va co noi dung.',
'scrollToLatest':'Dua thanh cuon xuong cuoi sau khi bo cuc cap nhat.',
'clearConversation':'Xoa chat tren may nay, khong gui yeu cau len mang.',
'saveConversation':'Chon noi luu va ghi hoi thoai ra tep UTF-8.',
'showHelp':'Mo hop thoai huong dan su dung.',
'Recipient':'Luu ten nguoi nhan; null nghia la gui ca nhom.',
'RoundedPanel':'Tao khung nen bo goc voi mau duoc truyen vao.',
'paintComponent':'Tu ve thanh phan thay cho cach ve mac dinh.',
'Transcript':'Xep cac hang tin nhan theo chieu doc.',
'getPreferredScrollableViewportSize':'De xuat kich thuoc ban dau cua vung chat.',
'getScrollableUnitIncrement':'So diem anh cuon cho moi buoc nho.',
'getScrollableBlockIncrement':'Cuon gan mot trang cho moi buoc lon.',
'getScrollableTracksViewportWidth':'Cho khung chat bam theo chieu rong vung cuon.',
'getScrollableTracksViewportHeight':'Cho noi dung cao hon vung hien thi de cuon doc.',
'MessageRow':'Tao bong bong tin; self cho biet tin cua minh.',
'bubbleSize':'Tinh kich thuoc bong bong theo noi dung va chieu rong.',
'getPreferredSize':'Bao kich thuoc phu hop cho bo sap xep giao dien.',
'getMaximumSize':'Gioi han chieu cao hang de tranh bi keo gian.',
'doLayout':'Dat tin cua minh ben phai, tin nguoi khac ben trai.',
'SystemRow':'Tao hang thong bao he thong can giua.',
'ModernButton':'Tao nut co mau nen, hover va trang thai nhan.',
'brighten':'Pha them trang de tao mau khi re chuot.',
'componentResized':'Tinh lai bo cuc khi kich thuoc vung hien thi doi.',
'mouseClicked':'Xu ly nhap dup de chon nguoi nhan rieng.',
'actionPerformed':'Xu ly khi nguoi dung kich hoat hanh dong.',
'insertUpdate':'Cap nhat nut gui khi them ky tu.',
'removeUpdate':'Cap nhat nut gui khi xoa ky tu.',
'changedUpdate':'Cap nhat nut gui khi tai lieu doi thuoc tinh.',
'addListener':'Dang ky doi tuong can nhan su kien chat.',
'removeListener':'Go doi tuong khoi danh sach nhan su kien.',
'isConnected':'Kiem tra service dang chay va socket con mo.',
'getUsername':'Tra ve ten hien thi cua phien hien tai.',
'getSenderId':'Tra ve ma phien cua ben gui.',
'getGroupAddress':'Tra ve dia chi nhom dang su dung.',
'getPort':'Tra ve cong UDP dang su dung.',
'connect':'Tao socket UDP, tham gia nhom va bat tac vu nen.',
'joinGroupLegacy':'Thu tham gia nhom bang API Java cu.',
'leaveGroupLegacy':'Thu roi nhom bang API Java cu.',
'disconnect':'Bao roi nhom, dung heartbeat va dong socket UDP.',
'sendMessage':'Tao thong diep CHAT de gui ca nhom.',
'sendWhisper':'Tao WHISPER kem ten nguoi nhan rieng.',
'sendPacket':'Doi tin thanh byte va gui mot goi UDP den nhom.',
'startReceiverThread':'Tao luong rieng cho viec cho va nhan goi UDP.',
'handleIncomingMessage':'Phan loai goi nhan va thong bao cho giao dien.',
'startHeartbeatExecutor':'Gui tin bao song va don thanh vien qua han.',
'autoDetectMulticastInterface':'Uu tien card IPv4 hoat dong, ho tro multicast.',
'notifyMessageReceived':'Goi callback bao tin moi cho tung listener.',
'notifyUserListUpdated':'Tao ban chup danh sach online roi bao cac listener.',
'notifyStatusChanged':'Bao trang thai ket noi cho cac listener.',
'notifyError':'Bao loi mang cho cac listener.',
'getType':'Tra ve loai thong diep de phan nhanh xu ly.',
'getSenderName':'Tra ve ten hien thi cua nguoi gui.',
'getContent':'Tra ve noi dung tin nhan.',
'getTargetUser':'Tra ve ten nhan rieng; rong neu gui chung.',
'getTimestamp':'Tra ve thoi diem tao tin theo mili giay.',
'getFormattedTime':'Doi thoi diem tao tin sang gio:phut:giay.',
 'toBytes':'Ghi cac truong dung thu tu de gui qua UDP.',
'fromBytes':'Doc goi MCUP; neu khong hop le thi thu doc van ban.',
'createJoin':'Tao thong bao thanh vien vua tham gia.',
'createLeave':'Tao thong bao thanh vien vua roi nhom.',
'createChat':'Tao tin nhan gui tat ca thanh vien.',
'createWhisper':'Tao tin rieng kem ten nguoi nhan.',
'createHeartbeat':'Tao tin bao ben gui van hoat dong.',
'getCode':'Tra ve ma byte de ghi vao goi tin.',
'fromCode':'Doi ma byte nhan duoc thanh loai thong diep.',
'getNetworkInterface':'Tra card mang thuc te de truyen cho socket.',
'getIpv4Address':'Tra dia chi IPv4 cua card mang.',
'isLoopback':'Cho biet day co phai card noi bo may hay khong.',
'getAvailableInterfaces':'Quet card dang chay, ho tro multicast va co IPv4.',
'start':'Doc tham so, ket noi va xu ly tung dong lenh CLI.',
}
fields = {
'BACKGROUND':'Mau nen chung.', 'INK':'Mau chu chinh.', 'MUTED':'Mau chu phu.', 'BLUE':'Mau nhan va mau tin cua minh.',
'DEFAULT_GROUP_IP':'Dia chi multicast mac dinh.', 'DEFAULT_PORT':'Cong UDP mac dinh.',
'USER_TIMEOUT_MS':'Nguong im lang 12 giay de xem la offline.',
'MAGIC_NUMBER':'Dau nhan MCUP de nhan dien dinh dang goi.',
'TIME_FORMAT':'Dinh dang gio hien tren tin nhan.',
'chatService':'Bo xu ly ket noi, gui va nhan UDP.',
'memberModel':'Du lieu ten thanh vien cho JList.', 'memberList':'Danh sach thanh vien tren giao dien.',
'chatLog':'Bo dem hoi thoai de xuat ra tep.', 'connecting':'Chan ket noi lap khi dang cho ket qua.',
'activeUsers':'Ten thanh vien -> thoi diem hoat dong gan nhat.',
'listeners':'Danh sach nhan su kien, ho tro truy cap nhieu luong.',
'running':'Co dung/chay; volatile giup cac luong thay gia tri moi.',
'receiverThread':'Luong nen cho goi UDP den.', 'heartbeatExecutor':'Bo lap lich gui heartbeat.',
'socket':'Socket UDP Multicast dung de gui va nhan.', 'group':'Dia chi IP nhom dang su dung.',
'networkInterface':'Card mang de gui va tham gia nhom.',
'senderId':'Ma phien gui de nhan biet tin cua minh.', 'username':'Ten hien thi cua phien hien tai.',
'groupAddress':'Chuoi dia chi nhom tu GUI hoac CLI.', 'port':'Cong UDP cua phong chat.',
'emptyState':'Loi chao khi chua co noi dung.', 'onlineCount':'So thanh vien dang online.',
'transcript':'Khung chua cac hang tin nhan.', 'scrollPane':'Vung cuon doc cua khung chat.',
'lblCompose':'Nhan cho biet dang gui chung hay rieng.', 'recipients':'Hop chon nguoi nhan.',
'interfaces':'Hop chon card mang hoac tu dong.', 'type':'Loai thong diep de phan nhanh xu ly.',
'senderName':'Ten hien thi cua nguoi gui.', 'content':'Noi dung van ban cua tin.',
'targetUser':'Ten nhan rieng; rong neu gui chung.', 'timestamp':'Thoi diem tao tin theo mili giay.',
'displayName':'Ten card mang de hien thi.', 'ipv4Address':'Dia chi IPv4 cua card mang.',
'isLoopback':'Danh dau card chi giao tiep noi bo may.', 'code':'Ma byte dai dien loai thong diep.',
}
# Chu thich nghia cua cac cau lenh quan trong, khong doi cau lenh.
exact = {
'chatService = new MulticastChatService();':'Tao bo xu ly mang cho cua so nay.',
'chatService.addListener(this);':'Dang ky cua so nhan tin va trang thai tu service.',
'initUI();':'Dung cac thanh phan giao dien.',
'setupWindowListeners();':'Gan xu ly dong cua so de don socket.',
'new MultichatCLI().start(args);':'Chay CLI va khong mo cua so GUI.',
'SwingUtilities.invokeLater(() -> {':'Dua cong viec cap nhat GUI ve luong Swing.',
'ui.setVisible(true);':'Hien cua so chat.',
'setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);':'Tu xu ly dong de gui LEAVE truoc khi thoat.',
'setLocationRelativeTo(null);':'Dat cua so o giua man hinh.',
'setSidebarVisible(getWidth() >= 1000);':'An bang ben neu cua so hep hon 1000 diem anh.',
'if (connecting || chatService.isConnected()) return;':'Bo qua neu dang ket noi hoac da tham gia nhom.',
'port = Integer.parseInt(portStr);':'Doi cong tu chuoi sang so; chuoi sai se gay loi.',
'if (port < 1024 || port > 65535) {':'GUI chi chap nhan cong trong khoang nay.',
'connecting = true;':'Khoa nut de tranh bam ket noi nhieu lan.',
'connecting = false;':'Ket thuc trang thai dang cho ket noi.',
'new SwingWorker<Void, Void>() {':'Chay ket noi o nen de cua so khong bi dung.',
'get();':'Lay ket qua tac vu; nem loi neu ket noi that bai.',
'}.execute();':'Bat dau tac vu SwingWorker.',
'chatService.disconnect();':'Bao roi nhom va don tai nguyen mang.',
'String content = txtInput.getText().trim();':'Lay noi dung va bo khoang trang hai dau.',
'if (content.isEmpty()) {':'Bo qua tin rong.',
'Recipient recipient = (Recipient) recipients.getSelectedItem();':'Lay nguoi nhan dang chon.',
'if (recipient != null && recipient.username != null) {':'Co ten nguoi nhan thi gui rieng.',
'if (!memberModel.contains(recipient.username)) {':'Khong gui rieng neu ten nay khong con online.',
'chatService.sendWhisper(recipient.username, content);':'Gui WHISPER kem ten nguoi nhan rieng.',
'chatService.sendMessage(content);':'Gui CHAT cho ca nhom multicast.',
'chatService.sendMessage(line);':'Gui dong van ban cho ca nhom.',
'chatService.sendWhisper(parts[1], parts[2]);':'Gui rieng theo ten va noi dung tach tu lenh.',
'txtInput.setText("");':'Xoa o soan sau khi gui.',
'txtInput.requestFocusInWindow();':'Dua con tro lai o soan tin.',
'scrollToLatest();':'Cuon toi cuoi cuoc tro chuyen.',
'if (!connected) {':'Don danh sach cu khi ngat ket noi.',
'memberModel.clear();':'Xoa cac ten cu truoc khi cap nhat.',
'recipients.removeAllItems();':'Xoa cac lua chon nguoi nhan cu.',
'recipients.addItem(new Recipient(null));':'Them lua chon gui cho ca nhom.',
'boolean atBottom = bar.getValue() + bar.getVisibleAmount() >= bar.getMaximum() - 48;':'Chi tu cuon neu dang doc gan cuoi khung chat.',
'if (emptyState != null) {':'Go loi chao khi bat dau co noi dung.',
'emptyState = null;':'Danh dau loi chao da duoc go.',
'transcript.add(row);':'Them hang tin vao cuoi khung chat.',
'if (atBottom) SwingUtilities.invokeLater(() -> bar.setValue(bar.getMaximum()));':'Doi bo cuc xong roi tu cuon toi tin moi.',
'if (message.getType() != MessageType.HEARTBEAT) {':'Khong ghi goi bao song vao lich su chat.',
'addRow(new MessageRow(message, isSelf));':'Tao bong bong; isSelf quyet dinh trai hay phai.',
'case JOIN:':'Thong bao co nguoi vua tham gia.',
'case LEAVE:':'Thong bao co nguoi vua roi nhom.',
'case HEARTBEAT:':'Goi bao ben gui van online.',
'case CHAT:':'Tin chung cho tat ca thanh vien.',
'case WHISPER:':'Tin rieng; can loc dung nguoi nhan.',
'boolean isSelf = senderId.equals(message.getSenderId());':'So ma phien de nhan biet tin do minh gui.',
'refreshMembers(onlineUsers);':'Dong bo thanh vien va o chon nguoi nhan.',
'onlineCount = onlineUsers.size();':'Dem thanh vien de hien len tieu de.',
'String target = selectedRecipient == null ? null : selectedRecipient.username;':'Giu nguoi nhan truoc khi tao lai danh sach.',
'for (String user : new java.util.TreeSet<>(users)) {':'Sap xep ten de danh sach de tim.',
'memberModel.addElement(user);':'Them ten vao danh sach hien thi.',
'if (!user.equals(chatService.getUsername())) {':'Khong them chinh minh vao lua chon nhan rieng.',
'if (user.equals(target)) recipients.setSelectedItem(recipient);':'Khoi phuc nguoi nhan neu van online.',
'if (target != null && !users.contains(target)) {':'Nguoi nhan offline thi chuyen ve ca nhom.',
'String user = memberList.getSelectedValue();':'Lay ten dang chon trong danh sach thanh vien.',
'recipients.setSelectedIndex(i);':'Chuyen o nguoi nhan sang thanh vien da chon.',
'boolean privateMessage = recipient != null && recipient.username != null;':'Phan biet gui rieng voi gui ca nhom.',
'transcript.removeAll();':'Xoa cac hang chat tren may nay.',
'chatLog.setLength(0);':'Xoa noi dung trong bo dem lich su.',
'showEmptyState();':'Hien loi chao cho khung chat rong.',
'JFileChooser chooser = new JFileChooser();':'Mo hop chon duong dan luu tep.',
'if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;':'Dung neu nguoi dung huy chon tep.',
'Files.writeString(file.toPath(), chatLog.toString(), StandardCharsets.UTF_8);':'Ghi hoi thoai bang UTF-8 de giu dung tieng Viet.',
'body.setLineWrap(true);':'Tu xuong dong neu noi dung dai hon bong bong.',
'body.setWrapStyleWord(true);':'Uu tien ngat dong theo tu.',
'body.setEditable(false);':'Cho doc va sao chep, khong cho sua tin da nhan.',
'bubble = new RoundedPanel(self ? BLUE : Color.WHITE);':'Tin cua minh nen xanh; tin nguoi khac nen trang.',
'Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(body.getText()), null);':'Sao chep toan bo noi dung tin vao clipboard.',
'body.setComponentPopupMenu(menu);':'Gan menu sao chep cho thao tac nhap phai.',
'int maxWidth = Math.max(100, (int) ((available - 40) * 0.74));':'Gioi han bong bong de con khoang trong hai ben.',
'body.setSize(Math.max(1, width - 28), Integer.MAX_VALUE);':'Dat chieu rong de tinh chieu cao sau xuong dong.',
'bubble.setBounds(self ? getWidth() - size.width - 18 : 18, 4, size.width, size.height);':'Tin cua minh can phai; tin nguoi khac can trai.',
'notice.setParagraphAttributes(attrs, false);':'Ap dung can giua cho thong bao he thong.',
'if (!isEnabled()) {':'Ve nut bi khoa bang mau xam.',
'if (getModel().isPressed()) {':'Dung mau dam khi dang nhan nut.',
'} else if (getModel().isRollover()) {':'Dung mau sang khi chuot nam tren nut.',
'if (isFocusOwner()) {':'Ve vien de biet nut duoc chon bang ban phim.',
'g2.dispose();':'Giai phong ban sao doi tuong ve.',
'g.dispose();':'Giai phong ban sao doi tuong ve.',
'if (isConnected()) {':'Tu choi ket noi lap khi phien hien tai con mo.',
'if (username == null || username.trim().isEmpty()) {':'Tu choi ten rong hoac null.',
 'this.group = InetAddress.getByName(this.groupAddress);':'Chuyen chuoi dia chi thanh doi tuong IP.',
'if (!this.group.isMulticastAddress()) {':'Chi chap nhan dia chi multicast.',
 'this.socket = new MulticastSocket(this.port);':'Tao socket UDP Multicast va gan vao cong nhan.',
 'this.socket.setReuseAddress(true);':'Bat tuy chon tai su dung dia chi socket.',
'if (preferredNetIf != null) {':'Uu tien card mang nguoi dung chon.',
 'this.networkInterface = autoDetectMulticastInterface();':'Tu tim card IPv4 phu hop neu chua chon.',
 'this.socket.setNetworkInterface(this.networkInterface);':'Chon card mang de phat multicast.',
 'this.socket.joinGroup(socketAddress, this.networkInterface);':'Dang ky nhan UDP cua nhom tren card da chon.',
'joinGroupLegacy(this.socket, this.group);':'Thu API joinGroup cu neu can.',
 'this.running = true;':'Cho phep luong nhan va heartbeat bat dau chay.',
 'this.activeUsers.put(this.username, System.currentTimeMillis());':'Danh dau chinh minh dang online.',
'startReceiverThread();':'Bat luong nhan UDP rieng, khong chan GUI.',
'startHeartbeatExecutor();':'Bat lich gui heartbeat va kiem tra offline.',
'sendPacket(joinMsg);':'Phat JOIN cho cac may trong nhom.',
'} catch (IOException | RuntimeException ex) {':'Don tai nguyen neu loi giua luc tham gia nhom.',
's.joinGroup(grp);':'Goi API cu de tham gia nhom multicast.',
's.leaveGroup(grp);':'Goi API cu de roi nhom multicast.',
'if (!running) {':'Khong can ngat neu service chua chay.',
'running = false;':'Yeu cau luong nhan va heartbeat dung lai.',
'sendPacket(leaveMsg);':'Gui LEAVE khi socket van con mo.',
'heartbeatExecutor.shutdownNow();':'Yeu cau dung heartbeat va huy lich cho.',
'socket.leaveGroup(socketAddress, this.networkInterface);':'Roi nhom tren card mang da tham gia.',
'socket.close();':'Dong socket; lenh receive dang cho se thoat bang loi.',
'socket = null;':'Bo tham chieu den socket da dong.',
'receiverThread.interrupt();':'Gui yeu cau ngat cho luong nhan.',
'activeUsers.clear();':'Xoa danh sach online khi roi nhom.',
'if (socket == null || socket.isClosed()) {':'Bo qua neu socket chua tao hoac da dong.',
'byte[] data = message.toBytes();':'Doi thong diep thanh byte truoc khi gui UDP.',
'DatagramPacket packet = new DatagramPacket(data, data.length, group, port);':'Tao goi UDP voi noi dung, IP nhom va cong dich.',
'socket.send(packet);':'Gui UDP; chua co xac nhan ben kia da nhan.',
'receiverThread = new Thread(() -> {':'Tao luong nen rieng cho viec nhan UDP.',
'DatagramPacket packet = new DatagramPacket(buffer, buffer.length);':'Tao goi rong de socket dien du lieu vao.',
'while (running && socket != null && !socket.isClosed()) {':'Nhan lien tuc khi service va socket con mo.',
'socket.receive(packet);':'Cho goi UDP den; chi chan luong nhan nay.',
'ChatMessage message = ChatMessage.fromBytes(packet.getData(), packet.getOffset(), packet.getLength());':'Giai ma dung so byte thuc te trong goi nhan.',
'handleIncomingMessage(message);':'Phan loai va xu ly thong diep vua nhan.',
'packet.setLength(buffer.length);':'Khoi phuc suc chua de nhan du goi dai hon lan truoc.',
'} catch (SocketException se) {':'Socket dong hoac loi socket thi thoat vong nhan.',
'receiverThread.setDaemon(true);':'Khong giu JVM song chi vi luong nhan.',
'receiverThread.start();':'Bat dau vong nhan tren luong moi.',
'activeUsers.put(sender, System.currentTimeMillis());':'Cap nhat thoi diem hoat dong cua nguoi gui.',
'activeUsers.remove(sender);':'Xoa thanh vien vua bao roi nhom.',
'notifyUserListUpdated();':'Bao danh sach online moi cho GUI hoac CLI.',
'notifyMessageReceived(message, isSelf);':'Bao tin moi kem co phan biet tin cua minh.',
'if (!isSelf) {':'Phan hoi heartbeat neu nguoi khac vua vao nhom.',
'if (isSelf || (username != null && username.equalsIgnoreCase(message.getTargetUser()))) {':'Chi hien tin rieng cho nguoi gui hoac ten nhan; chua ma hoa.',
'heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(r -> {':'Tao bo lap lich dung mot luong heartbeat.',
'heartbeatExecutor.scheduleAtFixedRate(() -> {':'Lap viec gui heartbeat va don thanh vien qua han.',
'if (!running) return;':'Bo qua tac vu neu da roi nhom.',
'sendPacket(ChatMessage.createHeartbeat(this.senderId, this.username));':'Bao cho nhom rang minh van online.',
'long now = System.currentTimeMillis();':'Lay gio hien tai de tinh thoi gian im lang.',
'boolean listChanged = false;':'Danh dau co thanh vien bi xoa hay khong.',
'if (!entry.getKey().equals(this.username)) {':'Khong tu xoa chinh minh khoi danh sach.',
'if (now - entry.getValue() > USER_TIMEOUT_MS) {':'Qua 12 giay im lang thi xem la offline.',
'activeUsers.remove(entry.getKey());':'Xoa ten thanh vien qua han.',
'listChanged = true;':'Danh dau can bao danh sach moi.',
'}, 2, 4, TimeUnit.SECONDS);':'Lan dau sau 2 giay, sau do moi 4 giay.',
'Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();':'Lay cac card mang cua may tu he dieu hanh.',
'if (!ni.isUp() || !ni.supportsMulticast()) continue;':'Bo card dang tat hoac khong ho tro multicast.',
'if (!ni.isUp() || !ni.supportsMulticast()) {':'Bo card dang tat hoac khong ho tro multicast.',
'Enumeration<InetAddress> addresses = ni.getInetAddresses();':'Lay cac dia chi IP cua card nay.',
'if (addr instanceof Inet4Address) {':'Chi lay dia chi IPv4.',
'if (!ni.isLoopback() && !addr.isLoopbackAddress()) {':'Uu tien card LAN/Wi-Fi thay vi card noi bo.',
'for (ChatListener listener : listeners) {':'Goi cac doi tuong da dang ky nhan su kien.',
'listener.onMessageReceived(message, isSelf);':'Chuyen tin moi sang GUI hoac CLI.',
'listener.onUserListUpdated(users);':'Chuyen ban chup danh sach thanh vien sang giao dien.',
'listener.onStatusChanged(status, isConnected);':'Chuyen trang thai ket noi sang giao dien.',
'listener.onError(error);':'Chuyen thong bao loi sang giao dien.',
'Set<String> users = Collections.unmodifiableSet(new TreeSet<>(activeUsers.keySet()));':'Tao danh sach sap xep, khong cho listener sua truc tiep.',
'ByteArrayOutputStream baos = new ByteArrayOutputStream();':'Tao bo dem byte trong bo nho.',
'dos.writeInt(MAGIC_NUMBER);':'Ghi dau nhan MCUP, chiem 4 byte.',
'dos.writeByte(type.getCode());':'Ghi ma loai thong diep, chiem 1 byte.',
'dos.writeUTF(senderId);':'Ghi ma phien theo modified UTF-8 cua Java.',
'dos.writeUTF(senderName);':'Ghi ten nguoi gui.',
'dos.writeUTF(content);':'Ghi noi dung tin nhan.',
'dos.writeUTF(targetUser);':'Ghi ten nhan rieng; rong voi tin chung.',
'dos.writeLong(timestamp);':'Ghi thoi diem tao tin, chiem 8 byte.',
'dos.flush();':'Day du lieu con lai vao bo dem byte.',
'return baos.toByteArray();':'Lay mang byte de tao DatagramPacket.',
'if (data == null || length <= 0) {':'Bo qua goi khong co du lieu.',
'ByteArrayInputStream bais = new ByteArrayInputStream(data, offset, length);':'Chi doc dung vung du lieu cua goi UDP.',
'DataInputStream dis = new DataInputStream(bais);':'Doc truong nhi phan theo thu tu ben gui.',
'if (length >= 4) {':'Can it nhat 4 byte de doc dau nhan.',
'int magic = dis.readInt();':'Doc 4 byte dau de nhan dien dinh dang.',
'if (magic == MAGIC_NUMBER) {':'Dau nhan trung khop thi doc cac truong MCUP.',
'byte typeCode = dis.readByte();':'Doc ma loai thong diep.',
'MessageType type = MessageType.fromCode(typeCode);':'Doi ma byte thanh enum cua chuong trinh.',
'String senderId = dis.readUTF();':'Doc ma phien, dung thu tu da ghi.',
'String senderName = dis.readUTF();':'Doc ten nguoi gui.',
'String content = dis.readUTF();':'Doc noi dung tin nhan.',
'String targetUser = dis.readUTF();':'Doc ten nguoi nhan rieng.',
'long timestamp = dis.readLong();':'Doc thoi diem ben gui tao tin.',
'String rawText = new String(data, offset, length, StandardCharsets.UTF_8).trim();':'Thu doc UTF-8 thuan neu goi khong dung dinh dang MCUP.',
'for (MessageType t : values()) {':'Duyet cac loai thong diep da khai bao.',
'if (t.code == code) return t;':'Tim enum co ma trung voi byte nhan duoc.',
'return CHAT;':'Mac dinh la CHAT neu khong nhan ra ma.',
'BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));':'Doc van ban nguoi dung go vao console.',
'while ((line = reader.readLine()) != null) {':'Doc tung dong den khi dau vao dong.',
'if (line.equalsIgnoreCase("exit") || line.equalsIgnoreCase("quit")) {':'Hai lenh nay ket thuc vong doc ban phim.',
'if (line.equalsIgnoreCase("/users")) {':'Hien moi in tieu de, chua in danh sach thanh vien.',
'if (line.startsWith("/w ") || line.startsWith("/whisper ")) {':'Nhan dien lenh gui tin rieng.',
'if (parts.length >= 3) {':'Chi gui khi co du ten va noi dung.',
}

originals = {}
counts = {}
for path in sorted(root.glob('*.java')):
    original = path.read_text(encoding='utf-8')
    originals[path.name] = original
    source = lexer.sub(lambda m: ascii_text(m.group()) if m.group().startswith(('//','/*')) else m.group(), original)
    lines = []
    block = False
    count = 0
    for line in source.splitlines():
        t = line.strip()
        indent = line[:len(line)-len(line.lstrip())]
        if block or t.startswith(('//','/*','*')):
            lines.append(line)
            if '/*' in t and '*/' not in t: block = True
            if '*/' in t: block = False
            continue
        comment = None
        declaration = re.match(r'(?:(?:public|private|protected|static|final|synchronized|abstract|@Override)\s+)*(?:[\w<>\[\],?]+\s+)?(\w+)\s*\(', t)
        if declaration and (t.startswith(('public ', 'private ', 'protected ', '@Override ', 'Recipient(', 'RoundedPanel(', 'Transcript(', 'SystemRow(', 'MessageRow(', 'MessageType('))):
            name = declaration.group(1)
            comment = methods.get(name)
            if name == 'toString':
                comment = 'Tra chuoi hien ten card mang trong hop chon.' if path.stem == 'NetworkInterfaceInfo' else 'Hien Ca nhom hoac Rieng: ten trong hop chon.'
        if path.stem == 'ChatListener' and t.startswith('void '):
            comment = methods.get(t.split('(')[0].split()[-1])
        if not comment and re.match(r'(?:private|public|protected)\s', t) and '(' not in t.split('=')[0]:
            match = re.search(r'\b(\w+)\s*(?:=[^;]*)?;', t)
            if match: comment = fields.get(match.group(1))
        if not comment: comment = exact.get(t)
        if not comment:
            if '.addActionListener(' in t:
                target = re.search(r'->\s*(\w+)\(', t)
                comment = 'Bam nut thi goi ' + target.group(1) + '().' if target else 'Dang ky viec can lam khi bam nut.'
            elif '.addListSelectionListener(' in t: comment = 'Cap nhat nut nhan rieng khi doi ten dang chon.'
            elif '.addMouseListener(' in t: comment = 'Theo doi thao tac chuot tren danh sach.'
            elif '.getInputMap().put(' in t: comment = 'Gan phim tat: Enter gui, Shift + Enter xuong dong.'
            elif '.getActionMap().put(' in t: comment = 'Gan hanh dong phim tat voi xu ly gui tin.'
            elif '.addDocumentListener(' in t: comment = 'Theo doi noi dung de bat/tat nut gui.'
            elif '.getAccessibleContext().setAccessibleName(' in t: comment = 'Dat ten cho cong cu ho tro doc man hinh.'
            elif '.putClientProperty("html.disable"' in t: comment = 'Hien chu nguyen van, khong dien giai HTML.'
            elif '.setToolTipText(' in t: comment = 'Hien goi y khi re chuot len thanh phan.'
            elif '.revalidate();' in t: comment = 'Tinh lai kich thuoc va bo cuc.'
            elif '.repaint();' in t: comment = 'Ve lai phan giao dien vua thay doi.'
            elif '.setOpaque(false)' in t: comment = 'De nen trong suot cho khung cha hoac nen tu ve.'
            elif '.setBorder(' in t: comment = 'Dat vien hoac khoang dem cho thanh phan.'
            elif '.setPreferredSize(' in t: comment = 'De xuat kich thuoc cho bo sap xep giao dien.'
            elif '.setMinimumSize(' in t: comment = 'Gioi han kich thuoc nho nhat.'
            elif '.setFont(' in t: comment = 'Dat kieu chu va co chu.'
            elif '.setForeground(' in t: comment = 'Dat mau chu hoac mau noi dung.'
            elif '.setBackground(' in t: comment = 'Dat mau nen cho thanh phan.'
            elif '.setEnabled(' in t: comment = 'Bat hoac khoa thao tac theo dieu kien.'
            elif '.setLayout(' in t: comment = 'Chon cach sap xep cac thanh phan con.'
            elif 'g.fillRoundRect(' in t or 'g2.fillRoundRect(' in t: comment = 'Ve nen hinh chu nhat bo goc.'
            elif '.setRenderingHint(' in t: comment = 'Lam min duong ve hoac chu.'
        if comment:
            if lines and lines[-1].strip() == '@Override':
                annotation = lines.pop()
                lines.extend([indent+'// '+comment, annotation])
            else: lines.append(indent+'// '+comment)
            count += 1
        lines.append(line)
    result = '\n'.join(lines)+'\n'
    assert code_only(original) == code_only(result), path.name + ': changed code'
    for m in lexer.finditer(result):
        if m.group().startswith(('//','/*')): assert m.group().isascii(), path.name
    path.write_text(result, encoding='utf-8')
    counts[path.name] = count
Path('build/comment-check/originals.json').write_text(json.dumps(originals, ensure_ascii=False), encoding='utf-8')
print(json.dumps(counts, indent=2))
print('PASS: executable code unchanged; all comments are ASCII')
