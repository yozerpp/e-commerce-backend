package com.yusuf.simpleecommercesite.network.servlets;

import com.fasterxml.jackson.databind.ObjectReader;
import com.yusuf.simpleecommercesite.entities.Cart;
import com.yusuf.simpleecommercesite.entities.Customer;
import com.yusuf.simpleecommercesite.helpers.ErrandBoy;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Objects;

import static com.yusuf.simpleecommercesite.network.servlets.ApiServlet.apiRoot;

@WebServlet(urlPatterns = {apiRoot + "/user"}, name = "User",asyncSupported = true)
public class UserServlet extends ApiServlet {
    ObjectReader userReader;

    @Override
    public void init() throws ServletException {
        super.init();
        this.userReader=this.jsonMapper.readerFor(Customer.class);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Customer customer= (Customer) req.getSession().getAttribute(Customer.class.getSimpleName());
        if(customer.getEmail()==null) {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        }else {
            resp.setContentType("application/json");
            jsonMapper.writeValue(resp.getWriter(), customer);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Customer customer= userReader.readValue(req.getReader());
        Cart cart= (Cart) req.getSession().getAttribute(Cart.class.getSimpleName());
        if(cart!=null)
            customer.setCart(cart);
        else {
            try {
                customer.setCart(dbContext.save(new Cart()));
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        if (!ErrandBoy.validateCredentialFormat(customer.getEmail(), customer.getPassword())){
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Email should be a valid email format and password should contain numbers, lower and uppercase characters and be longer than 12 characters.");
            return;
        }
        try{
            dbContext.save(customer);
        } catch (SQLException e) {
            if (e.getErrorCode()==1062){
                resp.setStatus(HttpServletResponse.SC_CONFLICT);
            } else if(Objects.equals(e.getSQLState(), SqlErrors.OMITTED_REQUIRED_CREDENTIAL.toString())){
                resp.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
            }
            else throw new RuntimeException(e);
        }
    }

    // {email: a@gmail.con, credential: addrq, firstName: someOne, lastName: asdasd ...}
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Customer customer= (Customer) req.getSession().getAttribute(Customer.class.getSimpleName());
        if(customer==null || customer.getEmail()==null){
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        Customer newCustomer= userReader.readValue(req.getReader());
        boolean valid=true;
        if(newCustomer.getEmail()!=null)
            valid &=ErrandBoy.validateCredentialFormat(newCustomer.getEmail(),null);
        else if(newCustomer.getPassword()!=null)
            valid &=ErrandBoy.validateCredentialFormat(null, newCustomer.getPassword());
        if(!valid){
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        newCustomer.setId(customer.getId());
        try {
            dbContext.update(newCustomer,false);
            newCustomer= dbContext.findById(Customer.class,newCustomer.getId());
            req.getSession().setAttribute(Customer.class.getSimpleName(),newCustomer);
        } catch (SQLException e) {
            if(e.getSQLState()==SqlErrors.OMITTED_REQUIRED_CREDENTIAL.toString())
                resp.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
            else throw new RuntimeException(e);
        }
    }
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) {
        Customer customer = (Customer) req.getSession().getAttribute(Customer.class.getSimpleName());
        if (customer.getEmail()==null){
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        customer.setEmail(null);
        customer.setPassword(null);
        try {
            dbContext.update(customer, true);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        req.getSession().removeAttribute(Customer.class.getSimpleName());
        req.getSession().setAttribute("login", false);
    }
}
