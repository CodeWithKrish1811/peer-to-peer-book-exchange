package com.bookexchange.servlet;

import static com.bookexchange.util.HtmlUtil.esc;

import com.bookexchange.exception.AppException;
import com.bookexchange.model.Book;
import com.bookexchange.model.Student;
import com.bookexchange.service.BookService;
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

/** Student dashboard: every number comes from MySQL. */
@WebServlet("/dashboard")
public class DashboardServlet extends HttpServlet {
    private final StudentService studentService = new StudentService();
    private final BookService bookService = new BookService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String ctx = req.getContextPath();
        int me = SessionUtil.studentId(req);
        StringBuilder h = new StringBuilder();
        try {
            Student student = studentService.getById(me);
            Map<String, Integer> stats = studentService.dashboardStats(me);
            List<Book> recent = bookService.getRecentBooks(me, 5);

            h.append("<div class=\"page-head\"><div><h1 class=\"page-title\">Welcome back, ")
                    .append(esc(student.getFirstName())).append("</h1><p class=\"muted\">Here is where your exchanges stand today.</p></div>")
                    .append("<a class=\"btn btn-primary\" href=\"").append(ctx).append("/books?action=add\">Add a book</a></div>");

            h.append("<section class=\"stats\" aria-label=\"Summary\">")
                    .append(stat(ctx + "/books", stats.get("totalBooks"), "Books listed"))
                    .append(stat(ctx + "/books", stats.get("availableBooks"), "Available to exchange"))
                    .append(stat(ctx + "/requests?tab=incoming", stats.get("incoming"), "Incoming requests"))
                    .append(stat(ctx + "/requests?tab=outgoing", stats.get("outgoing"), "Outgoing requests"))
                    .append(stat(ctx + "/history?status=completed", stats.get("completed"), "Completed exchanges"))
                    .append("</section>");

            h.append("<section class=\"panel\"><div class=\"panel-head\"><h2>Recently added books</h2>")
                    .append("<a class=\"link\" href=\"").append(ctx).append("/books\">View all</a></div>");
            if (recent.isEmpty()) {
                h.append(HtmlUtil.emptyState("No books listed yet",
                        "Add the textbooks you no longer need so classmates can find them.",
                        ctx + "/books?action=add", "Add your first book"));
            } else {
                h.append(HtmlUtil.bookTable(recent));
            }
            h.append("</section>");
        } catch (AppException e) {
            SessionUtil.flash(req, "error", e.getMessage());
            h.append("<h1 class=\"page-title\">Dashboard</h1>");
        }
        HtmlUtil.render(req, resp, "Dashboard", "dashboard", h.toString());
    }

    private String stat(String href, Integer value, String label) {
        return "<a class=\"stat\" href=\"" + href + "\"><span class=\"stat-value\">" + value
                + "</span><span class=\"stat-label\">" + esc(label) + "</span></a>";
    }
}
