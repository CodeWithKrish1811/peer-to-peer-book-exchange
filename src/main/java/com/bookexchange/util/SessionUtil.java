package com.bookexchange.util;

import com.bookexchange.model.Student;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/** Session helpers: login state and one-time "flash" notifications. */
public final class SessionUtil {
    public static final String STUDENT_ID = "studentId";
    private static final String STUDENT_NAME = "studentName";
    private static final String FLASH_TYPE = "flashType";
    private static final String FLASH_MESSAGE = "flashMessage";

    private SessionUtil() { }

    /** Starts a fresh session on login (prevents session fixation). */
    public static void login(HttpServletRequest req, Student student) {
        HttpSession old = req.getSession(false);
        if (old != null) {
            old.invalidate();
        }
        HttpSession session = req.getSession(true);
        session.setAttribute(STUDENT_ID, student.getId());
        session.setAttribute(STUDENT_NAME, student.getFullName());
    }

    public static void logout(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    /** The logged-in student's id. Protected pages are behind AuthenticationFilter, so it is always set. */
    public static int studentId(HttpServletRequest req) {
        return (Integer) req.getSession().getAttribute(STUDENT_ID);
    }

    public static String studentName(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        Object name = session == null ? null : session.getAttribute(STUDENT_NAME);
        return name == null ? "" : name.toString();
    }

    public static void updateName(HttpServletRequest req, String name) {
        req.getSession().setAttribute(STUDENT_NAME, name);
    }

    /** type is "success" or "error". */
    public static void flash(HttpServletRequest req, String type, String message) {
        HttpSession session = req.getSession();
        session.setAttribute(FLASH_TYPE, type);
        session.setAttribute(FLASH_MESSAGE, message);
    }

    /** Returns {type, message} once, then clears it; null if there is none. */
    public static String[] popFlash(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute(FLASH_MESSAGE) == null) {
            return null;
        }
        String[] flash = {String.valueOf(session.getAttribute(FLASH_TYPE)),
                String.valueOf(session.getAttribute(FLASH_MESSAGE))};
        session.removeAttribute(FLASH_TYPE);
        session.removeAttribute(FLASH_MESSAGE);
        return flash;
    }
}
