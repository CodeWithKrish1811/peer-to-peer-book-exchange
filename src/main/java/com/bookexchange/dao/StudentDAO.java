package com.bookexchange.dao;

import com.bookexchange.model.Student;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

/** JDBC access for the students table. */
public class StudentDAO implements BaseDao<Student> {
    private static final String SELECT =
            "SELECT student_id, full_name, email, college_id, phone, password_hash, created_at FROM students ";

    private Student map(ResultSet rs) throws SQLException {
        Student s = new Student();
        s.setId(rs.getInt("student_id"));
        s.setFullName(rs.getString("full_name"));
        s.setEmail(rs.getString("email"));
        s.setCollegeId(rs.getString("college_id"));
        s.setPhone(rs.getString("phone"));
        s.setPasswordHash(rs.getString("password_hash"));
        s.setCreatedAt(rs.getTimestamp("created_at"));
        return s;
    }

    @Override
    public Optional<Student> findById(Connection c, int id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(SELECT + "WHERE student_id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public Optional<Student> findByEmail(Connection c, String email) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(SELECT + "WHERE email = ?")) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    /** INSERT; returns the generated student_id. */
    public int insert(Connection c, Student s) throws SQLException {
        String sql = "INSERT INTO students (full_name, email, college_id, phone, password_hash) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, s.getFullName());
            ps.setString(2, s.getEmail());
            ps.setString(3, s.getCollegeId());
            ps.setString(4, s.getPhone());
            ps.setString(5, s.getPasswordHash());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    public boolean existsByEmail(Connection c, String email) throws SQLException {
        return exists(c, "SELECT 1 FROM students WHERE email = ?", email);
    }

    public boolean existsByCollegeId(Connection c, String collegeId) throws SQLException {
        return exists(c, "SELECT 1 FROM students WHERE college_id = ?", collegeId);
    }

    private boolean exists(Connection c, String sql, String value) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, value);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** UPDATE */
    public void updateProfile(Connection c, int id, String fullName, String phone) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE students SET full_name = ?, phone = ? WHERE student_id = ?")) {
            ps.setString(1, fullName);
            ps.setString(2, phone);
            ps.setInt(3, id);
            ps.executeUpdate();
        }
    }

    public void updatePassword(Connection c, int id, String passwordHash) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE students SET password_hash = ? WHERE student_id = ?")) {
            ps.setString(1, passwordHash);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }
}
