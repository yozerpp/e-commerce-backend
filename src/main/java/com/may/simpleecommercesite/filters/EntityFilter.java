package com.may.simpleecommercesite.filters;

import com.may.simpleecommercesite.servlets.BaseServlet;
import com.may.simpleecommercesite.entityManager.DbContext;
import com.may.simpleecommercesite.entities.Cart;
import com.may.simpleecommercesite.entities.Customer;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebFilter(servletNames = {"Cart", "Invoice", "User", "Login", "Product"} , filterName = "Entity", asyncSupported = true)
public class EntityFilter extends HttpFilter {
    DbContext dbContext;
    private static final Class<?>[] cookieClasses={Cart.class, Customer.class};
    @Override
    public void init() throws ServletException {
        super.init();
        this.dbContext= (DbContext) getServletContext().getAttribute("DbContext");
    }

    @Override
    protected void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws IOException, ServletException {
        for (Class<?> cls:cookieClasses)
            if(cls!= Customer.class || req.getSession().getAttribute("login")==null || !((boolean) req.getSession().getAttribute("login")))
                addSessionObject(cls, req);
        chain.doFilter(req, res);
    }
    public void addSessionObject(Class<?> clazz ,HttpServletRequest req) {
        if ( req.getSession().getAttribute(clazz.getSimpleName()) == null) {
            int cookieValue= BaseServlet.getCookieValue(req, clazz);
            req.getSession().setAttribute(clazz.getSimpleName(), dbContext.findById(clazz,  cookieValue));
        }
    }
}
