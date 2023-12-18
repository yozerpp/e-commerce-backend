package com.may.simpleecommercesite.apiServlets;


import com.may.simpleecommercesite.beans.DBService;
import com.may.simpleecommercesite.helpers.ResultSetJsonConverter;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import javax.annotation.Resource;
import javax.json.Json;
import javax.json.stream.JsonParser;
import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import javax.sql.DataSource;
import javax.sql.RowSet;
import javax.sql.rowset.CachedRowSet;
import java.io.CharArrayReader;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@WebServlet(urlPatterns = {"/api/cart"}, asyncSupported = true)
public class CartApiServlet extends BaseApiServlet{
    DBService dbService;
    @Resource(name = "java:comp/env/jdbc/pool/test")
    DataSource dataSource;
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        try {
            this.dbService=new DBService(dataSource, "root", "Yusuf_2002");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        ResultSet cart=createCartIfNotExists(req, resp);
        resp.setContentType("application/json");
        AsyncContext asyncContext= req.startAsync();
        asyncContext.start(new Runnable() {
            @Override
            public void run() {
                ResultSet cart= (ResultSet) asyncContext.getRequest().getAttribute("cart");
                try {
                    asyncContext.getResponse().getWriter().print(ResultSetJsonConverter.cart(cart));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                asyncContext.complete();
            }
        });
    }

    /**
     * {productId: 1, quantity: 3, CouponCode:AXV13}
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException{
        ResultSet cart=createCartIfNotExists(req, resp);
        char[] buf = new char[64];
        req.getReader().read(buf);
        int productId = 0;
        int quantity = 0;
        String cc = null;
        JsonParser parse=Json.createParser(new CharArrayReader(buf));
        while (parse.hasNext()){
                if (parse.next()== JsonParser.Event.KEY_NAME) {
                switch (parse.getString()){
                    case "productId":
                        parse.next();
                        productId= parse.getInt();
                        break;
                    case "quantity":
                        parse.next();
                        quantity=parse.getInt();
                        break;
                    case "CouponCode":
                        parse.next();
                        cc=parse.getString();
                        break;
                }
            }
        }
           if (dbService.insertSale(createCookieIfNotExists(req, resp), productId, quantity, cc)!=0)
               resp.setStatus(HttpServletResponse.SC_CREATED);
           else
               resp.setStatus(HttpServletResponse.SC_EXPECTATION_FAILED);
    }
    RowSet createCartIfNotExists(HttpServletRequest req, HttpServletResponse resp){
        RowSet cart;
        if ((cart = (RowSet) req.getSession().getAttribute("cart"))==null){
            int cartId= createCookieIfNotExists(req, resp);
            cart= dbService.cartById(cartId);
            req.setAttribute("cart", cart);
        }
        return cart;
    }
    int createCookieIfNotExists(HttpServletRequest req, HttpServletResponse resp){
    int cartId;
    Cookie[] activeCookies=req.getCookies();
        Cookie cookie;
        if (Arrays.stream(activeCookies).noneMatch((c -> c.getName()=="cart"))){
            cartId=dbService.newCart();
            cookie=new Cookie ("cart", Integer.toString(cartId));
            cookie.setMaxAge(60*60);
            resp.addCookie(cookie);
        }
        else {
            cookie= Arrays.stream(activeCookies).filter(c -> c.getName()=="cart").findFirst().get();
            cartId= Integer.parseInt(cookie.getValue());
        }
        return cartId;
    }
}
