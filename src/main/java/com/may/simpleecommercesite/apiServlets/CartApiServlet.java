package com.may.simpleecommercesite.apiServlets;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.may.simpleecommercesite.entities.Cart;
import com.may.simpleecommercesite.entities.RegisteredCustomer;
import com.may.simpleecommercesite.entities.Sale;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

@WebServlet(urlPatterns = {"/api/cart"}, name = "Cart")
public class CartApiServlet extends ApiServlet {
    ObjectReader saleReader;

    @Override
    public void init() throws ServletException {
        super.init();
        saleReader = this.jsonMapper.readerFor(Sale.class);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        Cart cart = (Cart) req.getSession().getAttribute(Cart.class.getSimpleName());
        StringWriter out = new StringWriter();
        jsonMapper.writeValue(out, cart);
        jsonMapper.writeValue(resp.getWriter(), cart);
    }

    /* {productId: 1, quantity: 2, couponCode: ACS23}*/
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Cart cart = (Cart) req.getSession().getAttribute(Cart.class.getSimpleName());
        Sale newSale = saleReader.readValue(req.getReader());
        newSale.setCart(cart);
        List<Sale> sales = cart.getSales();
        sales.remove(newSale);
        try {
            dbContext.insert(newSale);
        } catch (SQLException e) {
            if(e.getErrorCode()==1062) {
                try {
                    dbContext.update(newSale, false);
                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
        sales.add(newSale);
        cart= (Cart) req.getSession().getAttribute(Cart.class.getSimpleName());
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) {
        Cart cart = (Cart) req.getSession().getAttribute(Cart.class.getSimpleName());
        dbContext.remove(cart);
        try {
            cart = dbContext.save(new Cart());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        RegisteredCustomer customer= (RegisteredCustomer) req.getSession().getAttribute(RegisteredCustomer.class.getSimpleName());
        if (customer!=null) {
            customer.setCart(cart);
            try {
                dbContext.save(customer);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        Cart finalCart = cart;
        Arrays.stream(req.getCookies()).filter(cookie -> cookie.getName().equals(Cart.class.getSimpleName()))
                .forEach(cookie -> {
                    cookie.setValue(getCookieValue(finalCart));
                    resp.addCookie(cookie);
                });
        req.getSession().setAttribute(Cart.class.getSimpleName(), cart);
    }
}