package com.may.simpleecommercesite.apiServlets;

import com.fasterxml.jackson.databind.ObjectReader;
import com.may.simpleecommercesite.entities.Cart;
import com.may.simpleecommercesite.entities.Invoice;
import com.may.simpleecommercesite.entities.RegisteredCustomer;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@WebServlet(urlPatterns = {"/api/invoice", "/api/invoice/*"}, name = "Invoice")
public class InvoiceApiServlet extends ApiServlet {
    ObjectReader invoiceReader;

    @Override
    public void init() throws ServletException {
        super.init();
        invoiceReader=this.jsonMapper.readerFor(Invoice.class);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Map<String, Object> credentials=getOrdererEmail(req);
        resp.setContentType("application/json");
        List<Invoice> results= dbContext.search(Invoice.class, credentials);
        if (results.isEmpty())
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        else
            jsonMapper.writeValue(resp.getWriter(), results);
    }
// {unregEmail?: a@b.com, deliveryAddress: address object, paymentMethod: atdoor}
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Invoice invoice=invoiceReader.readValue(req.getReader());
        if (req.getSession().getAttribute(RegisteredCustomer.class.getSimpleName())!=null)
            invoice.setCustomer((RegisteredCustomer) req.getSession().getAttribute(RegisteredCustomer.class.getSimpleName()));
        invoice.setCart((Cart) req.getSession().getAttribute(Cart.class.getSimpleName()));
        try {
            dbContext.insert(invoice);
            Cart cart = dbContext.save(new Cart());
            Arrays.stream(req.getCookies()).filter(cookie -> cookie.getName().equals(Cart.class.getSimpleName()))
                            .peek(cookie -> cookie.setValue(getCookieValue(cart))).forEach(resp::addCookie);
            req.setAttribute(Cart.class.getSimpleName(), cart);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        int invoiceId=extractResourceId(req.getPathInfo(), 1);
        try {
            Invoice invoice= invoiceReader.readValue(req.getReader());
            invoice.setInvoiceId(invoiceId);
            dbContext.update(invoice, false);
        } catch (SQLException e) {
            if (Objects.equals(e.getSQLState(), "45001")) resp.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
            else if (Objects.equals(e.getSQLState(), "45002")) resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            else throw new RuntimeException(e);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        int invoiceId=extractResourceId(req.getPathInfo(), 1);
        Invoice invoice=new Invoice(invoiceId);
        invoice.setInvoiceStatus(Invoice.Status.Canceled);
        try {
            dbContext.update(invoice, false);
            resp.setStatus(HttpServletResponse.SC_ACCEPTED);
        } catch (SQLException e) {
            if (Objects.equals(e.getSQLState(), "45001")) resp.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
            else throw new RuntimeException(e);
        }
    }
    private static Map<String, Object> getOrdererEmail(HttpServletRequest req){
        RegisteredCustomer user= (RegisteredCustomer) req.getSession().getAttribute(RegisteredCustomer.class.getSimpleName());
        if (user==null){
            return Map.of("unregEmail",req.getParameter("email"));
        } else return Map.of("email", user.getEmail());
    }
}
