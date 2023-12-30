package com.may.simpleecommercesite.apiServlets;

import com.may.simpleecommercesite.beans.DBService;
import com.may.simpleecommercesite.beans.EntityFactory;
import com.may.simpleecommercesite.entities.Cart;
import com.may.simpleecommercesite.entities.Invoice;
import com.may.simpleecommercesite.entities.RegisteredCustomer;
import com.may.simpleecommercesite.filters.ExtendedHttpRequest;
import com.may.simpleecommercesite.helpers.Json;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
@WebServlet(urlPatterns = {"/api/invoice"}, asyncSupported = true, name = "Invoice")
public class InvoiceApiServlet extends ApiServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Map<String, Object> credentials=getOrdererEmail(req, resp);
        resp.setContentType("application/json");
        AsyncContext asyncContext= req.startAsync();
        asyncContext.start(new Runnable() {
            @Override
            public void run() {
                DBService service=new DBService(dataSource);
                Invoice invoice= (Invoice) service.byFields(Invoice.class, credentials, false);
                if (invoice!=null){
                    try {
                        Json.serializeObject(invoice, ((HttpServletResponse) asyncContext.getResponse()).getWriter());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                } else resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            }
        });
    }
// {unregEmail?: a@b.com, deliveryAddress: address object, paymentMethod: atdoor, card: cardObject}
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        AsyncContext asyncContext=req.startAsync();
        asyncContext.start(new Runnable() {
            @Override
            public void run() {
                DBService service=new DBService(dataSource);
                ExtendedHttpRequest req=(ExtendedHttpRequest) asyncContext.getRequest();
                HttpServletResponse resp=(HttpServletResponse) asyncContext.getResponse();
                try{
                    Invoice invoice= null;
                    invoice = (Invoice) Json.instantiateFromJson(req.getReader(), Invoice.class);
                    invoice.setCartId((Cart) EntityFactory.cart(Integer.parseInt(req.getCookie(Cart.class.getSimpleName()).get().getValue())));
                    if(invoice.getUnregEmail()==null){
                        RegisteredCustomer customer;
                        if((customer=(RegisteredCustomer) req.getSession().getAttribute(RegisteredCustomer.class.getSimpleName()))==null){
                            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                            asyncContext.complete();
                        }
                        invoice.setEmail(customer);
                    }
                    invoice.commit();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } finally {
                    service.destroy();
                }
                asyncContext.complete();
            }
        });
    }
    //{invoiceId:1 ,deliveryAddres: addressObject, status:canceled, paymentMethod: }
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        AsyncContext asyncContext=req.startAsync();
        asyncContext.start(new Runnable() {
            @Override
            public void run() {
                Invoice invoice = null;
                try {
                    invoice = (Invoice) Json.instantiateFromJson(asyncContext.getRequest().getReader(), Invoice.class);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                Invoice oldInvoice= (Invoice) new Invoice(invoice.getInvoiceId()).fetch();
                // ugly code
                if (oldInvoice.getInvoiceStatus()!= Invoice.Status.InProgress) {
                    resp.setStatus(HttpServletResponse.SC_CONFLICT);
                    try {
                        asyncContext.getResponse().getWriter().print("{\"error\":1}");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                } else if (oldInvoice.getPaymentMethod()== Invoice.PaymentMethod.onlineCard && invoice.getPaymentMethod()!= Invoice.PaymentMethod.onlineCard){
                    resp.setStatus(HttpServletResponse.SC_CONFLICT);
                    try {
                        asyncContext.getResponse().getWriter().print("{\"error\":2}");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    invoice.commit();
                    resp.setStatus(HttpServletResponse.SC_ACCEPTED);
                }
                asyncContext.complete();
            }
        });
    }

    private static Map<String, Object> getOrdererEmail(HttpServletRequest req, HttpServletResponse resp){
        String email=req.getUserPrincipal().getName();
        if (email==null){
            return Map.of("unregEmail",req.getParameter("email"));
        } else return Map.of("email", email);
    }
}
