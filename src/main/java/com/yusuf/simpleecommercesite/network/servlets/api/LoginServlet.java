package com.yusuf.simpleecommercesite.network.servlets.api;


import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectReader;
import com.yusuf.simpleecommercesite.entities.Customer;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static com.yusuf.simpleecommercesite.network.servlets.api.ApiServlet.apiPath;

@WebServlet( urlPatterns = {apiPath+ "/login"}, name = "Login",asyncSupported = true)
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
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
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
        Customer customer= (Customer) req.getSession().getAttribute(Customer.class.getSimpleName());
        if (customer!=null && customer.getEmail()!=null) {
//            resp.sendRedirect("/");
            return;
        }
        List<Customer> results=dbContext.search(Customer.class, Map.of("email",email,"password",password));
        if(results.isEmpty()){
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        customer=results.get(0);
//        Map<String, String> cookieVal= getCookieValues(customer);
//        cookieVal.remove(Customer.class.getSimpleName());
//        if (req.getCookies()!=null)
//            Arrays.stream(req.getCookies()).filter(cookie -> cookieVal.containsKey(cookie.getName()))
//                    .peek(cookie -> cookie.setValue(cookieVal.get(cookie.getName())))
//                    .forEach(resp::addCookie);
//        else cookieVal.entrySet().forEach(entry->resp.addCookie(createGlobalCookie(entry.getKey(), entry.getValue())));
        req.getSession().setAttribute(Customer.class.getSimpleName(),customer);
        req.getSession().setAttribute("login", true);
//        resp.sendRedirect("/");
    }
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.getSession().removeAttribute(Customer.class.getSimpleName());
        req.getSession().setAttribute("login", false);
//        resp.sendRedirect("/login");
    }
}
