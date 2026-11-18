package com.bookexchange.service;

import com.bookexchange.dao.BookDAO;
import com.bookexchange.dao.ExchangeHistoryDAO;
import com.bookexchange.dao.ExchangeRequestDAO;
import com.bookexchange.exception.AppException;
import com.bookexchange.exception.AuthorizationException;
import com.bookexchange.exception.BusinessRuleException;
import com.bookexchange.model.Availability;
import com.bookexchange.model.Book;
import com.bookexchange.model.ExchangeHistory;
import com.bookexchange.model.ExchangeRequest;
import com.bookexchange.model.RequestStatus;
import com.bookexchange.util.Tx;
import com.bookexchange.util.ValidationUtil;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * The exchange workflow. Every state change runs inside ONE database transaction
 * (Tx.write): rows are locked with SELECT ... FOR UPDATE, the request, the books and
 * the history log are updated together, then committed - or rolled back on any error.
 */
public class ExchangeService {
    private final ExchangeRequestDAO requestDao = new ExchangeRequestDAO();
    private final BookDAO bookDao = new BookDAO();
    private final ExchangeHistoryDAO historyDao = new ExchangeHistoryDAO();

    // ------------------------------------------------------------------ send
    public int sendRequest(int requesterId, int bookId, Integer offeredBookId, String message) throws AppException {
        String text = ValidationUtil.trimToNull(message);
        ValidationUtil.requireMaxLength(text, 500, "Message");

        return Tx.write(c -> {
            Book book = bookDao.findByIdForUpdate(c, bookId)
                    .orElseThrow(() -> new BusinessRuleException("This book no longer exists."));
            if (book.isOwnedBy(requesterId)) {
                throw new BusinessRuleException("You cannot request your own book.");
            }
            if (book.getAvailability() != Availability.AVAILABLE) {
                throw new BusinessRuleException("This book is not available for exchange right now.");
            }
            if (requestDao.existsActive(c, requesterId, bookId)) {
                throw new BusinessRuleException("You already have an active request for this book.");
            }
            if (offeredBookId != null) {
                Book offered = bookDao.findByIdForUpdate(c, offeredBookId)
                        .orElseThrow(() -> new BusinessRuleException("The book you offered no longer exists."));
                if (!offered.isOwnedBy(requesterId)) {
                    throw new AuthorizationException("You can only offer your own books.");
                }
                if (offered.getAvailability() != Availability.AVAILABLE) {
                    throw new BusinessRuleException("The book you offered is not available.");
                }
            }
            int requestId = requestDao.insert(c,
                    new ExchangeRequest(requesterId, book.getOwnerId(), bookId, offeredBookId, text));
            log(c, requestId, bookId, requesterId, RequestStatus.PENDING, "Exchange request sent");
            return requestId;
        });
    }

    // ------------------------------------------------------------ accept/reject
    public void accept(int ownerId, int requestId) throws AppException {
        Tx.write(c -> {
            ExchangeRequest r = lockOwnedRequest(c, ownerId, requestId, RequestStatus.PENDING,
                    "Only the book owner can accept requests.");
            Book book = bookDao.findByIdForUpdate(c, r.getRequestedBookId())
                    .orElseThrow(() -> new BusinessRuleException("The requested book no longer exists."));
            if (book.getAvailability() != Availability.AVAILABLE) {
                throw new BusinessRuleException("This book is no longer available, so the request cannot be accepted.");
            }
            Book offered = null;
            if (r.getOfferedBookId() != null) {
                offered = bookDao.findByIdForUpdate(c, r.getOfferedBookId()).orElse(null);
                if (offered == null || offered.getAvailability() != Availability.AVAILABLE) {
                    throw new BusinessRuleException("The book offered in return is no longer available.");
                }
            }

            requestDao.updateStatus(c, requestId, RequestStatus.ACCEPTED);
            bookDao.updateAvailability(c, book.getId(), Availability.EXCHANGE_PENDING);
            if (offered != null) {
                bookDao.updateAvailability(c, offered.getId(), Availability.EXCHANGE_PENDING);
            }
            log(c, requestId, book.getId(), ownerId, RequestStatus.ACCEPTED, "Owner accepted the request");

            // Conflicting requests for the same book are rejected automatically.
            for (int otherId : requestDao.findOtherPendingIds(c, book.getId(), requestId)) {
                requestDao.updateStatus(c, otherId, RequestStatus.REJECTED);
                log(c, otherId, book.getId(), ownerId, RequestStatus.REJECTED,
                        "Rejected automatically: the book was promised to another student");
            }
            return null;
        });
    }

