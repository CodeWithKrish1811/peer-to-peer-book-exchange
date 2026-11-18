package com.bookexchange.servlet;

import static com.bookexchange.util.HtmlUtil.esc;

import com.bookexchange.exception.AppException;
import com.bookexchange.exception.ValidationException;
import com.bookexchange.model.Availability;
import com.bookexchange.model.Book;
import com.bookexchange.model.BookCondition;
import com.bookexchange.service.BookService;
import com.bookexchange.util.HtmlUtil;
import com.bookexchange.util.SessionUtil;
import com.bookexchange.util.ValidationUtil;
import java.io.IOException;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** My books: list, add, edit, delete and change availability (owner only). */
@WebServlet("/books")
public class BookServlet extends HttpServlet {
    private static final String[] COMMON_SUBJECTS = {"Programming", "Databases", "Algorithms", "Operating Systems",
            "Networking", "Mathematics", "Physics", "Electronics", "Software Engineering", "Artificial Intelligence"};
    private final BookService bookService = new BookService();

    // ------------------------------------------------------------------- GET
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = req.getParameter("action");
        int me = SessionUtil.studentId(req);
        if ("add".equals(action)) {
            showForm(req, resp, new Book(), false);
        } else if ("edit".equals(action)) {
            try {
                Book book = bookService.getOwnedBook(me, ValidationUtil.parseId(req.getParameter("id"), "book"));
                showForm(req, resp, book, true);
            } catch (AppException e) {
                SessionUtil.flash(req, "error", e.getMessage());
                resp.sendRedirect(req.getContextPath() + "/books");
            }
        } else {
            showList(req, resp, me);
        }
    }

    private void showList(HttpServletRequest req, HttpServletResponse resp, int me) throws IOException {
        String ctx = req.getContextPath();
        StringBuilder h = new StringBuilder();
        h.append("<div class=\"page-head\"><div><h1 class=\"page-title\">My books</h1>")
                .append("<p class=\"muted\">Books you have listed for exchange.</p></div>")
                .append("<a class=\"btn btn-primary\" href=\"").append(ctx).append("/books?action=add\">Add a book</a></div>");
        try {
            List<Book> books = bookService.getMyBooks(me);
            if (books.isEmpty()) {
                h.append(HtmlUtil.emptyState("You have not listed any books",
                        "List a book and other students can send you exchange requests.",
                        ctx + "/books?action=add", "Add a book"));
            } else {
                h.append("<section class=\"panel\"><div class=\"table-wrap\"><table class=\"table\"><thead><tr>")
                        .append("<th>Book</th><th>Subject</th><th>Edition</th><th>Condition</th><th>Availability</th><th>Actions</th>")
                        .append("</tr></thead><tbody>");
                for (Book b : books) {
                    h.append("<tr><td><strong>").append(esc(b.getTitle())).append("</strong><div class=\"muted small\">")
                            .append(esc(b.getAuthor())).append("</div></td><td>").append(esc(b.getSubject()))
                            .append("</td><td>").append(esc(b.getEdition())).append("</td><td>")
                            .append(esc(b.getCondition().getLabel())).append("</td><td>");
                    if (b.getAvailability() == Availability.EXCHANGE_PENDING) {
                        h.append(HtmlUtil.availabilityBadge(b.getAvailability()))
                                .append("<div class=\"muted small\">Managed in Requests</div>");
                    } else {
                        h.append(availabilityForm(ctx, b));
                    }
                    h.append("</td><td class=\"actions\"><a class=\"btn btn-ghost btn-sm\" href=\"").append(ctx)
                            .append("/books?action=edit&amp;id=").append(b.getId()).append("\">Edit</a>")
                            .append(HtmlUtil.actionForm(ctx, "/books", "delete", "id", b.getId(), "Delete",
                                    "btn btn-danger btn-sm", "Delete \"" + b.getTitle() + "\"? This cannot be undone."))
                            .append("</td></tr>");
                }
                h.append("</tbody></table></div></section>");
            }
        } catch (AppException e) {
            SessionUtil.flash(req, "error", e.getMessage());
        }
        HtmlUtil.render(req, resp, "My books", "books", h.toString());
    }

    private String availabilityForm(String ctx, Book b) {
        StringBuilder f = new StringBuilder("<form method=\"post\" action=\"" + ctx + "/books\" class=\"inline-form\">")
                .append(HtmlUtil.hidden("action", "availability")).append(HtmlUtil.hidden("id", b.getId()))
                .append("<select name=\"availability\" class=\"select select-sm auto-submit\" aria-label=\"Availability\">");
        for (Availability a : new Availability[]{Availability.AVAILABLE, Availability.EXCHANGED}) {
            f.append("<option value=\"").append(a.name()).append("\"")
                    .append(a == b.getAvailability() ? " selected" : "").append(">").append(esc(a.getLabel())).append("</option>");
        }
        return f.append("</select><noscript><button type=\"submit\" class=\"btn btn-ghost btn-sm\">Save</button></noscript></form>").toString();
    }

    private void showForm(HttpServletRequest req, HttpServletResponse resp, Book b, boolean editing) throws IOException {
        String ctx = req.getContextPath();
        StringBuilder h = new StringBuilder();
        h.append("<div class=\"page-head\"><div><h1 class=\"page-title\">").append(editing ? "Edit book" : "Add a book")
                .append("</h1><p class=\"muted\">").append(editing ? "Update the details of your listing." : "Tell other students what you can offer.")
                .append("</p></div></div><section class=\"panel\"><form method=\"post\" action=\"").append(ctx)
                .append("/books\" class=\"form-grid\">").append(HtmlUtil.hidden("action", "save"));
        if (editing) {
            h.append(HtmlUtil.hidden("id", b.getId()));
        }
        h.append(field("Book title", "title", b.getTitle(), "text", true, 200, null))
                .append(field("Author", "author", b.getAuthor(), "text", true, 150, null))
                .append(field("Subject", "subject", b.getSubject(), "text", true, 100, "subjects"))
                .append(field("Edition", "edition", b.getEdition(), "text", true, 30, null))
                .append(field("ISBN (optional)", "isbn", b.getIsbn(), "text", false, 17, null));

        h.append("<div class=\"field\"><label for=\"condition\">Condition</label><select id=\"condition\" name=\"condition\" class=\"select\">");
        for (BookCondition c : BookCondition.values()) {
            h.append("<option value=\"").append(c.name()).append("\"").append(c == b.getCondition() ? " selected" : "")
                    .append(">").append(esc(c.getLabel())).append("</option>");
        }
        h.append("</select></div>")
                .append(field("Exchange preference (optional)", "exchangePreference", b.getExchangePreference(), "text", false, 255, null))
                .append("<div class=\"field field-wide\"><label for=\"description\">Description (optional)</label>")
                .append("<textarea id=\"description\" name=\"description\" rows=\"4\" maxlength=\"2000\" class=\"input\">")
                .append(esc(b.getDescription())).append("</textarea></div>");

        h.append("<datalist id=\"subjects\">");
        for (String s : COMMON_SUBJECTS) {
            h.append("<option value=\"").append(esc(s)).append("\">");
        }
        h.append("</datalist><div class=\"form-actions field-wide\"><button type=\"submit\" class=\"btn btn-primary\">")
                .append(editing ? "Save changes" : "Add book").append("</button><a class=\"btn btn-ghost\" href=\"")
                .append(ctx).append("/books\">Cancel</a></div></form></section>");
        HtmlUtil.render(req, resp, editing ? "Edit book" : "Add a book", "books", h.toString());
    }

    private String field(String label, String name, String value, String type, boolean required, int max, String list) {
        return "<div class=\"field\"><label for=\"" + name + "\">" + esc(label) + "</label><input class=\"input\" id=\"" + name
                + "\" name=\"" + name + "\" type=\"" + type + "\" value=\"" + esc(value) + "\" maxlength=\"" + max + "\""
                + (required ? " required" : "") + (list == null ? "" : " list=\"" + list + "\"") + "></div>";
    }

    // ------------------------------------------------------------------ POST
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String ctx = req.getContextPath();
        int me = SessionUtil.studentId(req);
        String action = req.getParameter("action") == null ? "" : req.getParameter("action");
        try {
            switch (action) {
                case "save":
                    save(req, resp, me);
                    return;
                case "delete":
                    bookService.deleteBook(me, ValidationUtil.parseId(req.getParameter("id"), "book"));
                    SessionUtil.flash(req, "success", "Book deleted.");
                    break;
                case "availability":
                    bookService.changeAvailability(me, ValidationUtil.parseId(req.getParameter("id"), "book"),
                            Availability.fromString(req.getParameter("availability")));
                    SessionUtil.flash(req, "success", "Availability updated.");
                    break;
                default:
                    throw new ValidationException("Unknown action.");
            }
        } catch (AppException e) {
            SessionUtil.flash(req, "error", e.getMessage());
        }
        resp.sendRedirect(ctx + "/books");
    }

    private void save(HttpServletRequest req, HttpServletResponse resp, int me) throws IOException {
        String idParam = req.getParameter("id");
        boolean editing = !ValidationUtil.isBlank(idParam);
        Book b = new Book();
        b.setOwnerId(me);
        b.setTitle(req.getParameter("title"));
        b.setAuthor(req.getParameter("author"));
        b.setSubject(req.getParameter("subject"));
        b.setEdition(req.getParameter("edition"));
        b.setIsbn(req.getParameter("isbn"));
        b.setDescription(req.getParameter("description"));
        b.setExchangePreference(req.getParameter("exchangePreference"));
        try {
            b.setCondition(BookCondition.fromString(req.getParameter("condition")));
            if (editing) {
                b.setId(ValidationUtil.parseId(idParam, "book"));
                bookService.updateBook(me, b);
                SessionUtil.flash(req, "success", "Book updated.");
            } else {
                bookService.addBook(b);
                SessionUtil.flash(req, "success", "Book added to your listings.");
            }
            resp.sendRedirect(req.getContextPath() + "/books");
        } catch (AppException e) {
            SessionUtil.flash(req, "error", e.getMessage());
            showForm(req, resp, b, editing);       // redisplay the form with what the student typed
        }
    }
}
