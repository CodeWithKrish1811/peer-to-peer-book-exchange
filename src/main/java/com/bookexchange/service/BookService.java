package com.bookexchange.service;

import com.bookexchange.dao.BookDAO;
import com.bookexchange.exception.AppException;
import com.bookexchange.exception.AuthorizationException;
import com.bookexchange.exception.BusinessRuleException;
import com.bookexchange.exception.ValidationException;
import com.bookexchange.model.Availability;
import com.bookexchange.model.Book;
import com.bookexchange.model.BookCondition;
import com.bookexchange.util.Tx;
import com.bookexchange.util.ValidationUtil;
import java.util.List;

/** Book listing rules: validation, ownership checks, search. */
public class BookService {
    private final BookDAO bookDao = new BookDAO();

    /** Validates and normalises the user-editable fields of a book. */
    private void validate(Book b) throws ValidationException {
        b.setTitle(ValidationUtil.requireNonBlank(b.getTitle(), "Book title", 200));
        b.setAuthor(ValidationUtil.requireNonBlank(b.getAuthor(), "Author", 150));
        b.setSubject(ValidationUtil.requireNonBlank(b.getSubject(), "Subject", 100));
        b.setEdition(ValidationUtil.requireNonBlank(b.getEdition(), "Edition", 30));
        if (b.getCondition() == null) {
            throw new ValidationException("Book condition is required.");
        }
        b.setIsbn(ValidationUtil.trimToNull(b.getIsbn()));
        ValidationUtil.validateOptionalIsbn(b.getIsbn());
        b.setDescription(ValidationUtil.trimToNull(b.getDescription()));
        ValidationUtil.requireMaxLength(b.getDescription(), 2000, "Description");
        b.setExchangePreference(ValidationUtil.trimToNull(b.getExchangePreference()));
        ValidationUtil.requireMaxLength(b.getExchangePreference(), 255, "Exchange preference");
    }

    public int addBook(Book book) throws AppException {
        validate(book);
        return Tx.write(c -> bookDao.insert(c, book));
    }

    public void updateBook(int ownerId, Book book) throws AppException {
        validate(book);
        Tx.write(c -> {
            Book existing = bookDao.findByIdForUpdate(c, book.getId())
                    .orElseThrow(() -> new BusinessRuleException("This book no longer exists."));
            if (!existing.isOwnedBy(ownerId)) {
                throw new AuthorizationException("You can only edit your own books.");
            }
            book.setOwnerId(ownerId);
            bookDao.update(c, book);
            return null;
        });
    }

    public void deleteBook(int ownerId, int bookId) throws AppException {
        Tx.write(c -> {
            Book existing = bookDao.findByIdForUpdate(c, bookId)
                    .orElseThrow(() -> new BusinessRuleException("This book no longer exists."));
            if (!existing.isOwnedBy(ownerId)) {
                throw new AuthorizationException("You can only delete your own books.");
            }
            if (existing.getAvailability() == Availability.EXCHANGE_PENDING) {
                throw new BusinessRuleException("This book is part of an accepted exchange. Finish or cancel that exchange first.");
            }
            if (bookDao.hasRequests(c, bookId)) {
                throw new BusinessRuleException("This book has exchange history and cannot be deleted. Mark it as Exchanged instead.");
            }
            if (bookDao.isOfferedInActiveRequest(c, bookId)) {
                throw new BusinessRuleException("This book is offered in an active request. Cancel that request first.");
            }
            bookDao.delete(c, bookId, ownerId);
            return null;
        });
    }

    /** Manual availability change. EXCHANGE_PENDING is controlled only by the request workflow. */
    public void changeAvailability(int ownerId, int bookId, Availability target) throws AppException {
        if (target == Availability.EXCHANGE_PENDING) {
            throw new BusinessRuleException("'Exchange Pending' is set automatically when you accept a request.");
        }
        Tx.write(c -> {
            Book existing = bookDao.findByIdForUpdate(c, bookId)
                    .orElseThrow(() -> new BusinessRuleException("This book no longer exists."));
            if (!existing.isOwnedBy(ownerId)) {
                throw new AuthorizationException("You can only change your own books.");
            }
            if (existing.getAvailability() == Availability.EXCHANGE_PENDING) {
                throw new BusinessRuleException("This book is part of an accepted exchange. Complete or cancel it from the Requests page.");
            }
            bookDao.updateAvailability(c, bookId, target);
            return null;
        });
    }

    public Book getBook(int bookId) throws AppException {
        return Tx.read(c -> bookDao.findById(c, bookId)
                .orElseThrow(() -> new BusinessRuleException("This book no longer exists.")));
    }

    /** Returns the book only if the given student owns it. */
    public Book getOwnedBook(int ownerId, int bookId) throws AppException {
        Book book = getBook(bookId);
        if (!book.isOwnedBy(ownerId)) {
            throw new AuthorizationException("You can only edit your own books.");
        }
        return book;
    }

    public List<Book> getMyBooks(int ownerId) throws AppException {
        return Tx.read(c -> bookDao.findByOwner(c, ownerId));
    }

    public List<Book> getRecentBooks(int ownerId, int limit) throws AppException {
        return Tx.read(c -> bookDao.findRecentByOwner(c, ownerId, limit));
    }

    public List<Book> getAvailableBooksOf(int ownerId) throws AppException {
        return Tx.read(c -> bookDao.findAvailableByOwner(c, ownerId));
    }

    public List<Book> search(int studentId, String keyword, String searchBy,
                             String subject, String condition) throws AppException {
        BookCondition cond = ValidationUtil.isBlank(condition) ? null : BookCondition.fromString(condition);
        return Tx.read(c -> bookDao.search(c, studentId, keyword, searchBy, subject, cond));
    }

    /** Overload: all available books of other students. */
    public List<Book> search(int studentId) throws AppException {
        return Tx.read(c -> bookDao.search(c, studentId));
    }

    public List<String> getSubjects() throws AppException {
        return Tx.read(bookDao::findSubjects);
    }
}
