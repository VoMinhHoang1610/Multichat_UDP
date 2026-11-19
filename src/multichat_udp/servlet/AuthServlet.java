package multichat_udp.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import multichat_udp.*;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * Servlet điều phối Xác thực (Authentication) & Quản lý Phiên làm việc (Session):
 * - Đăng nhập tài khoản (action=login)
 * - Đăng ký thành viên mới (action=register)
 * - Đăng xuất và dọn dẹp kết nối socket (action=logout)
 * - Kiểm tra phiên hiện hành (action=me)
 */
@WebServlet(name = "AuthServlet", urlPatterns = {"/api/auth"})
public class AuthServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        String action = req.getParameter("action");
        if (action == null) action = "me";

        HttpSession session = req.getSession(false);

        if ("me".equalsIgnoreCase(action)) {
            if (session != null && session.getAttribute("account") != null) {
                Account acc = (Account) session.getAttribute("account");
                out.print("{\"success\":true,\"role\":\"" + acc.getRole() + "\",\"isAdmin\":" + acc.isAdmin() + ",\"account\":" + acc.toJson() + "}");
            } else {
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                out.print("{\"success\":false,\"message\":\"Chưa đăng nhập.\"}");
            }
        } else if ("check".equalsIgnoreCase(action)) {
            String username = req.getParameter("username");
            boolean taken = AccountService.getInstance().isUsernameTaken(username);
            out.print("{\"success\":true,\"available\":" + (!taken) + "}");
        } else if ("logout".equalsIgnoreCase(action)) {
            handleLogout(req, resp, out);
        } else {
            out.print("{\"success\":false,\"message\":\"Hành động không hợp lệ.\"}");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        String action = req.getParameter("action");
        if (action == null) action = "login";

        AccountService accountService = AccountService.getInstance();

        // ==========================================
        // 1. DANG NHAP HE THONG
        // ==========================================
        if ("login".equalsIgnoreCase(action)) {
            String username = req.getParameter("username");
            String password = req.getParameter("password");

            if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
                out.print("{\"success\":false,\"message\":\"Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu!\"}");
                return;
            }

            Account account = accountService.authenticate(username, password);
            if (account == null) {
                Account existing = accountService.getAccount(username);
                if (existing != null && !existing.isActive()) {
                    out.print("{\"success\":false,\"message\":\"Tài khoản của bạn hiện đang bị khóa hoặc vô hiệu hóa!\"}");
                } else {
                    out.print("{\"success\":false,\"message\":\"Tên đăng nhập hoặc mật khẩu không chính xác!\"}");
                }
                return;
            }

            // Khoi tao hoac lay session
            HttpSession session = req.getSession(true);
            session.setAttribute("account", account);
            session.setAttribute("role", account.getRole());
            session.setAttribute("isAdmin", account.isAdmin());

            // Dong bo thong tin UserProfile cho chat socket
            UserProfile userProfile = new UserProfile(
                    account.getUsername(),
                    account.getFullName(),
                    account.getAvatarUrl(),
                    account.getRole(),
                    req.getRemoteAddr()
            );
            userProfile.setBio(account.getBio());
            userProfile.setEmail(account.getEmail());
            session.setAttribute("user", userProfile);

            out.print("{\"success\":true,\"message\":\"Đăng nhập thành công!\",\"role\":\"" + account.getRole()
                    + "\",\"isAdmin\":" + account.isAdmin()
                    + ",\"account\":" + account.toJson() + "}");

        // ==========================================
        // 2. DANG KY TAI KHOAN MOI (MEMBER)
        // ==========================================
        } else if ("register".equalsIgnoreCase(action)) {
            String username = req.getParameter("username");
            String password = req.getParameter("password");
            String fullName = req.getParameter("fullName");
            String email = req.getParameter("email");
            String avatarUrl = req.getParameter("avatarUrl");

            try {
                Account newAcc = accountService.register(username, password, fullName, email, avatarUrl);

                // Tu dong dang nhap sau khi dang ky thanh cong
                HttpSession session = req.getSession(true);
                session.setAttribute("account", newAcc);
                session.setAttribute("role", "MEMBER");
                session.setAttribute("isAdmin", false);

                UserProfile userProfile = new UserProfile(
                        newAcc.getUsername(),
                        newAcc.getFullName(),
                        newAcc.getAvatarUrl(),
                        "MEMBER",
                        req.getRemoteAddr()
                );
                userProfile.setBio(newAcc.getBio());
                userProfile.setEmail(newAcc.getEmail());
                session.setAttribute("user", userProfile);

                out.print("{\"success\":true,\"message\":\"Đăng ký tài khoản thành công!\",\"account\":" + newAcc.toJson() + "}");
            } catch (IllegalArgumentException e) {
                out.print("{\"success\":false,\"message\":\"" + escapeJson(e.getMessage()) + "\"}");
            } catch (Exception e) {
                out.print("{\"success\":false,\"message\":\"Lỗi máy chủ khi đăng ký tài khoản: " + escapeJson(e.getMessage()) + "\"}");
            }

        // ==========================================
        // 3. DANG XUAT (LOGOUT)
        // ==========================================
        } else if ("logout".equalsIgnoreCase(action)) {
            handleLogout(req, resp, out);
        } else {
            out.print("{\"success\":false,\"message\":\"Hành động không xác định.\"}");
        }
    }

    private void handleLogout(HttpServletRequest req, HttpServletResponse resp, PrintWriter out) throws IOException {
        HttpSession session = req.getSession(false);
        if (session != null) {
            UserProfile user = (UserProfile) session.getAttribute("user");
            String roomCode = (String) session.getAttribute("roomCode");

            if (user != null && roomCode != null) {
                ChatRoom room = RoomManager.getInstance().getRoom(roomCode);
                if (room != null) {
                    room.removeUser(user.getUserId());
                    try {
                        String extra = "{\"role\":\"" + user.getRole() + "\"}";
                        ChatMessage leaveMsg = new ChatMessage(
                                MessageType.LEAVE,
                                user.getUserId(),
                                user.getUsername(),
                                user.getUsername() + " đã đăng xuất và rời phòng.",
                                "",
                                System.currentTimeMillis(),
                                roomCode,
                                user.getAvatarUrl(),
                                extra
                        );
                        room.broadcastUDP(leaveMsg);
                    } catch (Exception ignored) {
                    }
                }
            }
            session.invalidate();
        }

        String accept = req.getHeader("Accept");
        if (accept != null && accept.contains("text/html")) {
            resp.sendRedirect(req.getContextPath() + "/login.jsp");
        } else {
            out.print("{\"success\":true,\"message\":\"Đã đăng xuất thành công.\"}");
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
