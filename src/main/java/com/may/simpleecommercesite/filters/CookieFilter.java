package com.may.simpleecommercesite.filters;

import com.may.simpleecommercesite.servlets.BaseServlet;
import com.may.simpleecommercesite.entityManager.DbContext;
import com.may.simpleecommercesite.entities.Cart;
import com.may.simpleecommercesite.entities.Customer;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Objects;

@WebFilter(servletNames = {"Cart", "User", "Invoice", "Login", "Product"}, filterName = "Cookie", asyncSupported = true)
public class CookieFilter extends HttpFilter {
    DbContext dbContext;
    private static final Class<?>[] cookieClasses={Cart.class, Customer.class};
    @Override
    public void init() throws ServletException {
        super.init();
        this.dbContext= (DbContext) getServletContext().getAttribute("DbContext");
    }

    @Override
    protected void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws IOException, ServletException {
        for (Class<?> cookieClass : cookieClasses) {
            Cookie cookie= addCookie(req, res, cookieClass);
            if (cookie!=null)
                req.setAttribute(cookieClass.getSimpleName() + "-Cookie", cookie);
        }
        chain.doFilter(req, res);
    }

    /***
     *
     * @param req
     * @param res
     * @param clazz
     * @return if cookie is added, returns the cookie. If cookie was already present and weren't added, then returns null.
     * @throws ServletException
     * @throws IOException
     */
    protected Cookie addCookie(HttpServletRequest req, HttpServletResponse res, Class<?> clazz) throws ServletException, IOException {
        String cookieName= clazz.getSimpleName();
        if((req.getCookies() == null || Arrays.stream(req.getCookies()).noneMatch(cookie -> cookie.getName().equals(clazz.getSimpleName()) && !Objects.equals(cookie.getValue(), "")))) {
            try {
                Object entity=dbContext.save(clazz.getConstructor().newInstance());
                Cookie cookie= BaseServlet.createGlobalCookie(cookieName, BaseServlet.getCookieValue(entity));
                res.addCookie(cookie);
                return cookie;
            } catch (SQLException | NoSuchMethodException | IllegalAccessException | InvocationTargetException |
                     InstantiationException e) {
                throw new RuntimeException(e);
            }
        } else return null;
    }
}
