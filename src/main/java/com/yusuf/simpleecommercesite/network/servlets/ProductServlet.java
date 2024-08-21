package com.yusuf.simpleecommercesite.network.servlets;

import com.fasterxml.jackson.databind.ObjectReader;
import com.yusuf.simpleecommercesite.entities.*;
import com.yusuf.simpleecommercesite.helpers.ErrandBoy;
import com.yusuf.simpleecommercesite.network.dtos.SearchResult;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.security.URIParameter;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.yusuf.simpleecommercesite.network.servlets.ApiServlet.apiRoot;

@WebServlet(urlPatterns = {apiRoot +"/product", apiRoot + "/product/*"}, name = "Product", asyncSupported = true)
public class    ProductServlet extends ApiServlet {
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
            resp.addHeader("Cache-Control", "private, must-revalidate, max-age=30");
            Map<String, Object> params = new HashMap<>();
            for (Map.Entry<String, String[]> param: req.getParameterMap().entrySet())
                if(param.getValue().length==1)
                    params.put(param.getKey(), param.getValue()[0]);
                else throw new RuntimeException("array params aren't supported");
            SearchResult<Product> products = dbContext.search(Product.class, params);
            if (products != null) jsonMapper.writeValue(resp.getWriter(),products);
            else resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
        else{ // single product
            resp.addHeader("Cache-Control", "no-cache");
            Matcher productIdFinder=Pattern.compile( "/(\\d+).*").matcher(pathInfo);
            productIdFinder.find();
            int productId= Integer.parseInt(productIdFinder.group(1));
            if(pathInfo.matches( "^/\\d+$")){
                Product product= dbContext.findById(Product.class, productId);
                jsonMapper.writeValue(resp.getWriter(), product);
            } else if (pathInfo.matches(".*\\d+/rating")){ // all ratings of a product
                Map<String,Object> params = getParams(req);
                params.put("productId", productId);
                SearchResult<Rating> ratings= dbContext.search(Rating.class,params );
                jsonMapper.writeValue(resp.getWriter(), ratings);
            } else if(pathInfo.matches(".*\\d+/rating/\\d+")){ // a rating of a product
                int ratingId=extractResourceId(pathInfo, 2);
                Rating rating= dbContext.findById(Rating.class,ratingId);
                if(rating==null) resp.setStatus(404);
                else jsonMapper.writeValue(resp.getWriter(), rating);
            }else if (pathInfo.matches(".*\\d+/image")){ // images
                resp.addHeader("Cache-Control", "public, max-age=" + 3600*24);
                List<Image> images= dbContext.findById(Product.class, productId).getImages();
                if (images==null || images.isEmpty()) {
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                AsyncContext asyncContext=req.startAsync();
                asyncContext.start(()-> {
                    HttpServletResponse response = (HttpServletResponse) asyncContext.getResponse();
                    Object param = asyncContext.getRequest().getParameter("all");
                    try {
                        if (param == null) {
                            Image i = images.stream().min((i1, i2) -> (i1.isMain() ? 1 : (-1))).get();
                            response.setContentType("image/jpeg");
                            try {
                                byte[]data=i.getData();
                                response.setContentLength(data.length);
                                response.getOutputStream().write(data);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        } else {
                            response.setContentType("application/octet-stream");
                            ByteArrayOutputStream tmp = new ByteArrayOutputStream();
                            ZipOutputStream zip = new ZipOutputStream(
                                    tmp);
                                int len=0;
                                int idx = 0;
                                for (Image img : images) {
                                    String entryName ="image"  +( img.isMain() ? "Main" : String.valueOf(idx++)) + ".jpg";
                                    zip.putNextEntry(new ZipEntry(entryName));
                                    byte[] data=img.getData();
                                    zip.write(data);
                                    zip.closeEntry();
                                }
                                zip.finish();
                                zip.close();
                                response.setContentLength(tmp.size());
                                response.getOutputStream().write(tmp.toByteArray());
                                response.getOutputStream().close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace(System.err);
                        throw new RuntimeException(e);
                    } finally {
                        asyncContext.complete();
                    }
                });
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (req.getSession().getAttribute("Seller") == null) {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        Product product= jsonMapper.readValue(req.getReader(), Product.class);
        if (product.getOriginalPrice()==null || product.getTitle()==null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        product.setSeller((Seller) req.getSession().getAttribute(Seller.class.getSimpleName()));
        product.setId(0);
        product.setDateAdded(null);
        try {
            dbContext.save(product);
        } catch (SQLException e) {
            e.printStackTrace(System.err);
            throw new RuntimeException(e);
        }
        resp.setHeader("Location",getServletContext().getContextPath()+ "/"+ ErrandBoy.toRestLink(product));
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Seller seller;
        int productId= Integer.parseInt(req.getParameter("productId"));
        if ((seller= (Seller) req.getSession().getAttribute(Seller.class.getSimpleName()))== null) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        Product product = jsonMapper.readValue(req.getReader(),Product.class);
        Product oldProduct=dbContext.findById(Product.class, productId);
        if (oldProduct==null || !oldProduct.getSeller().equals(seller)) {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        product.setSeller(seller);
        product.setId(productId);
        try {
            dbContext.save(product);
        }catch (SQLException e){
            if (e.getSQLState().equals(SqlErrors.DUPLICATE_KEY.toString())){
                resp.setStatus(HttpServletResponse.SC_CONFLICT);
                return;
            }
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
    private static final Map<String, Class<?>> paramNames=new HashMap<>();
    static {
        paramNames.put(  "size",Integer.class);paramNames.put("page",Integer.class);paramNames.put( "title",String.class);paramNames.put("boughtHigh", BigDecimal.class);
        paramNames.put("discountedPriceHigh", BigDecimal.class);paramNames.put("discountedPriceLow", BigDecimal.class);paramNames.put("avgRatingHigh", BigDecimal.class);paramNames.put("avgRatingLow" , BigDecimal.class);
    }
}