    public void reject(int ownerId, int requestId) throws AppException {
        Tx.write(c -> {
            ExchangeRequest r = lockOwnedRequest(c, ownerId, requestId, RequestStatus.PENDING,
                    "Only the book owner can reject requests.");
            requestDao.updateStatus(c, requestId, RequestStatus.REJECTED);
            log(c, requestId, r.getRequestedBookId(), ownerId, RequestStatus.REJECTED, "Owner rejected the request");
            return null;
        });
    }

    // ------------------------------------------------------------------ cancel
    /** The student who SENT the request can cancel it while it is pending or accepted. */
    public void cancel(int requesterId, int requestId) throws AppException {
        Tx.write(c -> {
            ExchangeRequest r = requestDao.findByIdForUpdate(c, requestId)
                    .orElseThrow(() -> new BusinessRuleException("Request not found."));
            if (r.getRequesterId() != requesterId) {
                throw new AuthorizationException("Only the student who sent a request can cancel it.");
            }
            if (!r.getStatus().isActive()) {
                throw new BusinessRuleException("Only pending or accepted requests can be cancelled.");
            }
            if (r.getStatus() == RequestStatus.ACCEPTED) {
                releaseBooks(c, r);          // put the books back on the market
            }
            requestDao.updateStatus(c, requestId, RequestStatus.CANCELLED);
            log(c, requestId, r.getRequestedBookId(), requesterId, RequestStatus.CANCELLED, "Requester cancelled the request");
            return null;
        });
    }

    // ---------------------------------------------------------------- complete
    /** Only the book owner can mark an ACCEPTED exchange as completed (the books were handed over). */
    public void complete(int ownerId, int requestId) throws AppException {
        Tx.write(c -> {
            ExchangeRequest r = lockOwnedRequest(c, ownerId, requestId, RequestStatus.ACCEPTED,
                    "Only the book owner can mark an exchange as completed.");
            requestDao.updateStatus(c, requestId, RequestStatus.COMPLETED);
            bookDao.updateAvailability(c, r.getRequestedBookId(), Availability.EXCHANGED);
            if (r.getOfferedBookId() != null) {
                bookDao.updateAvailability(c, r.getOfferedBookId(), Availability.EXCHANGED);
            }
            log(c, requestId, r.getRequestedBookId(), ownerId, RequestStatus.COMPLETED,
                    "Owner marked the exchange as completed");
            return null;
        });
    }

    // ----------------------------------------------------------------- helpers
    /** Locks the request row, then checks ownership and the expected current status. */
    private ExchangeRequest lockOwnedRequest(Connection c, int ownerId, int requestId,
                                             RequestStatus expected, String notOwnerMessage)
            throws SQLException, AppException {
        ExchangeRequest r = requestDao.findByIdForUpdate(c, requestId)
                .orElseThrow(() -> new BusinessRuleException("Request not found."));
        if (r.getOwnerId() != ownerId) {
            throw new AuthorizationException(notOwnerMessage);
        }
        if (r.getStatus() != expected) {
            throw new BusinessRuleException("This request is already " + r.getStatus().getLabel().toLowerCase()
                    + " and can no longer be changed this way.");
        }
        return r;
    }

    private void releaseBooks(Connection c, ExchangeRequest r) throws SQLException {
        bookDao.findByIdForUpdate(c, r.getRequestedBookId()).ifPresent(b -> release(c, b));
        if (r.getOfferedBookId() != null) {
            bookDao.findByIdForUpdate(c, r.getOfferedBookId()).ifPresent(b -> release(c, b));
        }
    }

    private void release(Connection c, Book b) {
        try {
            if (b.getAvailability() == Availability.EXCHANGE_PENDING) {
                bookDao.updateAvailability(c, b.getId(), Availability.AVAILABLE);
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);   // forces a rollback in Tx.write
        }
    }

    private void log(Connection c, int requestId, int bookId, int studentId,
                     RequestStatus status, String details) throws SQLException {
        historyDao.insert(c, new ExchangeHistory(requestId, bookId, studentId, status, details));
    }

    // ------------------------------------------------------------------- reads
    public List<ExchangeRequest> getIncoming(int ownerId) throws AppException {
        return Tx.read(c -> requestDao.findIncoming(c, ownerId));
    }

    public List<ExchangeRequest> getOutgoing(int requesterId) throws AppException {
        return Tx.read(c -> requestDao.findOutgoing(c, requesterId));
    }

    public List<ExchangeRequest> getHistory(int studentId, RequestStatus status) throws AppException {
        return Tx.read(c -> requestDao.findForStudent(c, studentId, status));
    }

    public List<ExchangeHistory> getActivityLog(int studentId, int limit) throws AppException {
        return Tx.read(c -> historyDao.findForStudent(c, studentId, limit));
    }

    public Map<RequestStatus, Integer> getStatistics(int studentId) throws AppException {
        return Tx.read(c -> requestDao.countByStatus(c, studentId));
    }
}
