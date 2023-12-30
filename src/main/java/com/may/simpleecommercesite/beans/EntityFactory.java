package com.may.simpleecommercesite.beans;

import com.may.simpleecommercesite.entities.*;

import javax.annotation.Resource;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.sql.DataSource;
import javax.xml.crypto.Data;
import java.math.BigDecimal;

public class EntityFactory {
    @Resource(name = "java:comp/env/jdbc/pool/test")
    private static DataSource dataSource;
    public static void setDataSource(DataSource dataSource) {
        EntityFactory.dataSource = dataSource;
    }
    public static RegisteredCustomer registeredcustomer(String email){
        RegisteredCustomer customer=new RegisteredCustomer(email);
        customer.setDataSource(dataSource);
        return customer;
    }
    public static RegisteredCustomer registeredcustomer (String email, int cookieId){
        RegisteredCustomer customer=new RegisteredCustomer(email, cookieId);
        customer.setDataSource(dataSource);
        return customer;
    }
    public static RegisteredCustomer registeredcustomer(String email, String credential ,int cookieId){
    RegisteredCustomer customer=new RegisteredCustomer(email, credential, cookieId);
    customer.setDataSource(dataSource);
    return customer;
    }
    public static RegisteredCustomer registeredcustomer(){
        RegisteredCustomer registeredCustomer=new RegisteredCustomer();
        registeredCustomer.setDataSource(dataSource);
        return registeredCustomer;
    }
    public static Sale sale(int cartId, int productId, int quantity, String couponCode) {
        Sale sale=new Sale(EntityFactory.cart(cartId), EntityFactory.coupon(couponCode), EntityFactory.product(productId), quantity);
        sale.setDataSource(dataSource);
        return sale;
    }
    public static Sale sale(int productId, int quantity){
        Sale sale=new Sale(product(productId), quantity);
        sale.setDataSource(dataSource);
        return sale;
    }
    public static Sale sale(int cartId) {
        Sale sale = new Sale(cart(cartId));
        sale.setDataSource(dataSource);
        return sale;
    }
    public static Sale sale(){
        Sale sale=new Sale();
        sale.setDataSource(dataSource);
        return sale;
    }
    public static Cart cart(int cartId){
        Cart cart=new Cart(cartId);
        cart.setDataSource(dataSource);
        return cart;
    }
    public static Cart cart(){
        Cart cart=new Cart();
        cart.setDataSource(dataSource);
        return cart;
    }
    public static Invoice invoice(String email, boolean type){
        Invoice invoice;
        if (type) {
            invoice = new Invoice(registeredcustomer(email));
        }
        else {
            invoice=new Invoice(email);
        }
        invoice.setDataSource(dataSource);
        return invoice;
    }
    public static Invoice invoice(){
        Invoice invoice=new Invoice();
        invoice.setDataSource(dataSource);
        return invoice;
    }
    public static Product product(int productId){
        Product product=new Product(productId);
        product.setDataSource(dataSource);
        return product;
    }
    public static Product product(){
        Product product=new Product();
        product.setDataSource(dataSource);
        return product;
    }
    public static Coupon coupon(String couponCode){
        Coupon coupon=new Coupon(couponCode);
        coupon.setDataSource(dataSource);
        return coupon;
    }
    public static Coupon coupon(String couponCode, int sellerId){
        Coupon coupon=new Coupon(couponCode, seller(sellerId));
        coupon.setDataSource(dataSource);
        return coupon;
    }
    public static Coupon coupon(){
        Coupon coupon=new Coupon();
        coupon.setDataSource(dataSource);
        return coupon;
    }
    public static Seller seller (int sellerId){
        Seller seller=new Seller (sellerId);
        seller.setDataSource(dataSource);
        return seller;
    }
    public static Seller seller(){
        Seller seller=new Seller();
        seller.setDataSource(dataSource);
        return seller;
    }
    public static Rating rating(int ratingId){
        Rating rating =new Rating(ratingId);
        rating.setDataSource(dataSource);
        return rating;
    }
    public static Rating rating(int ratingId, int productId){
        Rating rating=new Rating(ratingId, product(productId));
        rating.setDataSource(dataSource);
        return rating;
    }
    public static Rating rating(){
        Rating rating=new Rating();
        rating.setDataSource(dataSource);
        return rating;
    }
    public static RatingVote ratingvote(int ratingId){
        RatingVote ratingVote=new RatingVote(rating(ratingId));
        ratingVote.setDataSource( dataSource);
        return ratingVote;
    }
    public static RatingVote ratingvote(int ratingId, int cookieId){
        RatingVote ratingVote=new RatingVote(rating(ratingId), cookieId);
        ratingVote.setDataSource(dataSource);
        return ratingVote;
    }
    public static RatingVote ratingvote(){
        RatingVote ratingVote=new RatingVote();
        ratingVote.setDataSource(dataSource);
        return ratingVote;
    }

}
