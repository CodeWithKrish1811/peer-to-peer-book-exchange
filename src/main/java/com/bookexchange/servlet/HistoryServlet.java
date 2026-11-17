package com.bookexchange.servlet;

import static com.bookexchange.util.HtmlUtil.esc;

import com.bookexchange.exception.AppException;
import com.bookexchange.model.ExchangeHistory;
import com.bookexchange.model.ExchangeRequest;
import com.bookexchange.model.RequestStatus;
import com.bookexchange.service.ExchangeService;
import com.bookexchange.util.HtmlUtil;
import com.bookexchange.util.SessionUtil;
import java.io.IOException;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Exchange history: every request the student sent or received, plus the audit log. */
@WebServlet("/history")
public class HistoryServlet extends HttpServlet {
    private final ExchangeService exchangeService = new ExchangeService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String ctx = req.getContextPath();
        int me = SessionUtil.studentId(req);
        String filter = req.getParameter("status") == null ? "" : req.getParameter("status").toLowerCase();
        RequestStatus status = null;
        try {
            status = filter.isEmpty() ? null : RequestStatus.fromString(filter);
        } catch (AppException e) {
            filter = "";   // unknown filter value: show everything
        }

        StringBuilder h = new StringBuilder();
        h.append("<div class=\"page-head\"><div><h1 class=\"page-title\">Exchange history</h1>")
                .append("<p class=\"muted\">Records are kept even after an exchange is completed.</p></div></div>")
                .append("<div class=\"tabs\">").append(tab(ctx, "", "All", filter));
        for (RequestStatus s : RequestStatus.values()) {
            h.append(tab(ctx, s.name().toLowerCase(), s.getLabel(), filter));
        }
        h.append("</div>");

        try {
            List<ExchangeRequest> rows = exchangeService.getHistory(me, status);
            if (rows.isEmpty()) {
                h.append(HtmlUtil.emptyState("Nothing here yet", "Requests you send or receive will be listed on this page.",
                        ctx + "/explore", "Explore books"));
            } else {
                h.append("<section class=\"panel\"><div class=\"table-wrap\"><table class=\"table\"><thead><tr>")
                        .append("<th>Book</th><th>Requested by</th><th>Owner</th><th>Request date</th><th>Status</th><th>Exchange details</th>")
                        .append("</tr></thead><tbody>");
                for (ExchangeRequest r : rows) {
                    h.append("<tr><td><strong>").append(esc(r.getRequestedBookTitle())).append("</strong></td><td>")
                            .append(esc(r.getRequesterName())).append(r.getRequesterId() == me ? " <span class=\"you\">you</span>" : "")
                            .append("</td><td>").append(esc(r.getOwnerName())).append(r.getOwnerId() == me ? " <span class=\"you\">you</span>" : "")
                            .append("</td><td>").append(HtmlUtil.formatDate(r.getRequestDate())).append("</td><td>")
                            .append(HtmlUtil.statusBadge(r.getStatus())).append("</td><td class=\"details\">")
                            .append(r.getOfferedBookTitle() == null ? "<span class=\"muted\">No book offered</span>"
                                    : "Offered: " + esc(r.getOfferedBookTitle()))
                            .append("<div class=\"muted small\">Updated ").append(HtmlUtil.formatDateTime(r.getUpdatedAt()))
                            .append("</div></td></tr>");
                }
                h.append("</tbody></table></div></section>");
            }

            List<ExchangeHistory> log = exchangeService.getActivityLog(me, 30);
            h.append("<section class=\"panel\"><div class=\"panel-head\"><h2>Activity log</h2></div>");
            if (log.isEmpty()) {
                h.append("<p class=\"muted\">No activity recorded yet.</p>");
            } else {
                h.append("<ul class=\"timeline\">");
                for (ExchangeHistory e : log) {
                    h.append("<li><span class=\"muted small\">").append(HtmlUtil.formatDateTime(e.getExchangeDate()))
                            .append("</span> ").append(HtmlUtil.statusBadge(e.getStatus())).append(" <strong>")
                            .append(esc(e.getBookTitle())).append("</strong> &ndash; ").append(esc(e.getDetails()))
                            .append(" <span class=\"muted small\">by ").append(esc(e.getStudentName())).append("</span></li>");
                }
                h.append("</ul>");
            }
            h.append("</section>");
        } catch (AppException e) {
            SessionUtil.flash(req, "error", e.getMessage());
        }
        HtmlUtil.render(req, resp, "History", "history", h.toString());
    }

    private String tab(String ctx, String value, String label, String current) {
        return "<a class=\"tab" + (value.equals(current) ? " active" : "") + "\" href=\"" + ctx + "/history"
                + (value.isEmpty() ? "" : "?status=" + value) + "\">" + esc(label) + "</a>";
    }
}
