package com.may.simpleecommercesite.apiServlets;

import com.fasterxml.jackson.core.JsonFactory;
import com.may.simpleecommercesite.annotations.Id;
import com.may.simpleecommercesite.beans.DBService;
import com.may.simpleecommercesite.beans.EntityFactory;
import com.may.simpleecommercesite.entities.Customer;
import com.may.simpleecommercesite.entities.Entity;
import com.may.simpleecommercesite.entities.RegisteredCustomer;
import com.may.simpleecommercesite.filters.ExtendedHttpRequest;
import com.may.simpleecommercesite.helpers.ErrandBoy;
import com.may.simpleecommercesite.helpers.Json;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
@WebServlet(urlPatterns = {"/api/user"}, asyncSupported = true, name = "User")
public class UserApiServlet extends ApiServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        RegisteredCustomer customer= (RegisteredCustomer) req.getSession().getAttribute(RegisteredCustomer.class.getSimpleName());
        resp.setContentType("application/json");
        AsyncContext asyncContext= req.startAsync();
        asyncContext.start(new Runnable() {
            @Override
            public void run() {
                DBService service=new DBService(dataSource);
                try {
                    Json.serializeObject(customer, resp.getWriter());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } finally {
                    service.destroy();
                    asyncContext.complete();
                }
            }
        });
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Map<String, Object> customerMap=Json.parseJson(req.getReader());
        if (!ErrandBoy.validateCredentialFormat(customerMap)){
            resp.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
            return;
        }
        int cookieId= Integer.parseInt(((ExtendedHttpRequest)req).getCookie(Customer.class.getSimpleName()).get().getValue());
        RegisteredCustomer newCustomer= (RegisteredCustomer) Json.instantiateFromMap(customerMap, RegisteredCustomer.class);
        AsyncContext asyncContext= req.startAsync();
        asyncContext.start(new Runnable() {
            @Override
            public void run() {
                DBService service=new DBService(dataSource);
                try {
                    newCustomer.setCookieId(cookieId);
                    newCustomer.persist();
                    ((HttpServletResponse) asyncContext.getResponse()).sendRedirect("/login");
                } catch (SQLException e) {
                    if (e.getErrorCode()==1062){
                        resp.setStatus(HttpServletResponse.SC_CONFLICT);
                    }
                    else throw new RuntimeException(e);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } finally {
                    service.destroy();
                    asyncContext.complete();
                }
            }
        });
    }

    // {email: a@gmail.con, credential: addrq, firstName: someOne, lastName: asdasd ...}
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Map<String, Object> updatedValues= Json.parseJson(req.getReader());
        RegisteredCustomer customer = (RegisteredCustomer) req.getSession().getAttribute(RegisteredCustomer.class.getSimpleName());
        AsyncContext asyncContext=req.startAsync();
        asyncContext.start(new Runnable() {
            @Override
            public void run() {
                DBService service=new DBService(dataSource);
                Entity.invokeSetters(customer, updatedValues);
                customer.commit();
                resp.setStatus(HttpServletResponse.SC_ACCEPTED);
                asyncContext.complete();
            }
        });
    }
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        AsyncContext asyncContext=req.startAsync();
        RegisteredCustomer customer= (RegisteredCustomer) req.getSession().getAttribute(RegisteredCustomer.class.getSimpleName());
        asyncContext.start(new Runnable() {
            @Override
            public void run() {
                DBService service=new DBService(dataSource);
                customer.remove();
                try {
                    ((HttpServletRequest)asyncContext.getRequest()).logout();
                    resp.sendRedirect("/");
                } catch (ServletException | IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }
}
