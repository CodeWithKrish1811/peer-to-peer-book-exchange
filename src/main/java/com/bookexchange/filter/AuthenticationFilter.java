package com.bookexchange.filter;

import com.bookexchange.util.HtmlUtil;
import com.bookexchange.util.SessionUtil;
import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/** Blocks every protected page unless a student is logged in (session-based authentication). */
@WebFilter(urlPatterns = {"/dashboard", "/books", "/explore", "/requests", "/history", "/profile"})
public class AuthenticationFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute(SessionUtil.STUDENT_ID) == null) {
            resp.sendRedirect(req.getContextPath() + "/login.html?error="
                    + HtmlUtil.urlEncode("Please log in to continue."));
            return;
        }
        // Stop the browser's back button from showing private pages after logout.
        resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        resp.setHeader("Pragma", "no-cache");
        chain.doFilter(request, response);
    }
}
