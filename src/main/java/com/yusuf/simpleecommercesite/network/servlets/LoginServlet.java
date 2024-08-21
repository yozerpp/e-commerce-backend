package com.yusuf.simpleecommercesite.network.servlets;


import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectReader;
import com.yusuf.simpleecommercesite.entities.Cart;
import com.yusuf.simpleecommercesite.entities.Customer;
import com.yusuf.simpleecommercesite.entities.IUser;
import com.yusuf.simpleecommercesite.entities.Seller;
import com.yusuf.simpleecommercesite.helpers.ErrandBoy;
import com.yusuf.simpleecommercesite.network.dtos.SearchResult;
import com.yusuf.simpleecommercesite.network.filters.CookieFilter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.yusuf.simpleecommercesite.network.servlets.ApiServlet.apiRoot;

@WebServlet( urlPatterns = { apiRoot+"/login/*"}, name = "Login",asyncSupported = true)
public class LoginServlet extends ApiServlet {
    ObjectReader userReader;
    @Override
    public void init() throws ServletException {
        super.init();
        userReader=this.jsonMapper.readerFor(Customer.class);
    }
    // body: {email: asdads@gmail.com, password: password123}
    // TODO Encrypt the cookies and user credentials.
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String email=req.getParameter("email");
        String password=req.getParameter("password");
        if(email==null || password==null){
            try {
                Customer customer = userReader.readValue(req.getReader());
                email = customer.getEmail();
                password = customer.getPassword();
            } catch(JsonMappingException e){
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
        }
        Map<String, Object> credentials = new HashMap<>();credentials.put("email", email);credentials.put("password", password);
        Class<? extends IUser> userType;
        if (req.getPathInfo().matches("/customer"))
            userType = Customer.class;
       else if (req.getPathInfo().matches("/seller"))
           userType = Seller.class;
       else {
           resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
           return;
        }

        IUser user= userType.cast( req.getSession().getAttribute(userType.getSimpleName()));
        if (user!=null && user.getEmail()!=null) {
//            resp.sendRedirect("/");
            return;
        }
        SearchResult<? extends IUser> results=  dbContext.search(userType, credentials);
        if(results.getCount()==0){
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        user=results.getData().get(0);
//        Map<String, String> cookieVal= getCookieValues(customer);
//        cookieVal.remove(Customer.class.getSimpleName());
//        if (req.getCookies()!=null)
//            Arrays.stream(req.getCookies()).filter(cookie -> cookieVal.containsKey(cookie.getName()))
//                    .peek(cookie -> cookie.setValue(cookieVal.get(cookie.getName())))
//                    .forEach(resp::addCookie);
//        else cookieVal.entrySet().forEach(entry->resp.addCookie(createGlobalCookie(entry.getKey(), entry.getValue())));
        req.getSession().setAttribute(userType.getSimpleName(),user);
        req.getSession().setAttribute("login", true);
        if (userType.equals(Seller.class)) CookieFilter.addCookie(req, resp, user);
        resp.setHeader("Location", getServletContext().getContextPath() + ErrandBoy.toRestLink(user));
//        resp.sendRedirect("/");
    }
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Class<? extends IUser> userType;
        if (req.getPathInfo().matches("/seller"))
            userType = Seller.class;
        else if (req.getPathInfo().matches("/customer"))
            userType = Customer.class;
        else {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        req.getSession().removeAttribute(userType.getSimpleName());
        req.getSession().setAttribute("login", false);
//        resp.sendRedirect("/login");
    }
}
