package com.yusuf.simpleecommercesite.network.servlets;

import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.yusuf.simpleecommercesite.dbContext.DbContext;
import com.yusuf.simpleecommercesite.entities.Customer;
import com.yusuf.simpleecommercesite.entities.Product;
import com.yusuf.simpleecommercesite.entities.Rating;
import com.yusuf.simpleecommercesite.entities.RatingVote;
import com.yusuf.simpleecommercesite.helpers.ErrandBoy;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;

@WebServlet(urlPatterns = {"/rating", "/rating/*"}, name = "Rating")
public class RatingServlet extends ApiServlet{
    private ObjectReader ratingReader;
    private ObjectWriter ratingWriter;

    @Override
    public void init() throws ServletException {
        super.init();
        this.ratingReader= this.jsonMapper.readerFor(Rating.class);
        this.ratingWriter=this.jsonMapper.writerFor(Rating.class);
    }
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        int ratingId= extractResourceId(pathInfo,1);
        resp.setContentType("application/json");
        Rating rating= dbContext.findById(Rating.class, ratingId);
        if (rating==null) resp.sendError(404);
        else {
            ratingWriter.writeValue(resp.getWriter(), rating);
        }
    }
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        Customer customer = getCustomer(req);
        if (pathInfo==null) {
        Rating rating= ratingReader.readValue(req.getReader());
        int productId;
        try {
            productId = Integer.parseInt(req.getParameter("productId"));
        } catch (NullPointerException | NumberFormatException e) {resp.sendError(HttpServletResponse.SC_BAD_REQUEST); return;}
        rating.setCustomer(customer);
        rating.setProduct(new Product(productId));
        try {
            if(!checkFirstNameConstraint(rating)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            dbContext.insert(rating);
            resp.setStatus(HttpServletResponse.SC_CREATED);
            resp.setHeader("Location", getServletContext().getContextPath()+'/' + ErrandBoy.toRestLink(rating));
         } catch (SQLException e) {
            if (e.getSQLState().equals(SqlErrors.DUPLICATE_KEY.toString())) resp.setStatus(HttpServletResponse.SC_CONFLICT);
            else throw new RuntimeException(e);
        }
    } else if (pathInfo.matches(".*/\\d+/vote")) {
            int ratingId= extractResourceId(pathInfo, 1);
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
        } else resp.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo=req.getPathInfo();
        int ratingId=extractResourceId(pathInfo, 1);
        Customer customer=(Customer) req.getSession().getAttribute(Customer.class.getSimpleName());
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
            dbContext.save(oldRating, oldRating.getRating().compareTo(BigDecimal.ZERO)==0);
        }catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo=req.getPathInfo();
        int ratingId=extractResourceId(pathInfo, 1);
        Customer customer=getCustomer(req);
        if(pathInfo.matches(".*/\\d+$")) {
            Rating old= dbContext.findById(Rating.class,ratingId);
            if(old.getCustomer().getId()!=customer.getId()){
                resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
            dbContext.remove(old);
            resp.setStatus(HttpServletResponse.SC_ACCEPTED);
        } else if(pathInfo.matches(".*/\\d+/vote")){
            RatingVote old= dbContext.findById(RatingVote.class,new Rating(ratingId), customer);
            if(old==null){
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            }
            else
                dbContext.remove(old);
        }
    }

    protected static boolean checkFirstNameConstraint(Rating rating) {
        if (rating.getFirstName() == null | rating.getLastName() == null) {
            if (rating.getCustomer().getEmail()==null) {
                return false;
            } else {
                rating.setFirstName(rating.getCustomer().getFirstName());
                rating.setLastName(rating.getCustomer().getLastName());
            }
        }
       return true;
    }
}
