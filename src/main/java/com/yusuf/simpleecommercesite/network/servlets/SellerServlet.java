package com.yusuf.simpleecommercesite.network.servlets;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.yusuf.simpleecommercesite.entities.Coupon;
import com.yusuf.simpleecommercesite.entities.Product;
import com.yusuf.simpleecommercesite.entities.Rating;
import com.yusuf.simpleecommercesite.entities.Seller;
import com.yusuf.simpleecommercesite.helpers.ErrandBoy;
import com.yusuf.simpleecommercesite.network.dtos.SearchResult;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.yusuf.simpleecommercesite.network.servlets.ApiServlet.apiRoot;

@WebServlet(urlPatterns = {apiRoot +  "/seller", apiRoot + "/seller/*"},name = "Seller", asyncSupported = true)
public class SellerServlet extends ApiServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (req.getPathInfo()==null){
            Seller seller;
            if ((seller = (Seller) req.getSession().getAttribute(Seller.class.getSimpleName()))==null){
                resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
            jsonMapper.writeValue(resp.getWriter(), seller);
        }
        else if(Objects.equals(req.getPathInfo(), "all") || Objects.equals(req.getPathInfo(), "")){
            SearchResult<Seller> ret=dbContext.search(Seller.class, null);
            jsonMapper.writeValue(resp.getWriter(),ret);
        }else if(req.getPathInfo().matches("/\\d+")){
            Seller seller=dbContext.findById(Seller.class, extractResourceId(req.getPathInfo(),1));
            jsonMapper.writeValue(resp.getWriter(),seller);
        } else if(req.getPathInfo().matches("/\\d+/coupons")){
            int sellerId=extractResourceId(req.getPathInfo(),1);
            Seller seller= dbContext.findById(Seller.class, sellerId);
            List<Coupon> coupons=seller.getCoupons();
            if(coupons==null ||coupons.isEmpty()){
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            }else {
                jsonMapper.writeValue(resp.getWriter(),coupons);
            }
        } else if(req.getPathInfo().matches("/\\d+/products")){
            int sellerId = extractResourceId(req.getPathInfo(), 1);
            Map<String, Object> params = getParams(req);
            params.put("seller",new Seller(sellerId));
            SearchResult<Product> result= dbContext.search(Product.class, params);
            if (result.getCount()==0) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            jsonMapper.writeValue(resp.getWriter(),result);
        } else if (req.getPathInfo().matches("/\\d+/ratings")){
            int sellerId = extractResourceId(req.getPathInfo(), 1);
            Map<String, Object> params = getParams(req);
            params.put("seller",new Seller(sellerId));
            SearchResult<Rating> result= dbContext.search(Rating.class, params);
            if (result.getCount()==0) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            jsonMapper.writeValue(resp.getWriter(),result);
        }
    }
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Seller seller;
        try {
            seller = jsonMapper.readValue(req.getReader(), Seller.class);
            if(seller==null || seller.getEmail()==null || seller.getPassword()==null){
                throw new JsonProcessingException("") {
                };
            }
        } catch (JsonProcessingException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        try {
        dbContext.save(seller);
        } catch (SQLException e){
            if (e.getSQLState().equals(SqlErrors.DUPLICATE_KEY.toString())){
                resp.sendError(HttpServletResponse.SC_CONFLICT);
                return;
            }
        }
        resp.setStatus(HttpServletResponse.SC_CREATED);
        resp.setHeader("Location", ErrandBoy.toRestLink(seller));
    }
}
