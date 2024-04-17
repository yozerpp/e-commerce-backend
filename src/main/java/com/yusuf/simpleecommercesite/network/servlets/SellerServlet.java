package com.yusuf.simpleecommercesite.network.servlets;

import com.yusuf.simpleecommercesite.entities.Coupon;
import com.yusuf.simpleecommercesite.entities.Seller;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.yusuf.simpleecommercesite.network.servlets.ApiServlet.apiRoot;

@WebServlet(urlPatterns = {apiRoot +  "/seller", apiRoot + "/seller/*"},name = "Seller", asyncSupported = true)
public class SellerServlet extends ApiServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if(req.getPathInfo()==null || Objects.equals(req.getPathInfo(), "")){
            List<Seller> ret=dbContext.search(Seller.class, Map.of());
            jsonMapper.writeValue(resp.getWriter(),ret);
        }else if(req.getPathInfo().matches("/\\d+")){
            Seller seller=dbContext.findById(Seller.class, extractResourceId(req.getPathInfo(),1));
        } else if(req.getPathInfo().matches("/\\d+/coupons")){
            int sellerId=extractResourceId(req.getPathInfo(),1);
            Seller seller= dbContext.findById(Seller.class, sellerId);
            List<Coupon> coupons=seller.getCoupons();
            if(coupons==null ||coupons.isEmpty()){
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            }else {
                jsonMapper.writeValue(resp.getWriter(),coupons);
            }
        }
    }
}
