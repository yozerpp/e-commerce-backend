package com.yusuf.simpleecommercesite.network.servlets;

import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.yusuf.simpleecommercesite.entities.*;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

import static com.yusuf.simpleecommercesite.network.servlets.ApiServlet.apiRoot;

@WebServlet(urlPatterns = { apiRoot +"/cart"}, name = "Cart",asyncSupported = true)
public class CartServlet extends ApiServlet {
    ObjectReader saleReader;

    @Override
    public void init() throws ServletException {
        super.init();
        saleReader = this.jsonMapper.readerFor(Sale.class);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        Cart cart = getCart(req.getSession());
        StringWriter out = new StringWriter();
        jsonMapper.writeValue(out, cart);
        jsonMapper.writeValue(resp.getWriter(), cart);
    }

    /* {productId: 1, quantity: 2, couponCode: ACS23}*/
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Cart cart = getCart(req.getSession());
        try {
            try  {
                Sale newSale = saleReader.readValue(req.getReader());
                newSale.setCart(cart);
                newSale.setPrice(dbContext.findById(Product.class, newSale.getProduct().getId()).getFinalPrice());
                List<Sale> sales = cart.getSales();
                sales.remove(newSale);
                dbContext.save(newSale);
                sales.add(newSale);
                cart.setSales(sales);
                cart.setTotal(cart.getTotal().add(newSale.getPrice().multiply(BigDecimal.valueOf(newSale.getQuantity()))));
            } catch (MismatchedInputException e) {
                if (req.getParameter("increment") != null && req.getParameter(Product.class.getSimpleName().toLowerCase()) != null) {
                    boolean increment = Boolean.parseBoolean(req.getParameter("increment"));
                    Product product = new Product(Integer.parseInt(req.getParameter(Product.class.getSimpleName().toLowerCase())));
                    String couponCode=req.getParameter(Coupon.class.getSimpleName().toLowerCase());
                    Sale newSale = cart.getSales().stream().filter(sale -> sale.getProduct() == product).findFirst().orElseGet(() -> new Sale(product, cart));
                    newSale.setQuantity(newSale.getQuantity() + (increment ? 1 : -1));
                    if(couponCode!=null)
                        newSale.setCoupon(new Coupon(couponCode));
                    dbContext.save(newSale);
                } else {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                }
            }
        }catch (SQLException e){
            if(e.getSQLState().equals(SqlErrors.CART_ORDERED.toString())) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            }else if(e.getSQLState().equals(SqlErrors.COUPON_OWNER_MISMATCH.toString())){
                resp.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
            }else {
                throw new RuntimeException(e);
            }
        }
       }
//    private void addToCart(Sale newSale, Cart cart) {
//        newSale.setCart(cart);
//        newSale.setPrice(dbContext.findById(Product.class, newSale.getProduct().getProductId()).getFinalPrice());
//        List<Sale> sales = cart.getSales();
//        sales.remove(newSale);
//        try {
//            dbContext.save(newSale);
//        } catch (SQLException e) {
//            throw new RuntimeException(e);
//        }
//        sales.add(newSale);
//        cart.setSales(sales);
//        cart.setTotal(cart.getTotal().add(newSale.getPrice().multiply(BigDecimal.valueOf(newSale.getQuantity()))));
//    }
//    @Override
//    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
//        Product product= new Product(Integer.parseInt(req.getParameter(Product.class.getSimpleName())));
//        Cart cart = getCart(req.getSession());
//        boolean increment=req.getParameter("increment")!=null;
//        if(cart.getSales().stream().noneMatch(sale -> sale.getProduct()==product) && increment)
//            addToCart(new Sale(product, cart),cart);
//        else{
//            cart.getSales().stream().filter(sale -> sale.getProduct()==product).peek(sale ->sale.setQuantity(sale.getQuantity()+(increment?1:-1))).forEach(sale -> {
//                try {
//                    if(sale.getQuantity()>0)
//                        dbContext.save(sale);
//                    else {
//                        dbContext.remove(sale);
//                    }
//                } catch (SQLException e) {
//                    throw new RuntimeException(e);
//                }
//            });
//        }
//    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) {
        Cart cart = getCart(req.getSession());
        dbContext.remove(cart);
        try {
            setNewCart(req,resp);
        } catch (SQLException e) {
            if(Objects.equals(e.getSQLState(), SqlErrors.CART_ORDERED.toString())){
                resp.setStatus(406);
            }
        }
    }
    Cart getCart(HttpSession session){
        if(session.getAttribute("login")==null||!(boolean)session.getAttribute("login")) return (Cart) session.getAttribute(Cart.class.getSimpleName());
        else return ((Customer)session.getAttribute(Customer.class.getSimpleName())).getCart();
    }
}