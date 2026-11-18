package com.bookexchange.servlet;

import static com.bookexchange.util.HtmlUtil.esc;

import com.bookexchange.exception.AppException;
import com.bookexchange.model.Book;
import com.bookexchange.model.BookCondition;
import com.bookexchange.service.BookService;
import com.bookexchange.util.HtmlUtil;
import com.bookexchange.util.SessionUtil;
import java.io.IOException;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Explore Books: search by title/author and filter by subject and condition. */
@WebServlet("/explore")
public class SearchBookServlet extends HttpServlet {
    private final BookService bookService = new BookService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String ctx = req.getContextPath();
        int me = SessionUtil.studentId(req);
        String q = nz(req.getParameter("q"));
        String by = nz(req.getParameter("by"));
        String subject = nz(req.getParameter("subject"));
        String condition = nz(req.getParameter("condition"));

        StringBuilder h = new StringBuilder();
        h.append("<div class=\"page-head\"><div><h1 class=\"page-title\">Explore books</h1>")
                .append("<p class=\"muted\">Find the books you need from other students.</p></div></div>");
        try {
            List<String> subjects = bookService.getSubjects();
            h.append("<form class=\"filters\" method=\"get\" action=\"").append(ctx).append("/explore\">")
                    .append("<input class=\"input search-input\" type=\"search\" name=\"q\" placeholder=\"Search by title or author\" value=\"")
                    .append(esc(q)).append("\" aria-label=\"Search\">")
                    .append("<select class=\"select\" name=\"by\" aria-label=\"Search in\">")
                    .append(option("", "Title and author", by)).append(option("title", "Title only", by))
                    .append(option("author", "Author only", by)).append("</select>")
                    .append("<select class=\"select\" name=\"subject\" aria-label=\"Subject\"><option value=\"\">All subjects</option>");
            for (String s : subjects) {
                h.append(option(s, s, subject));
            }
            h.append("</select><select class=\"select\" name=\"condition\" aria-label=\"Condition\"><option value=\"\">Any condition</option>");
            for (BookCondition c : BookCondition.values()) {
                h.append(option(c.name(), c.getLabel(), condition));
            }
            h.append("</select><button class=\"btn btn-primary\" type=\"submit\">Search</button>")
                    .append("<a class=\"btn btn-ghost\" href=\"").append(ctx).append("/explore\">Clear</a></form>");

            List<Book> books = bookService.search(me, q, by, subject, condition);
            h.append("<p class=\"result-count muted\">").append(books.size()).append(books.size() == 1 ? " book" : " books")
                    .append(" available</p>");
            if (books.isEmpty()) {
                h.append(HtmlUtil.emptyState("No books match your search",
                        "Try a different keyword or clear the filters.", ctx + "/explore", "Show all books"));
            } else {
                h.append("<section class=\"book-grid\">");
                for (Book b : books) {
                    h.append(card(ctx, b));
                }
                h.append("</section>");
            }
        } catch (AppException e) {
            SessionUtil.flash(req, "error", e.getMessage());
        }
        HtmlUtil.render(req, resp, "Explore books", "explore", h.toString());
    }

    private String card(String ctx, Book b) {
        StringBuilder c = new StringBuilder("<article class=\"book-card ").append(HtmlUtil.spineClass(b.getSubject())).append("\">")
                .append("<div class=\"book-card-body\"><div class=\"book-card-top\"><span class=\"subject\">").append(esc(b.getSubject()))
                .append("</span>").append(HtmlUtil.availabilityBadge(b.getAvailability())).append("</div>")
                .append("<h3>").append(esc(b.getTitle())).append("</h3><p class=\"author\">by ").append(esc(b.getAuthor())).append("</p>")
                .append("<dl class=\"meta\"><div><dt>Edition</dt><dd>").append(esc(b.getEdition())).append("</dd></div>")
                .append("<div><dt>Condition</dt><dd>").append(esc(b.getCondition().getLabel())).append("</dd></div></dl>");
        if (b.getDescription() != null || b.getExchangePreference() != null) {
            c.append("<details class=\"more\"><summary>Description</summary>");
            if (b.getDescription() != null) {
                c.append("<p>").append(esc(b.getDescription())).append("</p>");
            }
            if (b.getExchangePreference() != null) {
                c.append("<p><strong>Would like in return:</strong> ").append(esc(b.getExchangePreference())).append("</p>");
            }
            c.append("</details>");
        }
        c.append("<p class=\"owner\">Listed by <strong>").append(esc(b.getOwnerName())).append("</strong> (")
                .append(esc(b.getOwnerCollegeId())).append(")</p></div><div class=\"book-card-foot\">")
                .append("<a class=\"btn btn-primary\" href=\"").append(ctx).append("/requests?action=new&amp;bookId=")
                .append(b.getId()).append("\">Request exchange</a></div></article>");
        return c.toString();
    }

    private String option(String value, String label, String current) {
        return "<option value=\"" + esc(value) + "\"" + (value.equals(current) ? " selected" : "") + ">" + esc(label) + "</option>";
    }

    private String nz(String s) {
        return s == null ? "" : s.trim();
    }
}
