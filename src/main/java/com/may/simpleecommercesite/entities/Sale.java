package com.may.simpleecommercesite.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.may.simpleecommercesite.annotations.*;

import java.io.Serializable;
import java.util.Objects;
@Entity
public class Sale implements Serializable {
     @Id
     @ManyToOne(joinColumn = "cartId")
     @JsonIgnore
     public Cart cart;
     @Id
     @OneToOne(joinColumn = "productId")
     public Product product;
     public int quantity;
     public java.math.BigDecimal totalPrice;
    @OneToOne(joinColumn = "couponCode")
     public Coupon coupon;
    // Constructor for required fields
    public Sale() {
    }
    public Sale (Product product, Cart cart){
        this(product);
        this.cart=cart;
    }
    public Sale(Product product){
        this.product = product;
    }
    public Sale(Cart cart){
        this.cart = cart;
    }

    // Constructors for combinations of fields that don't have  annotation


    // Getters and setters (including dirty flags)

    public Cart getCart() {
        return cart;
    }
    public void setCart(Cart cart) {
        this.cart = cart;
    }

    public Product getProduct() {
        return product;
    }
    public void setProduct(Product product) {
        this.product = product;
    }

    
    public int getQuantity() {
        return quantity;
    }

    public void setQuantity( int quantity) {
        this.quantity = quantity;
    }

    
    public java.math.BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice( java.math.BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }
    public Coupon getCoupon() {
        return coupon;
    }
    public void setCoupon(Coupon coupon) {
        this.coupon = coupon;
    }

    // Equals and hashCode methods

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !getClass().isAssignableFrom(o.getClass())) return false;
        Sale sale = (Sale) o;
        return Objects.equals(getCart(), sale.getCart()) && Objects.equals(getProduct(), sale.getProduct()) && Objects.equals(getCoupon(), sale.getCoupon());
    }

    @Override
    public int hashCode() {
        return Objects.hash(cart, product);
    }
}
