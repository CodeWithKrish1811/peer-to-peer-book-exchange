package com.bookexchange.dao;

import com.bookexchange.model.ExchangeHistory;
import com.bookexchange.model.RequestStatus;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** JDBC access for the exchange_history audit table. Records are only ever inserted, never deleted. */
public class ExchangeHistoryDAO {

    public void insert(Connection c, ExchangeHistory h) throws SQLException {
        String sql = "INSERT INTO exchange_history (request_id, book_id, student_id, exchange_status, details) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, h.getRequestId());
            ps.setInt(2, h.getBookId());
            ps.setInt(3, h.getStudentId());
            ps.setString(4, h.getStatus().name());
            ps.setString(5, h.getDetails());
            ps.executeUpdate();
        }
    }

    /** Activity log for every request the student sent or received (JOIN with books and students). */
    public List<ExchangeHistory> findForStudent(Connection c, int studentId, int limit) throws SQLException {
        String sql = "SELECT h.*, b.title AS book_title, s.full_name AS student_name "
                + "FROM exchange_history h "
                + "JOIN exchange_requests r ON r.request_id = h.request_id "
                + "JOIN books b ON b.book_id = h.book_id "
                + "JOIN students s ON s.student_id = h.student_id "
                + "WHERE r.requester_id = ? OR r.owner_id = ? "
                + "ORDER BY h.exchange_date DESC, h.history_id DESC LIMIT ?";
        List<ExchangeHistory> result = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setInt(2, studentId);
            ps.setInt(3, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ExchangeHistory h = new ExchangeHistory();
                    h.setId(rs.getInt("history_id"));
                    h.setRequestId(rs.getInt("request_id"));
                    h.setBookId(rs.getInt("book_id"));
                    h.setStudentId(rs.getInt("student_id"));
                    h.setStatus(RequestStatus.valueOf(rs.getString("exchange_status")));
                    h.setDetails(rs.getString("details"));
                    h.setCreatedAt(rs.getTimestamp("exchange_date"));
                    h.setBookTitle(rs.getString("book_title"));
                    h.setStudentName(rs.getString("student_name"));
                    result.add(h);
                }
            }
        }
        return result;
    }
}
