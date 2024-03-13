package com.may.simpleecommercesite.filters;

import com.may.simpleecommercesite.entities.RegisteredCustomer;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebFilter(urlPatterns = {"/user", "/user/*"})
public class AuthorizationFilter extends HttpFilter {
    @Override
    protected void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws IOException, ServletException {
        if (req.getSession().getAttribute(RegisteredCustomer.class.getSimpleName())==null){
            getServletContext().getRequestDispatcher("/login").forward(req, res);
        }
    }
}
