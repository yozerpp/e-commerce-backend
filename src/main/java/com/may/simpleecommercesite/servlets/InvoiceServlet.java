package com.may.simpleecommercesite.servlets;

import com.fasterxml.jackson.databind.ObjectReader;
import com.may.simpleecommercesite.entities.Cart;
import com.may.simpleecommercesite.entities.Invoice;
import com.may.simpleecommercesite.entities.Customer;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@WebServlet(urlPatterns = {"/order", "/order/*"}, name = "Invoice",asyncSupported = true)
public class InvoiceServlet extends BaseServlet {
    ObjectReader invoiceReader;
    @Override
    public void init() throws ServletException {
        super.init();
        invoiceReader=this.jsonMapper.readerFor(Invoice.class);
    }
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Customer customer=(Customer)req.getSession().getAttribute(Customer.class.getSimpleName());
        String email=customer.getEmail()!=null?customer.getEmail():req.getParameter("email");
        if(email==null){
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        List<Invoice> results= dbContext.search(Invoice.class, Map.of("email",email));
        if(req.getParameter("email")==null)
            results=results.stream().filter(invoice -> invoice.getCustomer().getEmail()==null).toList();
        if (results.isEmpty())
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        else{
            resp.setContentType("application/json");
            jsonMapper.writeValue(resp.getWriter(), results);
        }
    }
// {unregEmail?: a@b.com, deliveryAddress: address object, paymentMethod: atdoor}
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Invoice invoice=invoiceReader.readValue(req.getReader());
        Customer customer=(Customer) req.getSession().getAttribute(Customer.class.getSimpleName());
        if (customer.getEmail()!=null)
            invoice.setEmail(customer.getEmail());
        else if (invoice.getEmail()==null){
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        invoice.setCustomer(customer);
        invoice.setCart((Cart) req.getSession().getAttribute(Cart.class.getSimpleName()));
        try {
            dbContext.save(invoice);
            setNewCart(req,resp);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        int invoiceId=extractResourceId(req.getPathInfo(), 1);
        try {
            Customer customer=getCustomer(req);
            Invoice oldInvoice= dbContext.findById(Invoice.class, invoiceId);
            if(oldInvoice==null){
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            Invoice invoice= invoiceReader.readValue(req.getReader());
            if( customer.getEmail()==null ){
                if(invoice.getEmail()==null)
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                else if(!Objects.equals(invoice.getEmail(), oldInvoice.getEmail()) || oldInvoice.getCustomer().getEmail()!=null) {
                    resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }
            } else if(oldInvoice.getCustomer()!=customer){
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
            invoice.setId(invoiceId);
            dbContext.update(invoice, false);
        } catch (SQLException e) {
            if (Objects.equals(e.getSQLState(), SqlErrors.INVOICE_COMPLETE.toString())) resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            else throw new RuntimeException(e);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        int invoiceId=extractResourceId(req.getPathInfo(), 1);
        Invoice invoice=new Invoice(invoiceId);
        invoice.setStatus(Invoice.Status.Canceled);
        if(invoice.getCustomer()!=getCustomer(req)){
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        try {
            dbContext.update(invoice, false);
        } catch (SQLException e) {
            if (Objects.equals(e.getSQLState(), SqlErrors.INVOICE_COMPLETE.toString())) resp.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
            else throw new RuntimeException(e);
        }
    }
}
