package com.bookexchange.util;

import com.bookexchange.model.Availability;
import com.bookexchange.model.Book;
import com.bookexchange.model.RequestStatus;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Builds the HTML shell (sidebar, notifications) shared by every logged-in page,
 * plus small reusable fragments. All dynamic text MUST go through esc().
 */
public final class HtmlUtil {
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.ENGLISH);
    private static final DateTimeFormatter DATE_ONLY = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);

    /** key, path, label */
    private static final String[][] NAV = {
            {"dashboard", "/dashboard", "Dashboard"},
            {"explore", "/explore", "Explore books"},
            {"books", "/books", "My books"},
            {"requests", "/requests", "Requests"},
            {"history", "/history", "History"},
            {"profile", "/profile", "Profile"}
    };

    private HtmlUtil() { }

    /** HTML-escapes text to prevent cross-site scripting (XSS). */
    public static String esc(Object value) {
        if (value == null) {
            return "";
        }
        String s = value.toString();
        StringBuilder out = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            switch (ch) {
                case '&': out.append("&amp;"); break;
                case '<': out.append("&lt;"); break;
                case '>': out.append("&gt;"); break;
                case '"': out.append("&quot;"); break;
                case '\'': out.append("&#39;"); break;
                default: out.append(ch);
            }
        }
        return out.toString();
    }

    public static String urlEncode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    public static String formatDateTime(Timestamp t) {
        return t == null ? "" : t.toLocalDateTime().format(DATE_TIME);
    }

    public static String formatDate(Timestamp t) {
        return t == null ? "" : t.toLocalDateTime().format(DATE_ONLY);
    }

    // ------------------------------------------------------------------ shell
    public static void render(HttpServletRequest req, HttpServletResponse resp,
                              String title, String activeNav, String content) throws IOException {
        String ctx = req.getContextPath();
        String[] flash = SessionUtil.popFlash(req);

        StringBuilder h = new StringBuilder(8192);
        h.append("<!DOCTYPE html>\n<html lang=\"en\"><head><meta charset=\"UTF-8\">")
                .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">")
                .append("<title>").append(esc(title)).append(" | BookSwap</title>")
                .append("<link rel=\"stylesheet\" href=\"").append(ctx).append("/css/style.css\">")
                .append("</head><body class=\"app-body\"><div class=\"app\">");

        // sidebar
        h.append("<aside class=\"sidebar\" id=\"sidebar\"><a class=\"brand\" href=\"").append(ctx).append("/dashboard\">")
                .append("<span class=\"brand-mark\" aria-hidden=\"true\"></span>BookSwap</a><nav class=\"nav\">");
        for (String[] item : NAV) {
            h.append("<a href=\"").append(ctx).append(item[1]).append("\" class=\"nav-link")
                    .append(item[0].equals(activeNav) ? " active" : "").append("\">").append(item[2]).append("</a>");
        }
        h.append("</nav><div class=\"sidebar-foot\"><div class=\"who\">").append(esc(SessionUtil.studentName(req)))
                .append("</div><a class=\"nav-link\" href=\"").append(ctx).append("/logout\">Log out</a></div></aside>");

        // main
        h.append("<div class=\"main\"><header class=\"topbar\"><button type=\"button\" class=\"menu-btn\" id=\"menuBtn\" ")
                .append("aria-label=\"Toggle navigation\" aria-controls=\"sidebar\">Menu</button>")
                .append("<span class=\"topbar-title\">").append(esc(title)).append("</span></header>")
                .append("<main class=\"content\">");
        if (flash != null) {
            h.append("<div class=\"flash flash-").append(esc(flash[0])).append("\" role=\"status\"><span>")
                    .append(esc(flash[1])).append("</span><button type=\"button\" class=\"flash-close\" aria-label=\"Dismiss\">&times;</button></div>");
        }
        h.append(content).append("</main></div></div><script src=\"").append(ctx).append("/js/app.js\"></script></body></html>");

        resp.setContentType("text/html;charset=UTF-8");
        resp.getWriter().write(h.toString());
    }

    // -------------------------------------------------------------- fragments
    public static String hidden(String name, Object value) {
        return "<input type=\"hidden\" name=\"" + name + "\" value=\"" + esc(value) + "\">";
    }

    /** A one-button POST form (accept, reject, delete ...). confirm may be null. */
    public static String actionForm(String ctx, String path, String action, String idName, int id,
                                    String label, String cssClass, String confirm) {
        return "<form method=\"post\" action=\"" + ctx + path + "\" class=\"inline-form\""
                + (confirm == null ? "" : " data-confirm=\"" + esc(confirm) + "\"") + ">"
                + hidden("action", action) + hidden(idName, id)
                + "<button type=\"submit\" class=\"" + cssClass + "\">" + esc(label) + "</button></form>";
    }

    public static String availabilityBadge(Availability a) {
        String css = a == Availability.AVAILABLE ? "ok" : a == Availability.EXCHANGE_PENDING ? "warn" : "muted";
        return "<span class=\"badge badge-" + css + "\">" + esc(a.getLabel()) + "</span>";
    }

    public static String statusBadge(RequestStatus s) {
        return "<span class=\"badge badge-" + s.name().toLowerCase() + "\">" + esc(s.getLabel()) + "</span>";
    }

    /** One of six spine colours, chosen from the subject so each subject keeps its colour. */
    public static String spineClass(String subject) {
        int h = subject == null ? 0 : Math.abs(subject.toLowerCase().hashCode());
        return "spine-" + (h % 6);
    }

    public static String emptyState(String title, String text, String href, String linkLabel) {
        StringBuilder h = new StringBuilder("<div class=\"empty\"><h3>").append(esc(title)).append("</h3><p>")
                .append(esc(text)).append("</p>");
        if (href != null) {
            h.append("<a class=\"btn btn-primary\" href=\"").append(href).append("\">").append(esc(linkLabel)).append("</a>");
        }
        return h.append("</div>").toString();
    }

    /** Compact read-only table of books (dashboard and profile). */
    public static String bookTable(List<Book> books) {
        StringBuilder h = new StringBuilder("<div class=\"table-wrap\"><table class=\"table\"><thead><tr>")
                .append("<th>Book</th><th>Subject</th><th>Condition</th><th>Status</th><th>Added</th></tr></thead><tbody>");
        for (Book b : books) {
            h.append("<tr><td><strong>").append(esc(b.getTitle())).append("</strong><div class=\"muted small\">")
                    .append(esc(b.getAuthor())).append("</div></td><td>").append(esc(b.getSubject()))
                    .append("</td><td>").append(esc(b.getCondition().getLabel())).append("</td><td>")
                    .append(availabilityBadge(b.getAvailability())).append("</td><td>")
                    .append(formatDate(b.getCreatedAt())).append("</td></tr>");
        }
        return h.append("</tbody></table></div>").toString();
    }
}
