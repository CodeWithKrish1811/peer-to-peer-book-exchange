package com.bookexchange.servlet;

import com.bookexchange.exception.AppException;
import com.bookexchange.model.Student;
import com.bookexchange.service.StudentService;
import com.bookexchange.util.HtmlUtil;
import com.bookexchange.util.SessionUtil;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Authenticates a student and starts the session. */
@WebServlet("/login")
public class LoginServlet extends HttpServlet {
    private final StudentService studentService = new StudentService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.sendRedirect(req.getContextPath() + "/login.html");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String ctx = req.getContextPath();
        try {
            Student student = studentService.login(req.getParameter("email"), req.getParameter("password"));
            SessionUtil.login(req, student);
            resp.sendRedirect(ctx + "/dashboard");
        } catch (AppException e) {
            resp.sendRedirect(ctx + "/login.html?error=" + HtmlUtil.urlEncode(e.getMessage()));
        }
    }
}
