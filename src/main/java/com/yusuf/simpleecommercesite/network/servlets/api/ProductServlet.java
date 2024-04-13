package com.yusuf.simpleecommercesite.network.servlets.api;

import com.fasterxml.jackson.databind.ObjectReader;
import com.yusuf.simpleecommercesite.entities.*;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.yusuf.simpleecommercesite.network.servlets.api.ApiServlet.apiPath;

@WebServlet(urlPatterns = {apiPath +"/product",apiPath + "/product/*"}, name = "Product", asyncSupported = true)
public class ProductServlet extends ApiServlet {
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
        if (pathInfo==null) { // search
            resp.addHeader("Cache-Control", "public, max-age=7200");
            Map<String, Object> params = new HashMap<>();
            for (Map.Entry<String, String[]> param: req.getParameterMap().entrySet())
                if(param.getValue().length==1)
                    params.put(param.getKey(), param.getValue()[0]);
                else throw new RuntimeException("array params aren't supported");
            List<Product> products = dbContext.search(Product.class, params);
            if (products != null) jsonMapper.writeValue(resp.getWriter(),products);
            else resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
        else{ // single product
            Matcher productIdFinder=Pattern.compile( "/(\\d+).*").matcher(pathInfo);
            productIdFinder.find();
            int productId= Integer.parseInt(productIdFinder.group(1));
            if(pathInfo.matches( "^/\\d+$")){
                Product product= dbContext.findById(Product.class, productId);
                jsonMapper.writeValue(resp.getWriter(), product);
            } else if (pathInfo.matches(".*\\d+/rating")){ // all ratings of a product
                Map<String, Object> params=Map.of("productId", productId);
                List<Rating> ratings= dbContext.findById(Product.class, productId).getRatings();
                jsonMapper.writeValue(resp.getWriter(), ratings);
            } else if(pathInfo.matches(".*\\d+/rating/\\d+")){ // a rating of a product
                int ratingId=extractResourceId(pathInfo, 2);
                Rating rating= dbContext.findById(Rating.class,ratingId);
                if(rating==null) resp.setStatus(404);
                else jsonMapper.writeValue(resp.getWriter(), rating);
            }else if (pathInfo.matches(".*\\d+/image")){ // images
                resp.addHeader("Cache-Control", "public, max-age=7200");
                List<Image> images= dbContext.findById(Product.class, productId).getImages();
                if (images==null || images.isEmpty()) {
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                AsyncContext asyncContext=req.startAsync();
                Object param= req.getParameter("all");
                asyncContext.start(()-> {
                    if (param == null || !((boolean) param)) {
                        Image i = images.stream().filter(Image::isMain).findFirst().get();
                        resp.setContentType("image/jpeg");
                        asyncContext.start(() -> {
                            try {
                                byte[]data=i.getData();
                                resp.setContentLength(data.length);
                                resp.getOutputStream().write(data);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
                    } else {
                        resp.setContentType("application/zip");
                        try (ZipOutputStream zip = new ZipOutputStream(
                                resp.getOutputStream())) {
                            int len=0;
                            int idx = 0;
                            for (Image img : images) {
                                ZipEntry entry = new ZipEntry(img.isMain() ? "main" : String.valueOf(idx++));
                                zip.putNextEntry(entry);
                                byte[] data=img.getData();
                                len+=data.length;
                                zip.write(data);
                            }
                            zip.finish();
                            resp.setContentLength(len);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo=req.getPathInfo();
        int productId=extractResourceId(pathInfo, 1);
        Customer customer = (Customer) req.getSession().getAttribute(Customer.class.getSimpleName());
        if (pathInfo.matches(".*/rating$")){
            try {
                Rating rating = ratingReader.readValue(req.getReader());
                if (rating.getFirstName() == null | rating.getLastName() == null) {
                    if (customer.getEmail()==null) {
                        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        return;
                    } else {
                        rating.setFirstName(customer.getFirstName());
                        rating.setLastName(customer.getLastName());
                    }
                }
                rating.setProduct(new Product(productId));
                rating.setCustomer(customer);
                rating= dbContext.save(rating);
                resp.setStatus(HttpServletResponse.SC_CREATED);
                resp.setHeader("Location", req.getContextPath() + req.getServletPath() +"product/" +productId + "/rating/" + rating.getId());
            } catch (SQLException e) {
                if (e.getErrorCode()==1062) resp.setStatus(HttpServletResponse.SC_CONFLICT);
                else throw new RuntimeException(e);
            }
        } else if (pathInfo.matches(".*/rating/\\d+/vote")){
            int ratingId= extractResourceId(pathInfo, 2);
            try {
                boolean vote_= req.getParameter("up")!=null;
                RatingVote vote=new RatingVote();
                vote.setVote(vote_?RatingVote.VoteType.UP:RatingVote.VoteType.DOWN);
                vote.setRating(new Rating(ratingId));
                vote.setCustomer(customer);
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
        Customer customer=(Customer) req.getSession().getAttribute(Customer.class.getSimpleName());
        if (pathInfo.matches(".*/rating/\\d+")){
            try {
                Rating newRating= ratingReader.readValue(req.getReader());
                Rating oldRating=dbContext.findById(Rating.class,ratingId);
                if(oldRating==null) {
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                if(oldRating.getCustomer().getId()!=customer.getId()) {
                    resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }
                    oldRating.setComment(newRating.getComment());
                    oldRating.setRating(newRating.getRating());
                    dbContext.save(oldRating);
                }catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo=req.getPathInfo();
        int ratingId=extractResourceId(pathInfo, 2);
        Customer customer=getCustomer(req);
        if(pathInfo.matches(".*/rating/\\d+$")){
            Rating old= dbContext.findById(Rating.class,ratingId);
            if(old.getCustomer().getId()!=customer.getId()){
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
            dbContext.remove(new Rating(ratingId));
            resp.setStatus(HttpServletResponse.SC_ACCEPTED);
        } else if(pathInfo.matches(".*/rating/\\d+/vote")){
            RatingVote old= dbContext.findById(RatingVote.class,new Rating(ratingId), customer);
            if(old==null){
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            }
            else
                dbContext.remove(old);
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
