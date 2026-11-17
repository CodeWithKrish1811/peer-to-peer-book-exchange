package com.bookexchange.service;

import com.bookexchange.dao.BookDAO;
import com.bookexchange.dao.ExchangeRequestDAO;
import com.bookexchange.dao.StudentDAO;
import com.bookexchange.exception.AppException;
import com.bookexchange.exception.AuthorizationException;
import com.bookexchange.exception.BusinessRuleException;
import com.bookexchange.exception.ValidationException;
import com.bookexchange.model.Availability;
import com.bookexchange.model.Student;
import com.bookexchange.util.PasswordUtil;
import com.bookexchange.util.Tx;
import com.bookexchange.util.ValidationUtil;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

/** Registration, login, profile and dashboard logic for students. */
public class StudentService {
    private final StudentDAO studentDao = new StudentDAO();
    private final BookDAO bookDao = new BookDAO();
    private final ExchangeRequestDAO requestDao = new ExchangeRequestDAO();

    public Student register(String fullName, String email, String collegeId,
                            String password, String confirmPassword) throws AppException {
        String name = ValidationUtil.requireNonBlank(fullName, "Full name", 100);
        String mail = ValidationUtil.requireNonBlank(email, "College email", 120).toLowerCase();
        String cid = ValidationUtil.requireNonBlank(collegeId, "Student ID", 30);
        if (!ValidationUtil.isValidEmail(mail)) {
            throw new ValidationException("Please enter a valid email address.");
        }
        ValidationUtil.requireNonBlank(password, "Password");
        ValidationUtil.validatePassword(password);
        if (!password.equals(confirmPassword)) {
            throw new ValidationException("Password and confirmation do not match.");
        }

        Student student = new Student(name, mail, cid, PasswordUtil.hash(password));
        return Tx.write(c -> {
            if (studentDao.existsByEmail(c, mail)) {
                throw new BusinessRuleException("An account with this email already exists.");
            }
            if (studentDao.existsByCollegeId(c, cid)) {
                throw new BusinessRuleException("An account with this student ID already exists.");
            }
            try {
                student.setId(studentDao.insert(c, student));
            } catch (SQLException e) {
                if (e.getErrorCode() == 1062) {   // duplicate key: two people registered at the same moment
                    throw new BusinessRuleException("An account with this email or student ID already exists.");
                }
                throw e;
            }
            return student;
        });
    }

    public Student login(String email, String password) throws AppException {
        if (ValidationUtil.isBlank(email) || ValidationUtil.isBlank(password)) {
            throw new ValidationException("Enter your email and password.");
        }
        Student student = Tx.read(c -> studentDao.findByEmail(c, email.trim().toLowerCase()).orElse(null));
        if (student == null || !PasswordUtil.verify(password, student.getPasswordHash())) {
            throw new AuthorizationException("Invalid email or password.");
        }
        return student;
    }

    public Student getById(int id) throws AppException {
        return Tx.read(c -> studentDao.findById(c, id)
                .orElseThrow(() -> new BusinessRuleException("Student account not found.")));
    }

    public void updateProfile(int id, String fullName, String phone) throws AppException {
        String name = ValidationUtil.requireNonBlank(fullName, "Full name", 100);
        String phoneValue = ValidationUtil.trimToNull(phone);
        ValidationUtil.validateOptionalPhone(phoneValue);
        Tx.write(c -> {
            studentDao.updateProfile(c, id, name, phoneValue);
            return null;
        });
    }

    public void changePassword(int id, String current, String newPassword, String confirm) throws AppException {
        ValidationUtil.requireNonBlank(current, "Current password");
        ValidationUtil.requireNonBlank(newPassword, "New password");
        ValidationUtil.validatePassword(newPassword);
        if (!newPassword.equals(confirm)) {
            throw new ValidationException("New password and confirmation do not match.");
        }
        Tx.write(c -> {
            Student s = studentDao.findById(c, id)
                    .orElseThrow(() -> new BusinessRuleException("Student account not found."));
            if (!PasswordUtil.verify(current, s.getPasswordHash())) {
                throw new AuthorizationException("Your current password is incorrect.");
            }
            studentDao.updatePassword(c, id, PasswordUtil.hash(newPassword));
            return null;
        });
    }

    /** Live numbers for the dashboard cards (keys keep insertion order). */
    public Map<String, Integer> dashboardStats(int studentId) throws AppException {
        return Tx.read(c -> {
            Map<String, Integer> stats = new LinkedHashMap<>();
            stats.put("totalBooks", bookDao.countByOwner(c, studentId));
            stats.put("availableBooks", bookDao.countByOwnerAndAvailability(c, studentId, Availability.AVAILABLE));
            stats.put("incoming", requestDao.countIncomingPending(c, studentId));
            stats.put("outgoing", requestDao.countOutgoingPending(c, studentId));
            stats.put("completed", requestDao.countCompleted(c, studentId));
            return stats;
        });
    }
}
