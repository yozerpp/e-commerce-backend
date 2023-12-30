package com.may.simpleecommercesite.apiServlets;


import com.may.simpleecommercesite.beans.DBService;
import com.may.simpleecommercesite.beans.EntityFactory;
import com.may.simpleecommercesite.entities.Cart;
import com.may.simpleecommercesite.entities.Entity;
import com.may.simpleecommercesite.entities.RegisteredCustomer;
import com.may.simpleecommercesite.entities.Sale;
import com.may.simpleecommercesite.filters.CookieFilter;
import com.may.simpleecommercesite.helpers.ErrandBoy;
import com.may.simpleecommercesite.helpers.Json;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

@WebServlet( urlPatterns = {"/api/login"}, asyncSupported = true)
public class LoginApiServlet extends ApiServlet {
    // body: {email: asdads@gmail.com, password: password123}
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Map<String, Object> credentials= Json.parseJson(req.getReader());
        if (req.getSession().getAttribute(RegisteredCustomer.class.getSimpleName())!=null) resp.sendRedirect("/");
        AsyncContext asyncContext=req.startAsync();
        asyncContext.start(new Runnable() {
            @Override
            public void run() {
                DBService service;
                HttpServletResponse resp=(HttpServletResponse) asyncContext.getResponse();
                HttpServletRequest req=(HttpServletRequest) asyncContext.getRequest();
                service =new DBService(dataSource);
                // Encrypt the cookies and user credentials.
                RegisteredCustomer customer= null;
                customer = (RegisteredCustomer) service.byFields(RegisteredCustomer.class, Map.of("email", credentials.get("email"), "credential" , credentials.get("credential")), false).get(0);
                if(customer==null){
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    asyncContext.complete();
                }
                try {
                    req.login((String) credentials.get("email"), (String) credentials.get("credential"));
                } catch (ServletException e) {
                    throw new RuntimeException(e);
                }
                RegisteredCustomer finalCustomer = (RegisteredCustomer) customer.fetch();
                    Entity.getAnnotatedFields(RegisteredCustomer.class,com.may.simpleecommercesite.annotations.Cookie.class).stream()
                            .map(field -> {
                                try {
                                    return new Cookie(field.getName(),finalCustomer.getClass().getDeclaredMethod("get" +ErrandBoy.firstLetterToUpperCase(field.getName()), field.getType()).invoke(finalCustomer).toString());
                                } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                                    throw new RuntimeException(e);
                                }
                            })
                            .forEach(resp::addCookie);
                req.getSession().setAttribute(RegisteredCustomer.class.getSimpleName(),customer);
                try {
                    resp.sendRedirect("/");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                asyncContext.complete();
            }
        });
    }
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.getSession().removeAttribute(RegisteredCustomer.class.getSimpleName());
        req.getSession().removeAttribute(Sale.class.getSimpleName());
        req.logout();
        resp.sendRedirect("/login");
    }
}
