package com.may.simpleecommercesite.apiServlets;


import com.fasterxml.jackson.databind.ObjectReader;
import com.may.simpleecommercesite.entities.RegisteredCustomer;
import com.may.simpleecommercesite.entities.Sale;
import com.may.simpleecommercesite.helpers.ErrandBoy;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

@WebServlet( urlPatterns = {"/api/login"}, name = "Login")
public class LoginApiServlet extends ApiServlet {
    ObjectReader userReader;

    @Override
    public void init() throws ServletException {
        super.init();
        userReader=this.jsonMapper.readerFor(RegisteredCustomer.class);
    }

    // body: {email: asdads@gmail.com, password: password123}
    // TODO Encrypt the cookies and user credentials.
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        Map<String, String> credentials= jsonMapper.readValue(req.getReader(), Map.class);
        if (req.getSession().getAttribute(RegisteredCustomer.class.getSimpleName())!=null) resp.sendRedirect("/");
        RegisteredCustomer customer=dbContext.findById(RegisteredCustomer.class, credentials.get("email"));
        if(customer==null){
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        // TODO DOES RESP.ADDCOOKIE OVERWRITE COOKIES?
        Map<String, String> cookieVal= getCookieValues(customer);
        if (req.getCookies()!=null)
            Arrays.stream(req.getCookies()).filter(cookie -> cookieVal.containsKey(cookie.getName()))
                    .peek(cookie -> cookie.setValue(cookieVal.get(cookie.getName())))
                    .forEach(resp::addCookie);
        else cookieVal.entrySet().forEach(entry->resp.addCookie(createGlobalCookie(entry.getKey(), entry.getValue())));
        req.getSession().setAttribute(RegisteredCustomer.class.getSimpleName(),customer);
//        resp.sendRedirect("/");
    }
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.getSession().removeAttribute(RegisteredCustomer.class.getSimpleName());
        req.getSession().removeAttribute(Sale.class.getSimpleName());
        req.logout();
//        resp.sendRedirect("/login");
    }
}
