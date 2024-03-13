package com.may.simpleecommercesite.apiServlets;

import com.fasterxml.jackson.databind.ObjectReader;
import com.may.simpleecommercesite.entities.Cart;
import com.may.simpleecommercesite.entities.RegisteredCustomer;
import com.may.simpleecommercesite.helpers.ErrandBoy;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
@WebServlet(urlPatterns = {"/api/user"}, name = "User")
public class UserApiServlet extends ApiServlet {
    ObjectReader userReader;

    @Override
    public void init() throws ServletException {
        super.init();
        this.userReader=this.jsonMapper.readerFor(RegisteredCustomer.class);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        RegisteredCustomer customer= (RegisteredCustomer) req.getSession().getAttribute(RegisteredCustomer.class.getSimpleName());
        resp.setContentType("application/json");
        jsonMapper.writeValue(resp.getWriter(), customer);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        RegisteredCustomer customer= userReader.readValue(req.getReader());
        try {
            customer.setCart(dbContext.save(new Cart()));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        if (!ErrandBoy.validateCredentialFormat(customer.getEmail(), customer.getCredential())){
            resp.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
            return;
        }
        try{
            dbContext.insert(customer);
            resp.sendRedirect("/login");
        } catch (SQLException e) {
            if (e.getErrorCode()==1062){
                resp.setStatus(HttpServletResponse.SC_CONFLICT);
            }
            else throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // {email: a@gmail.con, credential: addrq, firstName: someOne, lastName: asdasd ...}
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        RegisteredCustomer customer= userReader.readValue(req.getReader());
        try {
            dbContext.save(customer);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        resp.setStatus(HttpServletResponse.SC_ACCEPTED);
    }
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        RegisteredCustomer customer = (RegisteredCustomer) req.getSession().getAttribute(RegisteredCustomer.class.getSimpleName());
        if (customer ==null) resp.sendRedirect("/login");
        dbContext.remove(customer);
        try {
            req.logout();
            resp.sendRedirect("/");
        } catch (ServletException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}
