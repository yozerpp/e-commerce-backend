package com.may.simpleecommercesite.apiServlets;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.may.simpleecommercesite.beans.DBService;
import com.may.simpleecommercesite.entities.Product;
import com.may.simpleecommercesite.entities.Rating;
import com.may.simpleecommercesite.helpers.Json;
import org.apache.commons.lang.NullArgumentException;
import org.owasp.esapi.codecs.Base64;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@WebServlet(urlPatterns = {"/api/product", "/api/product/*"}, asyncSupported = true)
public class ProductApiServlet extends ApiServlet{
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        String pathInfo=req.getPathInfo();
        if (pathInfo.isEmpty()) {
            resp.addHeader("Cache-Control", "public, max-age=7200");
            Map<String, Object> params = new HashMap<>();
            for (Map.Entry<String, Class<?>> entry : paramNames.entrySet()) {
                Class<?> type = entry.getValue();
                String paramName = entry.getKey();
                params.put(paramName, type.cast(assignNumOrNull(type, req.getParameter(paramName))));
            }
            AsyncContext asyncContext = req.startAsync();
            asyncContext.start(new Runnable() {
                @Override
                public void run() {
                    DBService service = new DBService(dataSource);
                    List<Product> products = (List<Product>) service.byFields(Product.class, params, false);
                    try {
                        if (products != null) Json.serializeObject(products, asyncContext.getResponse().getWriter());
                        else ((HttpServletResponse) asyncContext.getResponse()).setStatus(HttpServletResponse.SC_NOT_FOUND);
                    } catch (IOException e){
                        throw new RuntimeException(e);
                    } finally {
                        service.destroy();
                        asyncContext.complete();
                    }
                }
            });
        }
        else{
            Matcher productIdFinder=Pattern.compile("/(\\d+).*").matcher(pathInfo);
            productIdFinder.find();
            int productId= Integer.parseInt(productIdFinder.group(1));
            if(pathInfo.matches("^/\\d+$")){
                Map<String, Object> params=Map.of("productId", productId);
                AsyncContext asyncContext =req.startAsync();
                asyncContext.start(new Runnable() {
                    @Override
                    public void run() {
                        DBService service=new DBService(dataSource);
                        Product product= (Product) service.byFields(Product.class, params, false).get(0);
                        try {
                            Json.serializeObject(product,asyncContext.getResponse().getWriter());
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        finally {
                            service.destroy();
                            asyncContext.complete();
                        }
                    }
                });
            } else if (pathInfo.matches(".*/rating")){
                Map<String, Object> params=Map.of("productId", productId);
                AsyncContext asyncContext= req.startAsync();
                asyncContext.start(new Runnable() {
                    @Override
                    public void run() {
                        DBService service=new DBService(dataSource);
                        List<Rating> ratings= (List<Rating>) service.byFields(Rating.class, params, false);
                        try {
                            Json.serializeObject(ratings, asyncContext.getResponse().getWriter());
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        } finally {
                            service.destroy();
                            asyncContext.complete();
                        }
                    }
                });
            } else if (pathInfo.matches(".*/image")){
                resp.setContentType("image/jpeg");
                AsyncContext asyncContext=req.startAsync();
                asyncContext.start(new Runnable() {
                    @Override
                    public void run() {
                        DBService service=new DBService(dataSource);
                        byte[] image= ((Product)service.byFields(Product.class, Map.of("productId", productId), true).get(0)).getImage();
                        try {
                            resp.getWriter().print(Base64.encodeBytes( image));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        } finally {
                            service.destroy();
                            asyncContext.complete();
                        }
                    }
                });
            }
        }
    }

//    @Override
//    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
//        String servletPath=req.getServletPath();
//        if(servletPath.equals("/api/product/")) {
//            Pattern productIdPattern=Pattern.compile("^(\\d+)");
//            String pathInfo=req.getPathInfo();
//            int productId= Integer.parseInt(productIdPattern.matcher(pathInfo).group(1));
//            //{firstName, lastName, rate, comment}
//            if (pathInfo.matches("\\d+/rating")){
//
//            }
//        } else {
//            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
//        }
//    }

    private static Object assignNumOrNull(Class<?> clazz, String value){
        Object val=null;
        try {
            val =clazz.getConstructor(String.class).newInstance(value);
        } catch (NumberFormatException | NullArgumentException | NoSuchMethodException | InstantiationException |
                 IllegalAccessException | InvocationTargetException ignored){}
        return val;
    }
    private static final Map<String, Class<?>> paramNames=Map.of("size",Integer.class,"page",Integer.class,  "title",String.class, "boughtHigh", BigDecimal.class,"boughtLow", BigDecimal.class,"discountedPriceHigh", BigDecimal.class,"discountedPriceLow", BigDecimal.class,"avgRatingHigh", BigDecimal.class,"avgRatingLow" , BigDecimal.class);
}
