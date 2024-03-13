package com.may.simpleecommercesite.filters;

import com.may.simpleecommercesite.annotations.Id;
import com.may.simpleecommercesite.apiServlets.ApiServlet;
import com.may.simpleecommercesite.beans.DbContext;
import com.may.simpleecommercesite.entities.Cart;
import com.may.simpleecommercesite.entities.RegisteredCustomer;
import com.may.simpleecommercesite.helpers.ErrandBoy;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@WebFilter(servletNames = {"Cart", "Invoice", "User", "Login"} , filterName = "Entity", asyncSupported = true)
public class EntityFilter extends HttpFilter {
    DbContext dbContext;
    private static final Class<?>[] sessionObjectClasses={Cart.class, RegisteredCustomer.class};
    @Override
    public void init() throws ServletException {
        super.init();
        this.dbContext= (DbContext) getServletContext().getAttribute("DbContext");
    }

    @Override
    protected void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws IOException, ServletException {
        addSessionObject(Cart.class, req);
        chain.doFilter(req, res);
    }
    public void addSessionObject(Class<?> clazz ,HttpServletRequest req) {
        if ( req.getSession().getAttribute(clazz.getSimpleName()) == null) {
            int cookieValue= ApiServlet.getCookieValue(req, clazz);
            req.getSession().setAttribute(clazz.getSimpleName(), dbContext.findById(clazz,  cookieValue));
        }
    }
}
