package com.bookexchange.servlet;

import static com.bookexchange.util.HtmlUtil.esc;

import com.bookexchange.exception.AppException;
import com.bookexchange.exception.ValidationException;
import com.bookexchange.model.Book;
import com.bookexchange.model.RequestStatus;
import com.bookexchange.model.Student;
import com.bookexchange.service.BookService;
import com.bookexchange.service.ExchangeService;
import com.bookexchange.service.StudentService;
import com.bookexchange.util.HtmlUtil;
import com.bookexchange.util.SessionUtil;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Profile page: details, statistics, listed books, update name/phone, change password. */
@WebServlet("/profile")
public class ProfileServlet extends HttpServlet {
    private final StudentService studentService = new StudentService();
    private final BookService bookService = new BookService();
    private final ExchangeService exchangeService = new ExchangeService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String ctx = req.getContextPath();
        int me = SessionUtil.studentId(req);
        StringBuilder h = new StringBuilder("<div class=\"page-head\"><div><h1 class=\"page-title\">My profile</h1>")
                .append("<p class=\"muted\">Your details and exchange activity.</p></div></div>");
        try {
            Student s = studentService.getById(me);
            Map<RequestStatus, Integer> stats = exchangeService.getStatistics(me);
            List<Book> books = bookService.getMyBooks(me);

            h.append("<section class=\"panel\"><div class=\"profile-top\"><div class=\"avatar\" aria-hidden=\"true\">")
                    .append(esc(s.getFullName().substring(0, 1).toUpperCase())).append("</div><div><h2>").append(esc(s.getFullName()))
                    .append("</h2><p class=\"muted\">").append(esc(s.getEmail())).append(" &middot; Student ID ").append(esc(s.getCollegeId()))
                    .append("</p><p class=\"muted small\">Member since ").append(HtmlUtil.formatDate(s.getCreatedAt())).append("</p></div></div>")
                    .append("<div class=\"stats stats-compact\">")
                    .append(mini(books.size(), "Books listed"));
            for (RequestStatus st : RequestStatus.values()) {
                h.append(mini(stats.get(st), st.getLabel() + " requests"));
            }
            h.append("</div></section>");

            h.append("<div class=\"two-col\"><section class=\"panel\"><div class=\"panel-head\"><h2>Edit details</h2></div>")
                    .append("<form method=\"post\" action=\"").append(ctx).append("/profile\" class=\"stack\">")
                    .append(HtmlUtil.hidden("action", "update"))
                    .append("<div class=\"field\"><label for=\"fullName\">Full name</label><input class=\"input\" id=\"fullName\" name=\"fullName\" required maxlength=\"100\" value=\"")
                    .append(esc(s.getFullName())).append("\"></div><div class=\"field\"><label for=\"phone\">Phone (optional)</label>")
                    .append("<input class=\"input\" id=\"phone\" name=\"phone\" maxlength=\"20\" value=\"").append(esc(s.getPhone()))
                    .append("\"></div><div class=\"form-actions\"><button class=\"btn btn-primary\" type=\"submit\">Save changes</button></div></form></section>")
                    .append("<section class=\"panel\"><div class=\"panel-head\"><h2>Change password</h2></div>")
                    .append("<form method=\"post\" action=\"").append(ctx).append("/profile\" class=\"stack\" data-match=\"newPassword,confirmPassword\">")
                    .append(HtmlUtil.hidden("action", "password"))
                    .append(pw("Current password", "currentPassword")).append(pw("New password", "newPassword"))
                    .append(pw("Confirm new password", "confirmPassword"))
                    .append("<p class=\"hint\">At least 8 characters with a letter and a digit.</p>")
                    .append("<div class=\"form-actions\"><button class=\"btn btn-primary\" type=\"submit\">Update password</button></div></form></section></div>");

            h.append("<section class=\"panel\"><div class=\"panel-head\"><h2>My listed books</h2><a class=\"link\" href=\"")
                    .append(ctx).append("/books\">Manage</a></div>");
            h.append(books.isEmpty() ? "<p class=\"muted\">You have not listed any books yet.</p>" : HtmlUtil.bookTable(books))
                    .append("</section>");
        } catch (AppException e) {
            SessionUtil.flash(req, "error", e.getMessage());
        }
        HtmlUtil.render(req, resp, "Profile", "profile", h.toString());
    }

    private String mini(int value, String label) {
        return "<div class=\"stat stat-static\"><span class=\"stat-value\">" + value + "</span><span class=\"stat-label\">"
                + esc(label) + "</span></div>";
    }

    private String pw(String label, String name) {
        return "<div class=\"field\"><label for=\"" + name + "\">" + esc(label) + "</label><input class=\"input\" type=\"password\" id=\""
                + name + "\" name=\"" + name + "\" required autocomplete=\"" + ("currentPassword".equals(name) ? "current-password" : "new-password") + "\"></div>";
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        int me = SessionUtil.studentId(req);
        String action = req.getParameter("action") == null ? "" : req.getParameter("action");
        try {
            switch (action) {
                case "update":
                    studentService.updateProfile(me, req.getParameter("fullName"), req.getParameter("phone"));
                    SessionUtil.updateName(req, studentService.getById(me).getFullName());
                    SessionUtil.flash(req, "success", "Profile updated.");
                    break;
                case "password":
                    studentService.changePassword(me, req.getParameter("currentPassword"),
                            req.getParameter("newPassword"), req.getParameter("confirmPassword"));
                    SessionUtil.flash(req, "success", "Password changed.");
                    break;
                default:
                    throw new ValidationException("Unknown action.");
            }
        } catch (AppException e) {
            SessionUtil.flash(req, "error", e.getMessage());
        }
        resp.sendRedirect(req.getContextPath() + "/profile");
    }
}
