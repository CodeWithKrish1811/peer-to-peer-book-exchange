package com.bookexchange.servlet;

import com.bookexchange.exception.AppException;
import com.bookexchange.service.StudentService;
import com.bookexchange.util.HtmlUtil;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Handles the registration form (register.html posts here). */
@WebServlet("/register")
public class RegisterServlet extends HttpServlet {
    private final StudentService studentService = new StudentService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.sendRedirect(req.getContextPath() + "/register.html");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String ctx = req.getContextPath();
        try {
            studentService.register(req.getParameter("fullName"), req.getParameter("email"),
                    req.getParameter("studentId"), req.getParameter("password"), req.getParameter("confirmPassword"));
            resp.sendRedirect(ctx + "/login.html?success=" + HtmlUtil.urlEncode("Account created. You can log in now."));
        } catch (AppException e) {
            resp.sendRedirect(ctx + "/register.html?error=" + HtmlUtil.urlEncode(e.getMessage()));
        }
    }
}
