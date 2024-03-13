package com.may.simpleecommercesite.apiServlets;

import com.fasterxml.jackson.databind.ObjectReader;
import com.may.simpleecommercesite.entities.*;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.nio.channels.AcceptPendingException;
import java.sql.SQLException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@WebServlet(urlPatterns = {"/api/product", "/api/product/*"})
public class ProductApiServlet extends ApiServlet{
    ObjectReader productReader;
    ObjectReader ratingReader;
    ObjectReader ratingVoteReader;

    @Override
    public void init() throws ServletException {
        super.init();
        this.productReader=this.jsonMapper.readerForArrayOf(Product.class);
        this.ratingReader=this.jsonMapper.readerFor(Rating.class);
        this.ratingVoteReader=this.jsonMapper.readerFor(RatingVote.class);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        String pathInfo=req.getPathInfo();
        if (pathInfo==null) {
            resp.addHeader("Cache-Control", "public, max-age=7200");
            Map<String, Object> params = new HashMap<>();
            for (Map.Entry<String, String[]> param: req.getParameterMap().entrySet())
                params.put(param.getKey(), param.getValue()[0]);
            List<Product> products = dbContext.search(Product.class, params);
            if (products != null) jsonMapper.writeValue(resp.getWriter(),products);
            else resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
        else{
            Matcher productIdFinder=Pattern.compile("/(\\d+).*").matcher(pathInfo);
            productIdFinder.find();
            int productId= Integer.parseInt(productIdFinder.group(1));
            if(pathInfo.matches("^/\\d+$")){
                Product product= dbContext.findById(Product.class, productId);
                jsonMapper.writeValue(resp.getWriter(), product);
            } else if (pathInfo.matches(".*/rating")){
                Map<String, Object> params=Map.of("productId", productId);
                List<Rating> ratings= dbContext.findById(Product.class, productId).getRatings();
                jsonMapper.writeValue(resp.getWriter(), ratings);
            } else if (pathInfo.matches(".*/image")){
                resp.setContentType("image/jpeg");
                byte[] image= dbContext.findById(Product.class, productId).getImage();
                if (image==null)
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                else
                    resp.getWriter().print(Base64.getMimeEncoder().encodeToString(image));
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo=req.getPathInfo();
        int cookieId= getCookieValue(req, Customer.class);
        int productId=extractResourceId(pathInfo, 1);
        if (pathInfo.matches(".*/rating$")){
            try {
                Rating rating = ratingReader.readValue(req.getReader());
                if (rating.getFirstName() == null | rating.getLastName() == null) {
                    if (req.getSession().getAttribute(RegisteredCustomer.class.getSimpleName()) == null) {
                        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        return;
                    } else {
                        RegisteredCustomer customer = (RegisteredCustomer) req.getSession().getAttribute(RegisteredCustomer.class.getSimpleName());
                        if (customer == null)
                            resp.sendRedirect("/login");
                        rating.setFirstName(customer.getFirstName());
                        rating.setLastName(customer.getLastName());
                    }
                }
                rating.setProduct(new Product(productId));
                rating.setCookieId(cookieId);
                dbContext.insert(rating);
                resp.setStatus(HttpServletResponse.SC_CREATED);
            } catch (SQLException e) {
                if (e.getErrorCode()==1062) resp.setStatus(HttpServletResponse.SC_CONFLICT);
                else throw new RuntimeException(e);
            }
        } else if (pathInfo.matches(".*/rating/\\d+/vote")){
            int ratingId= extractResourceId(pathInfo, 2);
            try {
                RatingVote vote=ratingVoteReader.readValue(req.getReader());
                vote.setRating(new Rating(ratingId));
                vote.setCookieId(cookieId);
                dbContext.save(vote);
                resp.setStatus(HttpServletResponse.SC_CREATED);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo=req.getPathInfo();
        int productId=extractResourceId(pathInfo, 1);
        int ratingId=extractResourceId(pathInfo, 2);
        if (pathInfo.matches(".*/rating/\\d+")){
            try {
                Rating rating= ratingReader.readValue(req.getReader());
                rating.setRatingId(ratingId);
                dbContext.save(rating);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo=req.getPathInfo();
        int ratingId=extractResourceId(pathInfo, 2);
        if(pathInfo.matches(".*/rating/\\d+$")){
            dbContext.remove(new Rating(ratingId));
            resp.setStatus(HttpServletResponse.SC_ACCEPTED);
        }
    }
    private static Object assignNumOrNull(Class<?> clazz, String value){
        Object val=null;
        try {
            val =clazz.getConstructor(String.class).newInstance(value);
        } catch (NumberFormatException  | NoSuchMethodException | InstantiationException |
                 IllegalAccessException | InvocationTargetException ignored){}
        return val;
    }
    private static final Map<String, Class<?>> paramNames=Map.of("size",Integer.class,"page",Integer.class,  "title",String.class, "boughtHigh", BigDecimal.class,"boughtLow", BigDecimal.class,"discountedPriceHigh", BigDecimal.class,"discountedPriceLow", BigDecimal.class,"avgRatingHigh", BigDecimal.class,"avgRatingLow" , BigDecimal.class);
}
