package com.may.simpleecommercesite.servlets;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.may.simpleecommercesite.annotations.Cookie;
import com.may.simpleecommercesite.annotations.Entity;
import com.may.simpleecommercesite.entityManager.DbContext;
import com.may.simpleecommercesite.entities.Cart;
import com.may.simpleecommercesite.entities.Customer;
import com.may.simpleecommercesite.helpers.ErrandBoy;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BaseServlet extends HttpServlet {
    //    protected Integer activeRequest=0;
    protected volatile boolean destroying;
    protected static final int maxThreads = 50;
    protected final Semaphore activeRequest = new Semaphore(maxThreads);
    protected DbContext dbContext;
    protected ObjectMapper jsonMapper;

    @Override
    public void init() throws ServletException {
        super.init();
        this.dbContext = (DbContext) getServletContext().getAttribute("DbContext");
        this.jsonMapper = (ObjectMapper) getServletContext().getAttribute("ObjectMapper");
    }

    @Override
    public void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (destroying) {
            resp.setStatus(503);
            resp.addHeader("Retry-After", "3");
            return;
        }
        startService();
        super.service(req, resp);
        finishService();
    }

    protected synchronized void startService() {
        synchronized (activeRequest) {
            try {
                activeRequest.acquire();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    protected synchronized void finishService() {
        synchronized (activeRequest) {
            activeRequest.release();
            activeRequest.notifyAll();
        }
    }

    @Override
    public void destroy() {
        destroying = true;
        synchronized (activeRequest) {
            while (activeRequest.availablePermits() != 50) {
                try {
                    activeRequest.wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        super.destroy();
    }

    public static javax.servlet.http.Cookie createGlobalCookie(String name, String value) {
        javax.servlet.http.Cookie cookie = new javax.servlet.http.Cookie(name, value);
        cookie.setMaxAge(24 * 60 * 60 * 14);
        cookie.setPath("/");
        return cookie;
    }

    public static String getCookieValue(Object entity) {
        String ret = null;
        Class<?> clazz = ErrandBoy.getRealClass(entity);
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(Cookie.class)) {
                Object value = null;
                try {
                    value = clazz.getDeclaredMethod("get" + ErrandBoy.firstLetterToUpperCase(field.getName())).invoke(entity);
                } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }
                if (field.getType().isAnnotationPresent(Entity.class)) ret = getCookieValue(value);
                else ret = value.toString();
            }
        }
        return ret;
    }
    public static Customer getCustomer(HttpServletRequest req){
        return (Customer) req.getSession().getAttribute(Customer.class.getSimpleName());
    }
    public static Map<String, String> getCookieValues(Object entity) {
        Map<String, String> ret = new HashMap<>();
        Class<?> clazz = ErrandBoy.getRealClass(entity);
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(Cookie.class)) {
                Object value;
                try {
                    value = clazz.getDeclaredMethod("get" + ErrandBoy.firstLetterToUpperCase(field.getName())).invoke(entity);
                } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }
                if (field.getType().isAnnotationPresent(Entity.class)) ret.putAll(getCookieValues(value));
                else ret.put(clazz.getSimpleName(), Objects.toString(value));
            }
        }
        return ret;
    }

    protected static int extractResourceId(String path, int index) {
        Matcher matcher = Pattern.compile("/(\\d+)(/.*/(\\d+))?").matcher(path);
//        for (int i=0 ; i<index; i++)
        matcher.find();
        return Integer.parseInt(matcher.group(index > 1 ? index + 1 : index));
    }

    public static int getCookieValue(HttpServletRequest req, Class<?> entity) {
        Class<?> clazz = ErrandBoy.getRealClass(entity);
        return Integer.parseInt(Arrays.stream(req.getCookies() != null ? req.getCookies() : new javax.servlet.http.Cookie[0]).filter(cookie -> cookie.getName().equals(clazz.getSimpleName()))
                .findFirst().orElseGet(() -> (javax.servlet.http.Cookie) req.getAttribute(clazz.getSimpleName() + "-Cookie")).getValue());
    }

    public void setNewCart(HttpServletRequest req, HttpServletResponse resp) throws SQLException {
        Cart cart = dbContext.save(new Cart());
        Customer customer = (Customer) req.getSession().getAttribute(Customer.class.getSimpleName());
        if (customer.getEmail() != null) {
            customer.setCart(cart);
            dbContext.save(customer);
        }
            Arrays.stream(req.getCookies()).filter(cookie -> cookie.getName().equals(Cart.class.getSimpleName()))
                    .forEach(cookie -> {
                        cookie.setValue(getCookieValue(cart));
                        resp.addCookie(cookie);
                    });
            req.removeAttribute(Cart.class.getSimpleName());
            req.getSession().setAttribute(Cart.class.getSimpleName(), cart);
    }
    enum SqlErrors{
        CART_ORDERED(45002),
        INVOICE_COMPLETE(45003),
        OMITTED_REQUIRED_CREDENTIAL(23502),
        COUPON_OWNER_MISMATCH(45005)
        ;
        private Integer SqlState;
        SqlErrors(int state){
            this.SqlState=state;
        }
        @Override
        public String toString() {
            return SqlState.toString();
        }
    }
}
