package com.bookexchange.dao;

import com.bookexchange.model.Availability;
import com.bookexchange.model.Book;
import com.bookexchange.model.BookCondition;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** JDBC access for the books table (INSERT, UPDATE, DELETE, SELECT and JOIN queries). */
public class BookDAO implements BaseDao<Book> {

    /** Book columns plus the owner's details through a JOIN. */
    private static final String SELECT_WITH_OWNER =
            "SELECT b.*, s.full_name AS owner_name, s.email AS owner_email, s.college_id AS owner_college_id "
                    + "FROM books b JOIN students s ON s.student_id = b.owner_id ";

    private Book map(ResultSet rs, boolean withOwner) throws SQLException {
        Book b = new Book();
        b.setId(rs.getInt("book_id"));
        b.setOwnerId(rs.getInt("owner_id"));
        b.setTitle(rs.getString("title"));
        b.setAuthor(rs.getString("author"));
        b.setSubject(rs.getString("subject"));
        b.setIsbn(rs.getString("isbn"));
        b.setEdition(rs.getString("edition"));
        b.setCondition(BookCondition.valueOf(rs.getString("book_condition")));
        b.setDescription(rs.getString("description"));
        b.setExchangePreference(rs.getString("exchange_preference"));
        b.setAvailability(Availability.valueOf(rs.getString("availability")));
        b.setCreatedAt(rs.getTimestamp("created_at"));
        if (withOwner) {
            b.setOwnerName(rs.getString("owner_name"));
            b.setOwnerEmail(rs.getString("owner_email"));
            b.setOwnerCollegeId(rs.getString("owner_college_id"));
        }
        return b;
    }

