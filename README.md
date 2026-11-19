# HỆ THỐNG MULTICHAT UDP ENTERPRISE (JSP & UDP MULTICAST)

Ứng dụng chat nhóm thời gian thực kết hợp giữa **Lập trình mạng UDP Multicast Socket** và **Giao diện Web JSP chuẩn shadcn/ui & Modern SaaS**, được thiết kế theo phong cách **Modern Floating Glassmorphism** (tương tự Linear, Vercel, Slack).

---

## 🌟 1. Các Tính Năng Nổi Bật & Giao Diện Hiện Đại

### 1.1. Thiết Kế Chuẩn shadcn/ui & Modern SaaS Glassmorphism
* **Khung thẻ kính nổi (Floating Glass Card)**: Loại bỏ triệt để hiện tượng vỡ layout full-width, áp dụng `max-width` chuẩn mực (`440px` cho Auth card, `1040px` cho Lobby, `1140px` cho Admin Dashboard).
* **Hiệu ứng Ambient Glow & Chiều sâu đa tầng**: Nền tỏa sáng quang phổ (`radial-gradient ambient orbs`) kết hợp lớp phủ kính mờ `backdrop-filter: blur(20px)` và viền siêu mảnh tinh tế.
* **Hệ thống Font & Biểu tượng**: Phông chữ cao cấp **Plus Jakarta Sans** cùng bộ biểu tượng **100% Vector SVG (Lucide Icons)** sắc nét chuẩn tỉ lệ.
* **Hỗ trợ Light / Dark Mode mượt mà**: Tự động đồng bộ và lưu trạng thái vào `localStorage`.
* **Cơ chế chống Cache CSS trình duyệt (Cache-busting)**: Mọi trang JSP đều được gắn tham số phiên bản động `?v=3.0.<%= System.currentTimeMillis() %>` để trình duyệt luôn cập nhật giao diện mới nhất.

