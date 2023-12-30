package com.may.simpleecommercesite.apiServlets;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.may.simpleecommercesite.annotations.Cookie;
import com.may.simpleecommercesite.beans.DBService;
import com.may.simpleecommercesite.beans.EntityFactory;
import com.may.simpleecommercesite.entities.Cart;
import com.may.simpleecommercesite.entities.Entity;
import com.may.simpleecommercesite.entities.Sale;
import com.may.simpleecommercesite.filters.ExtendedHttpRequest;
import com.may.simpleecommercesite.helpers.Json;

import javax.servlet.AsyncContext;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

@WebServlet(urlPatterns = {"/api/cart"}, asyncSupported = true, name = "Cart")
public class CartApiServlet extends ApiServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        resp.setContentType("application/json");
        AsyncContext asyncContext = req.startAsync();
        asyncContext.start(new Runnable() {
            @Override
            public void run() {
                try {
                    DBService dbService = new DBService(dataSource);
                    List<Sale> cart = (List<Sale>) ((HttpServletRequest) asyncContext.getRequest()).getSession().getAttribute(Sale.class.getSimpleName());
                    PrintWriter out = asyncContext.getResponse().getWriter();
                    new ObjectMapper().writeValue(out, cart);
                    dbService.destroy();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                asyncContext.complete();
            }
        });
    }

    /* {productId: 1, quantity: 2, couponCode: ACS23}*/
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        AsyncContext asyncContext = req.startAsync();
        asyncContext.start(new Runnable() {
            @Override
            public void run() {
                try {
                    DBService service = new DBService(dataSource);
                    List<Sale> sales = (List<Sale>) ((HttpServletRequest) asyncContext.getRequest()).getSession().getAttribute(Sale.class.getSimpleName());
                    Sale newSale = (Sale) Json.instantiateFromJson(asyncContext.getRequest().getReader(), Sale.class);
                    newSale.setCartId(EntityFactory.cart(Integer.parseInt(Arrays.stream(((HttpServletRequest) asyncContext.getRequest()).getCookies()).filter(cookie -> cookie.getName().equals(Entity.getAnnotatedFields(Cart.class, Cookie.class).get(0).getName())).findFirst().get().getValue())));
                    newSale.persist();
                    sales.add(newSale);
//                    ((HttpServletRequest) asyncContext.getRequest()).getSession().setAttribute(Sale.class.getSimpleName(), sales);
                    ((HttpServletResponse) asyncContext.getResponse()).setStatus(HttpServletResponse.SC_CREATED);
                    service.destroy();
                    asyncContext.complete();
                } catch (SQLException | IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) {
        List<Sale> sales = (List<Sale>) req.getSession().getAttribute(Sale.class.getSimpleName());
        sales.clear();
    }

    /*{productId:1, quantity: 2, couponCode: null}*/
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) {
        AsyncContext asyncContext = req.startAsync();
        asyncContext.start(new Runnable() {
            @Override
            public void run() {
                try {
                    DBService service = new DBService(dataSource);
                    List<Sale> sales = (List<Sale>) ((HttpServletRequest) asyncContext.getRequest()).getSession().getAttribute(Sale.class.getSimpleName());
                    Sale newSale = (Sale) Json.instantiateFromJson(asyncContext.getRequest().getReader(), Sale.class);
                    Sale oldSale = sales.stream().filter(sale -> sale.getProductId().getProductId() == newSale.getProductId().getProductId()).findFirst().get();
                    if(newSale.getQuantity()!=0){
                        oldSale.setCouponCode(newSale.getCouponCode());
                        oldSale.setQuantity(newSale.getQuantity());
                        oldSale.commit();
                    } else {
                        oldSale.remove();
                    }
                    ((HttpServletResponse) asyncContext.getResponse()).setStatus(HttpServletResponse.SC_ACCEPTED);
                    service.destroy();
                    asyncContext.complete();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }
}