    private List<Book> list(PreparedStatement ps) throws SQLException {
        List<Book> books = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                books.add(map(rs, true));
            }
        }
        return books;
    }

    /** INSERT; returns the generated book_id. */
    public int insert(Connection c, Book b) throws SQLException {
        String sql = "INSERT INTO books (owner_id, title, author, subject, isbn, edition, book_condition, "
                + "description, exchange_preference, availability) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, b.getOwnerId());
            ps.setString(2, b.getTitle());
            ps.setString(3, b.getAuthor());
            ps.setString(4, b.getSubject());
            ps.setString(5, b.getIsbn());
            ps.setString(6, b.getEdition());
            ps.setString(7, b.getCondition().name());
            ps.setString(8, b.getDescription());
            ps.setString(9, b.getExchangePreference());
            ps.setString(10, Availability.AVAILABLE.name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    /** UPDATE of the editable fields; the owner_id in the WHERE clause is a second safety net. */
    public int update(Connection c, Book b) throws SQLException {
        String sql = "UPDATE books SET title = ?, author = ?, subject = ?, isbn = ?, edition = ?, "
                + "book_condition = ?, description = ?, exchange_preference = ? WHERE book_id = ? AND owner_id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, b.getTitle());
            ps.setString(2, b.getAuthor());
            ps.setString(3, b.getSubject());
            ps.setString(4, b.getIsbn());
            ps.setString(5, b.getEdition());
            ps.setString(6, b.getCondition().name());
            ps.setString(7, b.getDescription());
            ps.setString(8, b.getExchangePreference());
            ps.setInt(9, b.getId());
            ps.setInt(10, b.getOwnerId());
            return ps.executeUpdate();
        }
    }

    /** DELETE */
    public int delete(Connection c, int bookId, int ownerId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("DELETE FROM books WHERE book_id = ? AND owner_id = ?")) {
            ps.setInt(1, bookId);
            ps.setInt(2, ownerId);
            return ps.executeUpdate();
        }
    }

    public void updateAvailability(Connection c, int bookId, Availability availability) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE books SET availability = ? WHERE book_id = ?")) {
            ps.setString(1, availability.name());
            ps.setInt(2, bookId);
            ps.executeUpdate();
        }
    }

    @Override
    public Optional<Book> findById(Connection c, int id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(SELECT_WITH_OWNER + "WHERE b.book_id = ?")) {
            ps.setInt(1, id);
            List<Book> found = list(ps);
            return found.isEmpty() ? Optional.empty() : Optional.of(found.get(0));
        }
    }

    /** Locks the row (SELECT ... FOR UPDATE) so concurrent requests are serialised inside a transaction. */
    public Optional<Book> findByIdForUpdate(Connection c, int id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT * FROM books WHERE book_id = ? FOR UPDATE")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs, false)) : Optional.empty();
            }
        }
    }

    public List<Book> findByOwner(Connection c, int ownerId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                SELECT_WITH_OWNER + "WHERE b.owner_id = ? ORDER BY b.created_at DESC, b.book_id DESC")) {
            ps.setInt(1, ownerId);
            return list(ps);
        }
    }

    public List<Book> findAvailableByOwner(Connection c, int ownerId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                SELECT_WITH_OWNER + "WHERE b.owner_id = ? AND b.availability = 'AVAILABLE' ORDER BY b.title")) {
            ps.setInt(1, ownerId);
            return list(ps);
        }
    }

    public List<Book> findRecentByOwner(Connection c, int ownerId, int limit) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                SELECT_WITH_OWNER + "WHERE b.owner_id = ? ORDER BY b.created_at DESC, b.book_id DESC LIMIT ?")) {
            ps.setInt(1, ownerId);
            ps.setInt(2, limit);
            return list(ps);
        }
    }

    /**
     * Explore query. Only AVAILABLE books of OTHER students are returned.
     * Every filter is optional (null/blank = ignore) and bound with PreparedStatement parameters.
     *
     * @param searchBy "title", "author" or anything else for both
     */
    public List<Book> search(Connection c, int excludeOwnerId, String keyword, String searchBy,
                             String subject, BookCondition condition) throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT_WITH_OWNER)
                .append("WHERE b.availability = 'AVAILABLE' AND b.owner_id <> ?");
        List<Object> params = new ArrayList<>();
        params.add(excludeOwnerId);

        if (keyword != null && !keyword.trim().isEmpty()) {
            String like = "%" + keyword.trim() + "%";
            if ("title".equals(searchBy)) {
                sql.append(" AND b.title LIKE ?");
                params.add(like);
            } else if ("author".equals(searchBy)) {
                sql.append(" AND b.author LIKE ?");
                params.add(like);
            } else {
                sql.append(" AND (b.title LIKE ? OR b.author LIKE ?)");
                params.add(like);
                params.add(like);
            }
        }
        if (subject != null && !subject.trim().isEmpty()) {
            sql.append(" AND b.subject = ?");
            params.add(subject.trim());
        }
        if (condition != null) {
            sql.append(" AND b.book_condition = ?");
            params.add(condition.name());
        }
        sql.append(" ORDER BY b.created_at DESC, b.book_id DESC");

        try (PreparedStatement ps = c.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            return list(ps);
        }
    }

    /** Overload: every available book from other students, no filters. */
    public List<Book> search(Connection c, int excludeOwnerId) throws SQLException {
        return search(c, excludeOwnerId, null, null, null, null);
    }

    public List<String> findSubjects(Connection c) throws SQLException {
        List<String> subjects = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement("SELECT DISTINCT subject FROM books ORDER BY subject");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                subjects.add(rs.getString(1));
            }
        }
        return subjects;
    }

    public int countByOwner(Connection c, int ownerId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM books WHERE owner_id = ?")) {
            ps.setInt(1, ownerId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public int countByOwnerAndAvailability(Connection c, int ownerId, Availability a) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT COUNT(*) FROM books WHERE owner_id = ? AND availability = ?")) {
            ps.setInt(1, ownerId);
            ps.setString(2, a.name());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /** True if any exchange request mentions the book as the requested book. */
    public boolean hasRequests(Connection c, int bookId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT 1 FROM exchange_requests WHERE requested_book_id = ? LIMIT 1")) {
            ps.setInt(1, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** True if the book is offered in a PENDING or ACCEPTED request. */
    public boolean isOfferedInActiveRequest(Connection c, int bookId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT 1 FROM exchange_requests WHERE offered_book_id = ? "
                        + "AND request_status IN ('PENDING','ACCEPTED') LIMIT 1")) {
            ps.setInt(1, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}
