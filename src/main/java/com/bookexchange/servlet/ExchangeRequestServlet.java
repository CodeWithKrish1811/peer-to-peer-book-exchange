package com.bookexchange.servlet;

import static com.bookexchange.util.HtmlUtil.esc;

import com.bookexchange.exception.AppException;
import com.bookexchange.exception.BusinessRuleException;
import com.bookexchange.exception.ValidationException;
import com.bookexchange.model.Availability;
import com.bookexchange.model.Book;
import com.bookexchange.model.ExchangeRequest;
import com.bookexchange.model.RequestStatus;
import com.bookexchange.service.BookService;
import com.bookexchange.service.ExchangeService;
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

/** Sends, lists, accepts, rejects, cancels and completes exchange requests. */
@WebServlet("/requests")
public class ExchangeRequestServlet extends HttpServlet {
    private final ExchangeService exchangeService = new ExchangeService();
    private final BookService bookService = new BookService();

    // ------------------------------------------------------------------- GET
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if ("new".equals(req.getParameter("action"))) {
            showRequestForm(req, resp);
        } else {
            showRequests(req, resp);
        }
    }

    private void showRequestForm(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String ctx = req.getContextPath();
        int me = SessionUtil.studentId(req);
        try {
            int bookId = ValidationUtil.parseId(req.getParameter("bookId"), "book");
            Book book = bookService.getBook(bookId);
            if (book.isOwnedBy(me)) {
                throw new BusinessRuleException("You cannot request your own book.");
            }
            if (book.getAvailability() != Availability.AVAILABLE) {
                throw new BusinessRuleException("This book is not available for exchange right now.");
            }
            List<Book> mine = bookService.getAvailableBooksOf(me);

            StringBuilder h = new StringBuilder();
            h.append("<div class=\"page-head\"><div><h1 class=\"page-title\">Request an exchange</h1>")
                    .append("<p class=\"muted\">The owner will see your request and can accept or reject it.</p></div></div>")
                    .append("<section class=\"panel request-summary ").append(HtmlUtil.spineClass(book.getSubject())).append("\">")
                    .append("<h2>").append(esc(book.getTitle())).append("</h2><p class=\"muted\">by ").append(esc(book.getAuthor()))
                    .append(" &middot; ").append(esc(book.getEdition())).append(" edition &middot; ")
                    .append(esc(book.getCondition().getLabel())).append("</p><p>Listed by <strong>")
                    .append(esc(book.getOwnerName())).append("</strong> (").append(esc(book.getOwnerCollegeId())).append(")</p>");
            if (book.getExchangePreference() != null) {
                h.append("<p><strong>Would like in return:</strong> ").append(esc(book.getExchangePreference())).append("</p>");
            }
            h.append("</section><section class=\"panel\"><form method=\"post\" action=\"").append(ctx)
                    .append("/requests\" class=\"form-grid\">").append(HtmlUtil.hidden("action", "send"))
                    .append(HtmlUtil.hidden("bookId", book.getId()))
                    .append("<div class=\"field field-wide\"><label for=\"offeredBookId\">Offer one of your books (optional)</label>")
                    .append("<select class=\"select\" id=\"offeredBookId\" name=\"offeredBookId\"><option value=\"\">I am not offering a book</option>");
            for (Book m : mine) {
                h.append("<option value=\"").append(m.getId()).append("\">").append(esc(m.getTitle())).append(" (")
                        .append(esc(m.getAuthor())).append(")</option>");
            }
            h.append("</select>");
            if (mine.isEmpty()) {
                h.append("<p class=\"hint\">You have no available books to offer. You can still send the request.</p>");
            }
            h.append("</div><div class=\"field field-wide\"><label for=\"message\">Message to the owner (optional)</label>")
                    .append("<textarea class=\"input\" id=\"message\" name=\"message\" rows=\"4\" maxlength=\"500\" ")
                    .append("placeholder=\"Say when and where you can meet on campus.\"></textarea></div>")
                    .append("<div class=\"form-actions field-wide\"><button type=\"submit\" class=\"btn btn-primary\">Send request</button>")
                    .append("<a class=\"btn btn-ghost\" href=\"").append(ctx).append("/explore\">Cancel</a></div></form></section>");
            HtmlUtil.render(req, resp, "Request an exchange", "explore", h.toString());
        } catch (AppException e) {
            SessionUtil.flash(req, "error", e.getMessage());
            resp.sendRedirect(ctx + "/explore");
        }
    }

    private void showRequests(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String ctx = req.getContextPath();
        int me = SessionUtil.studentId(req);
        boolean outgoingTab = "outgoing".equals(req.getParameter("tab"));
        StringBuilder h = new StringBuilder();
        h.append("<div class=\"page-head\"><div><h1 class=\"page-title\">Exchange requests</h1>")
                .append("<p class=\"muted\">Manage requests you received and requests you sent.</p></div></div>");
        try {
            List<ExchangeRequest> incoming = exchangeService.getIncoming(me);
            List<ExchangeRequest> outgoing = exchangeService.getOutgoing(me);
            long pendingIn = incoming.stream().filter(r -> r.getStatus() == RequestStatus.PENDING).count();

            h.append("<div class=\"tabs\" role=\"tablist\"><a class=\"tab").append(outgoingTab ? "" : " active").append("\" href=\"")
                    .append(ctx).append("/requests?tab=incoming\">Incoming (").append(incoming.size()).append(")")
                    .append(pendingIn > 0 ? " <span class=\"dot\" title=\"" + pendingIn + " waiting for you\"></span>" : "")
                    .append("</a><a class=\"tab").append(outgoingTab ? " active" : "").append("\" href=\"").append(ctx)
                    .append("/requests?tab=outgoing\">Outgoing (").append(outgoing.size()).append(")</a></div>");

            List<ExchangeRequest> shown = outgoingTab ? outgoing : incoming;
            if (shown.isEmpty()) {
                h.append(outgoingTab
                        ? HtmlUtil.emptyState("You have not sent any requests", "Browse books from other students and request the ones you need.", ctx + "/explore", "Explore books")
                        : HtmlUtil.emptyState("No requests received yet", "When someone requests one of your books, it will appear here.", ctx + "/books", "View my books"));
            } else {
                h.append("<section class=\"req-list\">");
                for (ExchangeRequest r : shown) {
                    h.append(outgoingTab ? outgoingCard(ctx, r) : incomingCard(ctx, r));
                }
                h.append("</section>");
            }
        } catch (AppException e) {
            SessionUtil.flash(req, "error", e.getMessage());
        }
        HtmlUtil.render(req, resp, "Requests", "requests", h.toString());
    }

    private String incomingCard(String ctx, ExchangeRequest r) {
        StringBuilder c = new StringBuilder("<article class=\"req-card\"><div class=\"req-head\"><h3>")
                .append(esc(r.getRequestedBookTitle())).append("</h3>").append(HtmlUtil.statusBadge(r.getStatus())).append("</div>")
                .append("<p>Requested by <strong>").append(esc(r.getRequesterName())).append("</strong> (")
                .append(esc(r.getRequesterCollegeId())).append(") &middot; <a href=\"mailto:").append(esc(r.getRequesterEmail()))
                .append("\">").append(esc(r.getRequesterEmail())).append("</a></p>")
                .append("<p>").append(r.getOfferedBookTitle() == null ? "<span class=\"muted\">No book offered in return.</span>"
                        : "<strong>Offers:</strong> " + esc(r.getOfferedBookTitle())).append("</p>");
        if (r.getMessage() != null) {
            c.append("<blockquote>").append(esc(r.getMessage())).append("</blockquote>");
        }
        c.append("<div class=\"req-foot\"><span class=\"muted small\">").append(HtmlUtil.formatDateTime(r.getRequestDate()))
                .append("</span><div class=\"actions\">");
        if (r.getStatus() == RequestStatus.PENDING) {
            c.append(HtmlUtil.actionForm(ctx, "/requests", "accept", "requestId", r.getId(), "Accept", "btn btn-primary btn-sm", null))
                    .append(HtmlUtil.actionForm(ctx, "/requests", "reject", "requestId", r.getId(), "Reject", "btn btn-danger btn-sm",
                            "Reject this request?"));
        } else if (r.getStatus() == RequestStatus.ACCEPTED) {
            c.append(HtmlUtil.actionForm(ctx, "/requests", "complete", "requestId", r.getId(), "Mark as completed",
                    "btn btn-primary btn-sm", "Confirm that the books have been handed over?"));
        }
        return c.append("</div></div></article>").toString();
    }

    private String outgoingCard(String ctx, ExchangeRequest r) {
        StringBuilder c = new StringBuilder("<article class=\"req-card\"><div class=\"req-head\"><h3>")
                .append(esc(r.getRequestedBookTitle())).append("</h3>").append(HtmlUtil.statusBadge(r.getStatus())).append("</div>")
                .append("<p>Owner: <strong>").append(esc(r.getOwnerName())).append("</strong>");
        if (r.getStatus() == RequestStatus.ACCEPTED || r.getStatus() == RequestStatus.COMPLETED) {
            c.append(" &middot; <a href=\"mailto:").append(esc(r.getOwnerEmail())).append("\">").append(esc(r.getOwnerEmail())).append("</a>");
        }
        c.append("</p><p>").append(r.getOfferedBookTitle() == null ? "<span class=\"muted\">No book offered in return.</span>"
                : "<strong>You offered:</strong> " + esc(r.getOfferedBookTitle())).append("</p>");
        if (r.getMessage() != null) {
            c.append("<blockquote>").append(esc(r.getMessage())).append("</blockquote>");
        }
        if (r.getStatus() == RequestStatus.ACCEPTED) {
            c.append("<p class=\"hint\">Accepted. Contact the owner to arrange the hand-over; the owner marks it completed afterwards.</p>");
        }
        c.append("<div class=\"req-foot\"><span class=\"muted small\">").append(HtmlUtil.formatDateTime(r.getRequestDate()))
                .append("</span><div class=\"actions\">");
        if (r.getStatus().isActive()) {
            c.append(HtmlUtil.actionForm(ctx, "/requests", "cancel", "requestId", r.getId(), "Cancel request",
                    "btn btn-ghost btn-sm", "Cancel this request?"));
        }
        return c.append("</div></div></article>").toString();
    }

    // ------------------------------------------------------------------ POST
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String ctx = req.getContextPath();
        int me = SessionUtil.studentId(req);
        String action = req.getParameter("action") == null ? "" : req.getParameter("action");
        String target = ctx + "/requests?tab=incoming";
        try {
            switch (action) {
                case "send":
                    target = ctx + "/explore";           // where to go if sending fails
                    int bookId = ValidationUtil.parseId(req.getParameter("bookId"), "book");
                    Integer offered = ValidationUtil.parseOptionalId(req.getParameter("offeredBookId"), "offered book");
                    exchangeService.sendRequest(me, bookId, offered, req.getParameter("message"));
                    target = ctx + "/requests?tab=outgoing";
                    SessionUtil.flash(req, "success", "Exchange request sent. The owner has been notified in their Requests page.");
                    break;
                case "accept":
                    exchangeService.accept(me, ValidationUtil.parseId(req.getParameter("requestId"), "request"));
                    SessionUtil.flash(req, "success", "Request accepted. The book is now reserved for this exchange.");
                    break;
                case "reject":
                    exchangeService.reject(me, ValidationUtil.parseId(req.getParameter("requestId"), "request"));
                    SessionUtil.flash(req, "success", "Request rejected.");
                    break;
                case "complete":
                    exchangeService.complete(me, ValidationUtil.parseId(req.getParameter("requestId"), "request"));
                    SessionUtil.flash(req, "success", "Exchange marked as completed.");
                    break;
                case "cancel":
                    target = ctx + "/requests?tab=outgoing";
                    exchangeService.cancel(me, ValidationUtil.parseId(req.getParameter("requestId"), "request"));
                    SessionUtil.flash(req, "success", "Request cancelled.");
                    break;
                default:
                    throw new ValidationException("Unknown action.");
            }
        } catch (AppException e) {
            SessionUtil.flash(req, "error", e.getMessage());
        }
        resp.sendRedirect(target);
    }
}
