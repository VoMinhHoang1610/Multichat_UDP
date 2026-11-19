package multichat_udp.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import multichat_udp.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Servlet quan ly cap nhat thong tin ca nhan va thay doi avatar.
 */
@WebServlet(name = "ProfileServlet", urlPatterns = {"/api/profile"})
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024,     // 1MB
        maxFileSize = 1024 * 1024 * 10,     // 10MB
        maxRequestSize = 1024 * 1024 * 15   // 15MB
)
public class ProfileServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            out.print("{\"success\":false,\"message\":\"Chưa đăng nhập.\"}");
            return;
        }

        UserProfile user = (UserProfile) session.getAttribute("user");
        out.print("{\"success\":true,\"user\":" + user.toJson() + "}");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            out.print("{\"success\":false,\"message\":\"Phiên đăng nhập đã kết thúc.\"}");
            return;
        }

        UserProfile user = (UserProfile) session.getAttribute("user");
        String roomCode = (String) session.getAttribute("roomCode");
        ChatRoom room = RoomManager.getInstance().getRoom(roomCode);

        String username = req.getParameter("username");
        String bio = req.getParameter("bio");
        String email = req.getParameter("email");
        String avatarPreset = req.getParameter("avatarPreset");

        if (username != null && !username.trim().isEmpty()) {
            user.setUsername(username.trim());
        }
        if (bio != null) {
            user.setBio(bio.trim());
        }
        if (email != null) {
            user.setEmail(email.trim());
        }

        // Kiem tra nguoi dung co tai len avatar tuy chinh khong
        Part avatarPart = null;
        try {
            avatarPart = req.getPart("avatarFile");
        } catch (Exception ignored) {
        }

        if (avatarPart != null && avatarPart.getSize() > 0) {
            String originalName = Paths.get(avatarPart.getSubmittedFileName()).getFileName().toString();
            String ext = "";
            int dot = originalName.lastIndexOf('.');
            if (dot >= 0) ext = originalName.substring(dot);

            String realPath = getServletContext().getRealPath("/");
            if (realPath == null) realPath = new File("web").getAbsolutePath();

            File avatarDir = new File(realPath, "uploads" + File.separator + "avatars");
            if (!avatarDir.exists()) avatarDir.mkdirs();

            String uniqueName = UUID.randomUUID().toString() + ext;
            File savedAvatar = new File(avatarDir, uniqueName);

            try (InputStream in = avatarPart.getInputStream()) {
                Files.copy(in, savedAvatar.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            user.setAvatarUrl("uploads/avatars/" + uniqueName);

        } else if (avatarPreset != null && !avatarPreset.trim().isEmpty()) {
            user.setAvatarUrl(avatarPreset.trim());
        }

        user.touch();

        // Dong bo voi Account trong AccountService neu co
        Account account = (Account) session.getAttribute("account");
        if (account != null) {
            account.setFullName(user.getUsername());
            account.setBio(user.getBio());
            account.setEmail(user.getEmail());
            account.setAvatarUrl(user.getAvatarUrl());
            AccountService.getInstance().updateAccount(account);
        }

        // Cap nhat trong phong neu co
        if (room != null) {
            UserProfile roomUser = room.getUser(user.getUserId());
            if (roomUser != null) {
                roomUser.setUsername(user.getUsername());
                roomUser.setBio(user.getBio());
                roomUser.setEmail(user.getEmail());
                roomUser.setAvatarUrl(user.getAvatarUrl());
                roomUser.touch();
            }

            // Phat thong bao cap nhat ho so qua UDP Multicast
            try {
                ChatMessage updateMsg = ChatMessage.createProfileUpdate(
                        user.getUserId(),
                        user.getUsername(),
                        user.getAvatarUrl(),
                        room.getRoomCode(),
                        user.getBio()
                );
                room.broadcastUDP(updateMsg);
            } catch (Exception ignored) {
            }
        }

        out.print("{\"success\":true,\"message\":\"Cập nhật thông tin cá nhân thành công!\",\"user\":" + user.toJson() + "}");
    }
}
