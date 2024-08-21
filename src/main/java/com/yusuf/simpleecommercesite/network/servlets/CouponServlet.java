package com.yusuf.simpleecommercesite.network.servlets;

import com.yusuf.simpleecommercesite.entities.Coupon;
import com.yusuf.simpleecommercesite.entities.Seller;
import com.yusuf.simpleecommercesite.helpers.ErrandBoy;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

@WebServlet(urlPatterns = {"/coupon/*", "/coupon"})
public class CouponServlet extends ApiServlet{
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String id= req.getPathInfo();
        Coupon coupon= dbContext.findById(Coupon.class, id);
        if (coupon==null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }else {
            jsonMapper.writeValue(resp.getWriter(), coupon);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Seller seller;
        if ((seller= (Seller) req.getSession().getAttribute(Seller.class.getSimpleName()))==null){
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        Coupon coupon = jsonMapper.readValue(req.getReader(), Coupon.class);
        coupon.setSeller(seller);
        try {
            dbContext.save(coupon);
        } catch (SQLException e) {
            if (e.getSQLState().equals(SqlErrors.DUPLICATE_KEY.toString())){
                resp.sendError(HttpServletResponse.SC_CONFLICT);
                return;
            }
        }
        resp.setHeader("Location", getServletContext().getContextPath() + "/" + ErrandBoy.toRestLink(coupon));
    }
}