### 1.2. Hệ thống Phân quyền Doanh nghiệp RBAC (Role-Based Access Control)
* **Trang Xác thực Tập trung ([`login.jsp`](file:///e:/4-LapTrinhMang_BaiTap/Multichat_UDP/web/login.jsp))**:
  * Thẻ kính nổi trung tâm với thanh chuyển Tab phân đoạn (Segmented Pill Tab Bar).
  * Các trường nhập liệu có icon định danh dẫn đầu (Leading SVG icons).
  * **Thẻ tài khoản mẫu thông minh**: Nhấp chuột để tự động điền tài khoản mẫu với hiệu ứng viền chớp sáng (flash confirmation) trực quan:
    * 🛡 **Quản Trị Viên (ADMIN)**: `admin` / `admin123` (Huy hiệu vương miện mạ vàng hổ phách)
    * 👤 **Thành Viên 1 (MEMBER)**: `user1` / `123456`
    * 👤 **Thành Viên 2 (MEMBER)**: `user2` / `123456`
* **Kiểm soát Truy cập & Bảo vệ 403 Forbidden**:
  * **Sảnh Chờ ([`index.jsp`](file:///e:/4-LapTrinhMang_BaiTap/Multichat_UDP/web/index.jsp))**: Tự động nhận diện `Session`, hiển thị avatar có chấm xanh online nhấp nháy (`pulse-dot`), huy hiệu vai trò `👑 Quản Trị Viên` hoặc `👤 Thành Viên`.
  * **Thành Viên (`MEMBER`)**: Tham gia phòng chat nhanh bằng mã hoặc nhấp vào thẻ phòng. Tab tạo phòng mới được ẩn an toàn.
  * **Quản Trị Viên (`ADMIN`)**: Được cấp quyền mở tab **Khởi Tạo Phòng Mới** và nút truy cập **Dashboard Admin**.
  * **Màn hình 403 Forbidden**: Chặn đứng mọi nỗ lực truy cập trái phép vào trang quản trị với giao diện kính cảnh báo sang trọng.

### 1.3. Phòng Chat Trực Tiếp Slack/Linear-style ([`chat.jsp`](file:///e:/4-LapTrinhMang_BaiTap/Multichat_UDP/web/chat.jsp))
* **Sidebar Thành Viên Chia 2 Nhóm Rõ Ràng**: Tách biệt thành viên thành 2 nhóm: **👑 Quản Trị Viên (Admins)** và **👤 Thành Viên (Members)** với số lượng đếm tự động, avatar tròn nét và chấm xanh online.
* **Luồng Tin Nhắn Đẳng Cấp**:
  * Tin nhắn của mình căn phải với bong bóng gradient tím xanh Electric Indigo.
  * Tin nhắn của Admin có huy hiệu vương miện nổi bật `👑 Admin` và tên màu vàng hổ phách.
  * Tin nhắn riêng tư (Whisper) mang màu tím lavender kèm biểu tượng ổ khóa bảo mật.
* **Thanh Soạn Thảo Nổi (Floating Input Dock)**:
  * Dock nổi bo tròn 22px với bóng đổ cao cấp ở đáy màn hình.
  * Ô nhập văn bản tự động co giãn dòng mượt mà khi nhấn Shift+Enter.
  * Nút đính kèm File, Folder (ZIP tự động), Sticker Picker và Emoji Picker.

### 1.4. Bảng Điều Khiển Quản Trị & Báo Cáo Doanh Nghiệp ([`admin.jsp`](file:///e:/4-LapTrinhMang_BaiTap/Multichat_UDP/web/admin.jsp))
* **4 Thẻ Thống Kê KPI Hiện Đại**: Mã phòng `#LTM2026`, Socket UDP `224.2.2.3:8888`, Số người online, Trạng thái Socket.
* **Xuất file Báo cáo**:
  * **Excel CSV (UTF-8 BOM)**: Tải về file `.csv` không lỗi font tiếng Việt trên Microsoft Excel.
  * **Bản In / Lưu PDF**: Bố cục trang in chuẩn mực trường học / cơ quan.
* **Thao tác Quản trị**: Mời thành viên ra khỏi phòng (Kick), Tắt tiếng (Mute), Xóa lịch sử chat, Tạm khóa phòng chat.

### 1.5. Quản lý thông tin cá nhân & Đổi Avatar
* Cập nhật **Tên hiển thị**, **Tiểu sử (Bio / Status)**, **Email liên hệ**.
* **Đổi Avatar**: Chọn từ bộ sưu tập 8 Avatar vector SVG sắc nét hoặc **tải lên ảnh avatar cá nhân từ máy tính**.
* Thông tin hồ sơ được tự động đồng bộ hóa xuống tài khoản và phát qua UDP tới các thành viên khác.

### 1.6. Trải nghiệm Chat đa phương tiện phong phú
* **Nhắn tin văn bản**: Hỗ trợ xuống dòng (Shift+Enter), tin nhắn nhóm và **nhắn riêng tư (Whisper)**.
* **Gửi File đính kèm**: Gửi mọi loại tệp (tài liệu, PDF, code, nén), hỗ trợ xem trước ảnh với cửa sổ phóng to (Lightbox).
* **Gửi cả Thư mục (Folder Upload)**: Tải lên cả thư mục máy tính, hệ thống tự động gom tệp và đóng gói thành file ZIP để các thành viên khác tải về tiện lợi.
* **Gửi Link với thẻ xem trước (Link Preview Card)**: Tự động nhận diện đường link URL, hiển thị thẻ thông tin với tên miền và tiêu đề nổi bật.
* **Gửi Sticker & Emoji**:
  * Bộ sưu tập Sticker vector: Mèo đáng yêu, Dev & Tech (Bug, Coffee, Rocket), Biểu tượng cảm xúc (Like, Heart, Fire).
  * Bảng chọn Emoji đầy đủ danh mục.
* **Âm thanh thông báo**: Tự động phát âm thanh chuông nhẹ nhàng khi có tin nhắn mới (sử dụng Web Audio API).

---

## 🛠 2. Kiến Trúc Mạng & Công Nghệ Áp Dụng

1. **Giao thức mạng cốt lõi (Giữ trọn vẹn UDP Multicast)**:
   * Địa chỉ IP Lớp D: `224.0.0.0` đến `239.255.255.255`.
   * Lớp `MulticastSocket`, `DatagramPacket`, `DatagramSocket`.
   * Giao thức đóng gói nhị phân **MCUP (`0x4D435550`)** với đầy đủ các loại thông điệp: `JOIN`, `LEAVE`, `CHAT`, `HEARTBEAT`, `WHISPER`, `FILE`, `FOLDER`, `LINK`, `STICKER`, `PROFILE_UPDATE`, `SYSTEM`.
   * Đọc/ghi luồng nhị phân bằng `DataInputStream` / `DataOutputStream` và `ByteArrayInputStream` / `ByteArrayOutputStream`.
2. **Ngôn ngữ & Nền tảng Web**:
   * **JSP (JavaServer Pages)** kết hợp **Java Servlets (Jakarta EE)**.
   * Máy chủ Web Apache Tomcat 10.1 được nhúng sẵn trong thư mục `tomcat/`, không yêu cầu người dùng phải cài đặt thêm Tomcat bên ngoài.
   * Giao diện theo triết lý **UI/UX Pro Max**: Swiss Minimalism, responsive trên mọi kích thước màn hình, hỗ trợ chế độ Sáng / Tối (Dark Mode).

---

## 🚀 3. Hướng Dẫn Chạy Chương Trình

### Cách 1: Khởi chạy Giao diện Web JSP (Khuyên dùng)
1. Nhấp đúp vào file [`run_web.bat`](file:///e:/4-LapTrinhMang_BaiTap/Multichat_UDP/run_web.bat).
2. Chương trình sẽ tự động kiểm tra biên dịch Java và khởi động máy chủ Web Tomcat.
3. Trình duyệt tự động mở tại địa chỉ:
   ```
   http://localhost:8088/
   ```
4. Đăng nhập với tài khoản Admin (`admin` / `admin123`) hoặc Thành viên (`user1` / `123456`) để trải nghiệm!
5. Bạn có thể mở nhiều tab hoặc trình duyệt khác nhau (hoặc các máy khác trong cùng mạng LAN truy cập IP máy chủ) để chat thử với nhau qua UDP Multicast!
6. Để dừng máy chủ Web, chỉ cần chạy file [`stop_web.bat`](file:///e:/4-LapTrinhMang_BaiTap/Multichat_UDP/stop_web.bat).

### Cách 2: Tự biên dịch lại mã nguồn khi có chỉnh sửa
* Chạy file [`build_web.bat`](file:///e:/4-LapTrinhMang_BaiTap/Multichat_UDP/build_web.bat) để biên dịch toàn bộ mã Java vào thư mục `web/WEB-INF/classes`.

### Cách 3: Mở trong IDE (NetBeans / Eclipse / IntelliJ)
* Nhập dự án dưới dạng Web Application hoặc Java Project, thêm các thư viện trong `tomcat/lib/` vào classpath.

---

## 📁 4. Cấu Trúc Thư Mục Dự Án

```
Multichat_UDP/
├── src/multichat_udp/             # Mã nguồn Java
│   ├── Account.java               # Đối tượng tài khoản người dùng & hash SHA-256
│   ├── AccountService.java        # Dịch vụ quản lý & lưu trữ tài khoản (JSON)
│   ├── ChatMessage.java           # Định dạng gói tin MCUP và serialize byte
│   ├── MessageType.java           # Enum loại thông điệp UDP
│   ├── UserProfile.java           # Mô hình thông tin người dùng và avatar
│   ├── ChatRoom.java              # Mô hình phòng chat và bộ đệm tin nhắn
│   ├── RoomManager.java           # Quản lý tập trung các phòng chat
│   ├── UDPMulticastEngine.java    # Socket UDP Multicast phát và nhận gói tin
│   ├── ReportService.java         # Dịch vụ xuất báo cáo CSV và HTML
│   └── servlet/                   # Các Servlet xử lý API Web & JSP
│       ├── AuthServlet.java       # Đăng nhập, đăng ký, đăng xuất (RBAC)
│       ├── RoomServlet.java       # Tạo phòng (Admin), vào phòng, danh sách phòng
│       ├── ChatApiServlet.java    # Nhận và phát tin nhắn qua UDP
│       ├── FileUploadServlet.java # Tải lên file và thư mục
│       ├── ProfileServlet.java    # Cập nhật hồ sơ và avatar
│       └── AdminServlet.java      # Bảng quản trị và xuất file báo cáo
├── web/                           # Thư mục Web JSP & Frontend
│   ├── login.jsp                  # Giao diện Đăng nhập & Đăng ký tài khoản (RBAC)
│   ├── index.jsp                  # Sảnh chờ phòng chat & tạo phòng (Admin)
│   ├── chat.jsp                   # Giao diện phòng chat UDP chính
│   ├── admin.jsp                  # Bảng quản trị & xuất báo cáo (Chặn 403)
│   ├── css/style.css              # Hệ thống phong cách UI/UX Pro Max
│   ├── js/chat.js                 # Bộ xử lý thời gian thực, sticker, dropzone
│   ├── assets/                    # Vector SVG Avatars và Stickers
│   └── WEB-INF/                   # Cấu hình web.xml và classes đã biên dịch
├── tomcat/                        # Máy chủ Apache Tomcat 10 tích hợp sẵn
├── run_web.bat                    # File chạy ứng dụng Web JSP một chạm
├── stop_web.bat                   # File dừng máy chủ Tomcat
├── build_web.bat                  # File biên dịch mã nguồn Java
└── README.md                      # Tài liệu hướng dẫn chi tiết
```
