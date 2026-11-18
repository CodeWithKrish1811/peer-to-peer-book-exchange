package com.bookexchange.dao;

import com.bookexchange.model.ExchangeRequest;
import com.bookexchange.model.RequestStatus;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** JDBC access for the exchange_requests table, including multi-table JOIN queries. */
public class ExchangeRequestDAO implements BaseDao<ExchangeRequest> {

    /** Request columns plus book titles and both students' details (JOIN of 5 tables). */
    private static final String SELECT_JOINED =
            "SELECT r.*, rb.title AS requested_title, ob.title AS offered_title, "
                    + "rq.full_name AS requester_name, rq.email AS requester_email, rq.college_id AS requester_college_id, "
                    + "ow.full_name AS owner_name, ow.email AS owner_email "
                    + "FROM exchange_requests r "
                    + "JOIN books rb ON rb.book_id = r.requested_book_id "
                    + "LEFT JOIN books ob ON ob.book_id = r.offered_book_id "
                    + "JOIN students rq ON rq.student_id = r.requester_id "
                    + "JOIN students ow ON ow.student_id = r.owner_id ";

    private ExchangeRequest map(ResultSet rs, boolean joined) throws SQLException {
        ExchangeRequest r = new ExchangeRequest();
        r.setId(rs.getInt("request_id"));
        r.setRequesterId(rs.getInt("requester_id"));
        r.setOwnerId(rs.getInt("owner_id"));
        r.setRequestedBookId(rs.getInt("requested_book_id"));
        int offered = rs.getInt("offered_book_id");
        r.setOfferedBookId(rs.wasNull() ? null : offered);
        r.setMessage(rs.getString("message"));
        r.setStatus(RequestStatus.valueOf(rs.getString("request_status")));
        r.setCreatedAt(rs.getTimestamp("request_date"));
        r.setUpdatedAt(rs.getTimestamp("updated_at"));
        if (joined) {
            r.setRequestedBookTitle(rs.getString("requested_title"));
            r.setOfferedBookTitle(rs.getString("offered_title"));
            r.setRequesterName(rs.getString("requester_name"));
            r.setRequesterEmail(rs.getString("requester_email"));
            r.setRequesterCollegeId(rs.getString("requester_college_id"));
            r.setOwnerName(rs.getString("owner_name"));
            r.setOwnerEmail(rs.getString("owner_email"));
        }
        return r;
    }

    private List<ExchangeRequest> list(PreparedStatement ps) throws SQLException {
        List<ExchangeRequest> result = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(map(rs, true));
            }
        }
        return result;
    }

    public int insert(Connection c, ExchangeRequest r) throws SQLException {
        String sql = "INSERT INTO exchange_requests (requester_id, owner_id, requested_book_id, offered_book_id, "
                + "message, request_status) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, r.getRequesterId());
            ps.setInt(2, r.getOwnerId());
            ps.setInt(3, r.getRequestedBookId());
            if (r.getOfferedBookId() == null) {
                ps.setNull(4, Types.INTEGER);
            } else {
                ps.setInt(4, r.getOfferedBookId());
            }
            ps.setString(5, r.getMessage());
            ps.setString(6, RequestStatus.PENDING.name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    @Override
    public Optional<ExchangeRequest> findById(Connection c, int id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(SELECT_JOINED + "WHERE r.request_id = ?")) {
            ps.setInt(1, id);
            List<ExchangeRequest> found = list(ps);
            return found.isEmpty() ? Optional.empty() : Optional.of(found.get(0));
        }
    }

    /** Locks the request row for the duration of the transaction. */
    public Optional<ExchangeRequest> findByIdForUpdate(Connection c, int id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT * FROM exchange_requests WHERE request_id = ? FOR UPDATE")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs, false)) : Optional.empty();
            }
        }
    }

    public List<ExchangeRequest> findIncoming(Connection c, int ownerId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(SELECT_JOINED + "WHERE r.owner_id = ? ORDER BY r.request_date DESC")) {
            ps.setInt(1, ownerId);
            return list(ps);
        }
    }

    public List<ExchangeRequest> findOutgoing(Connection c, int requesterId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(SELECT_JOINED + "WHERE r.requester_id = ? ORDER BY r.request_date DESC")) {
            ps.setInt(1, requesterId);
            return list(ps);
        }
    }

    /** Every request the student sent or received, optionally filtered by status. */
    public List<ExchangeRequest> findForStudent(Connection c, int studentId, RequestStatus status) throws SQLException {
        String sql = SELECT_JOINED + "WHERE (r.requester_id = ? OR r.owner_id = ?) "
                + (status == null ? "" : "AND r.request_status = ? ") + "ORDER BY r.request_date DESC";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setInt(2, studentId);
            if (status != null) {
                ps.setString(3, status.name());
            }
            return list(ps);
        }
    }

    /** Is there already a PENDING or ACCEPTED request from this student for this book? */
    public boolean existsActive(Connection c, int requesterId, int bookId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT 1 FROM exchange_requests WHERE requester_id = ? AND requested_book_id = ? "
                        + "AND request_status IN ('PENDING','ACCEPTED') LIMIT 1")) {
            ps.setInt(1, requesterId);
            ps.setInt(2, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Ids of the other PENDING requests for the same book (locked for update). */
    public List<Integer> findOtherPendingIds(Connection c, int bookId, int exceptRequestId) throws SQLException {
        List<Integer> ids = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT request_id FROM exchange_requests WHERE requested_book_id = ? "
                        + "AND request_status = 'PENDING' AND request_id <> ? FOR UPDATE")) {
            ps.setInt(1, bookId);
            ps.setInt(2, exceptRequestId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getInt(1));
                }
            }
        }
        return ids;
    }

    public void updateStatus(Connection c, int requestId, RequestStatus status) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE exchange_requests SET request_status = ? WHERE request_id = ?")) {
            ps.setString(1, status.name());
            ps.setInt(2, requestId);
            ps.executeUpdate();
        }
    }

    public int countIncomingPending(Connection c, int ownerId) throws SQLException {
        return count(c, "SELECT COUNT(*) FROM exchange_requests WHERE owner_id = ? AND request_status = 'PENDING'", ownerId);
    }

    public int countOutgoingPending(Connection c, int requesterId) throws SQLException {
        return count(c, "SELECT COUNT(*) FROM exchange_requests WHERE requester_id = ? AND request_status = 'PENDING'", requesterId);
    }

    public int countCompleted(Connection c, int studentId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT COUNT(*) FROM exchange_requests WHERE (requester_id = ? OR owner_id = ?) AND request_status = 'COMPLETED'")) {
            ps.setInt(1, studentId);
            ps.setInt(2, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private int count(Connection c, String sql, int id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /** GROUP BY query returning a status -> count map for the profile statistics. */
    public Map<RequestStatus, Integer> countByStatus(Connection c, int studentId) throws SQLException {
        Map<RequestStatus, Integer> counts = new EnumMap<>(RequestStatus.class);
        for (RequestStatus s : RequestStatus.values()) {
            counts.put(s, 0);
        }
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT request_status, COUNT(*) FROM exchange_requests "
                        + "WHERE requester_id = ? OR owner_id = ? GROUP BY request_status")) {
            ps.setInt(1, studentId);
            ps.setInt(2, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    counts.put(RequestStatus.valueOf(rs.getString(1)), rs.getInt(2));
                }
            }
        }
        return counts;
    }
}
