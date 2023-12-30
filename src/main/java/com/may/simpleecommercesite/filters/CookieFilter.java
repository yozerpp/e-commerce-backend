package com.may.simpleecommercesite.filters;

import com.may.simpleecommercesite.beans.DBService;
import com.may.simpleecommercesite.entities.Cart;
import com.may.simpleecommercesite.entities.Customer;
import com.may.simpleecommercesite.entities.Entity;
import com.may.simpleecommercesite.entities.RegisteredCustomer;
import com.may.simpleecommercesite.helpers.ErrandBoy;

import javax.servlet.AsyncContext;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.http.HttpRequest;
import java.util.Arrays;
import java.util.Map;

@WebFilter(urlPatterns = {"/api/cart", "/api/invoice", "/api/user"}, filterName = "Cookie", asyncSupported = true)
public class CookieFilter extends DatabaseFilter{
    @Override
    protected void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws IOException, ServletException {
        Class<?> clazz;
        if (!(req instanceof ExtendedHttpRequest)) req=new ExtendedHttpRequest(req);
        if (req.getServletPath().equals("/api/cart") | req.getServletPath().equals("/api/invoice")){
            addCookie(req, res, chain, Cart.class);
        } else{
            addCookie(req, res, chain, Customer.class);
        }

        chain.doFilter(req, res);
    }
    protected void addCookie(HttpServletRequest req, HttpServletResponse res, FilterChain chain, Class<?> clazz) throws ServletException, IOException {
        String cookieName= Entity.getAnnotatedFields(clazz, com.may.simpleecommercesite.annotations.Id.class).get(0).getName();
        if(((ExtendedHttpRequest) req).getCookie(cookieName).isEmpty()) {
            DBService service = new DBService(dataSource);
            Map.Entry<String, Object> cookieEntry = service.newEntity(clazz);
            Cookie cookie = ErrandBoy.createGlobalCookie(cookieEntry.getKey(),  cookieEntry.getValue().toString());
            ((ExtendedHttpRequest) req).addCookie(cookie);
            ((HttpServletResponse) res).addCookie(cookie);
            service.destroy();
        }
    }
}
