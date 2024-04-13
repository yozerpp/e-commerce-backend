package com.yusuf.simpleecommercesite.network.filters;

import com.yusuf.simpleecommercesite.entities.Customer;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;

@WebFilter(servletNames = {"User"}, filterName = "Login", asyncSupported = true)
public class AuthorizationFilter extends HttpFilter {
    @Override
    protected void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws IOException, ServletException {
        if ((req.getSession().getAttribute(Customer.class.getSimpleName())!=null && ((Customer)req.getSession().getAttribute(Customer.class.getSimpleName())).getEmail()!=null ) ||
                (Objects.equals(req.getServletPath(), "/api/user") && Objects.equals(req.getMethod(), "POST"))){
            chain.doFilter(req,res);
        } else res.setStatus(401);
    }
}
