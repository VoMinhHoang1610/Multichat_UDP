package multichat_udp.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import multichat_udp.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Servlet xu ly tai len File va Folder, sau do phat thong diep chia se qua UDP Multicast.
 */
@WebServlet(name = "FileUploadServlet", urlPatterns = {"/api/upload"})
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024 * 2,   // 2MB dem RAM
        maxFileSize = 1024 * 1024 * 100,      // 100MB cho moi file
        maxRequestSize = 1024 * 1024 * 200    // 200MB cho ca request
)
public class FileUploadServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            out.print("{\"success\":false,\"message\":\"Chưa đăng nhập phòng chat.\"}");
            return;
        }

        UserProfile user = (UserProfile) session.getAttribute("user");
        String roomCode = (String) session.getAttribute("roomCode");
        boolean isAdmin = Boolean.TRUE.equals(session.getAttribute("isAdmin"));
        String role = (session.getAttribute("role") != null) ? (String) session.getAttribute("role") : user.getRole();

        ChatRoom room = RoomManager.getInstance().getRoom(roomCode);
        if (room == null) {
            out.print("{\"success\":false,\"message\":\"Phòng chat không tồn tại.\"}");
            return;
        }

        // Kiem tra phong tam khoa
        if ("LOCKED".equalsIgnoreCase(room.getStatus()) && !isAdmin) {
            out.print("{\"success\":false,\"message\":\"Phòng chat hiện đang bị tạm khóa bởi Admin.\"}");
            return;
        }

        // Kiem tra nguoi dung bi tat tieng
        UserProfile roomUser = room.getUser(user.getUserId());
        if (roomUser != null && roomUser.isMuted()) {
            out.print("{\"success\":false,\"message\":\"Bạn đã bị Admin tắt tiếng trong phòng này!\"}");
            return;
        }

        String uploadType = req.getParameter("uploadType"); // "file" hoac "folder"

        String realPath = getServletContext().getRealPath("/");
        if (realPath == null) {
            realPath = new File("web").getAbsolutePath();
        }

        if ("folder".equalsIgnoreCase(uploadType)) {
            // Xu ly tai len thu muc: gom cac file duoc chon va dong goi thanh file ZIP
            String folderName = req.getParameter("folderName");
            if (folderName == null || folderName.trim().isEmpty()) {
                folderName = "Folder_" + System.currentTimeMillis();
            }

            Collection<Part> parts = req.getParts();
            List<Part> fileParts = new ArrayList<>();
            for (Part p : parts) {
                if (p.getName().equals("files") && p.getSize() > 0) {
                    fileParts.add(p);
                }
            }

            if (fileParts.isEmpty()) {
                out.print("{\"success\":false,\"message\":\"Không tìm thấy tệp nào trong thư mục.\"}");
                return;
            }

            File folderDir = new File(realPath, "uploads" + File.separator + "folders");
            if (!folderDir.exists()) folderDir.mkdirs();

            String zipFileName = sanitizeFileName(folderName) + "_" + System.currentTimeMillis() + ".zip";
            File zipFile = new File(folderDir, zipFileName);

            long totalBytes = 0;
            try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
                for (Part p : fileParts) {
                    String submittedName = p.getSubmittedFileName();
                    if (submittedName == null || submittedName.isEmpty()) continue;

                    ZipEntry ze = new ZipEntry(submittedName);
                    zos.putNextEntry(ze);

                    try (InputStream is = p.getInputStream()) {
                        byte[] buffer = new byte[8192];
                        int len;
                        while ((len = is.read(buffer)) > 0) {
                            zos.write(buffer, 0, len);
                            totalBytes += len;
                        }
                    }
                    zos.closeEntry();
                }
            }

            String zipUrl = "uploads/folders/" + zipFileName;
            ChatMessage folderMsg = ChatMessage.createFolder(
                    user.getUserId(),
                    user.getUsername(),
                    user.getAvatarUrl(),
                    room.getRoomCode(),
                    folderName,
                    fileParts.size(),
                    zipFile.length(),
                    zipUrl,
                    role
            );

            // Phat qua UDP Multicast
            try {
                room.broadcastUDP(folderMsg);
                user.incrementMessageCount();
                user.touch();

                out.print("{\"success\":true,\"message\":\"Đã gửi thư mục thành công!\",\"fileUrl\":\"" + zipUrl + "\"}");
            } catch (Exception e) {
                out.print("{\"success\":false,\"message\":\"Lỗi phát UDP: " + escapeJson(e.getMessage()) + "\"}");
            }

        } else {
            // Xu ly tai len mot hoac nhieu file
            Part filePart = req.getPart("file");
            if (filePart == null || filePart.getSize() <= 0) {
                out.print("{\"success\":false,\"message\":\"Vui lòng chọn tệp để gửi.\"}");
                return;
            }

            String originalName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();
            String ext = "";
            int dotIdx = originalName.lastIndexOf('.');
            if (dotIdx >= 0) {
                ext = originalName.substring(dotIdx);
            }

            File fileDir = new File(realPath, "uploads" + File.separator + "files");
            if (!fileDir.exists()) fileDir.mkdirs();

            String uniqueName = UUID.randomUUID().toString() + "_" + sanitizeFileName(originalName);
            File savedFile = new File(fileDir, uniqueName);

            try (InputStream input = filePart.getInputStream()) {
                Files.copy(input, savedFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }

            String fileUrl = "uploads/files/" + uniqueName;
            ChatMessage fileMsg = ChatMessage.createFile(
                    user.getUserId(),
                    user.getUsername(),
                    user.getAvatarUrl(),
                    room.getRoomCode(),
                    originalName,
                    savedFile.length(),
                    fileUrl,
                    role
            );

            // Phat qua UDP Multicast
            try {
                room.broadcastUDP(fileMsg);
                user.incrementMessageCount();
                user.touch();

                out.print("{\"success\":true,\"message\":\"Đã gửi tệp thành công!\",\"fileUrl\":\"" + fileUrl + "\",\"fileName\":\"" + escapeJson(originalName) + "\"}");
            } catch (Exception e) {
                out.print("{\"success\":false,\"message\":\"Lỗi phát UDP: " + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    private String sanitizeFileName(String name) {
        if (name == null) return "file";
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
