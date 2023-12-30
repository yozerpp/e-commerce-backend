package com.may.simpleecommercesite.filters;

import com.may.simpleecommercesite.annotations.Cookie;
import com.may.simpleecommercesite.annotations.Id;
import com.may.simpleecommercesite.beans.DBService;
import com.may.simpleecommercesite.entities.Cart;
import com.may.simpleecommercesite.entities.Entity;
import com.may.simpleecommercesite.entities.RegisteredCustomer;
import com.may.simpleecommercesite.entities.Sale;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@WebFilter(urlPatterns = {"/api/cart", "/api/invoice", "/api/user"} , filterName = "Entity",asyncSupported = true)
public class EntityFilter extends DatabaseFilter {

    @Override
    protected void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws IOException, ServletException {
        String servletPath=req.getServletPath();
        String cookieName;
        Class<?> entity;
        if (!(req instanceof ExtendedHttpRequest)) req=new ExtendedHttpRequest(req);
        if (servletPath.equals("/api/cart") | servletPath.equals("/api/invoice")) {
            cookieName= Entity.getAnnotatedFields(Cart.class, Id.class).get(0).getName();
            entity=Sale.class;
        } else{
            Principal user=req.getUserPrincipal();
            if (user==null) {
                chain.doFilter(req, res);
                return;
            }
            cookieName=user.getName();
            entity=RegisteredCustomer.class;
        }
        addSessionObject(entity, cookieName, req, res , chain);
        chain.doFilter(req, res);
    }
    public void addSessionObject(Class<?> clazz, String cookieName ,HttpServletRequest req, HttpServletResponse resp, FilterChain chain) throws ServletException, IOException {
        String entityName= clazz.getSimpleName();
        if ( req.getSession().getAttribute(entityName) == null) {
            Map<String, Object> params = Map.of(cookieName, ((ExtendedHttpRequest) req).getCookie(cookieName).get().getValue());
            DBService service = new DBService(dataSource);
            List<Entity> entities;
            if((entities = (List<Entity>) service.byFields(clazz, params, false))==null) entities=new ArrayList<>();
            if (entities.size()==1) ((HttpServletRequest)req).getSession().setAttribute(entityName,entities.get(0));
            else ((HttpServletRequest)req).getSession().setAttribute(entityName,entities );
        }
    }
}
