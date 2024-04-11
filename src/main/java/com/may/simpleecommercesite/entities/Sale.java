package com.may.simpleecommercesite.entities;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.may.simpleecommercesite.annotations.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;
@Entity
public class Sale implements Serializable {
     @Id
     @ManyToOne(joinColumn = "cartId")
     @JsonIgnore
     @JsonFilter("depth_4")
     private Cart cart;
    @Id
     @OneToOne(joinColumn = "productId")
     @JsonFilter("depth_4")
     private Product product;
    @ManyToOne(joinColumn = "couponId")
     @JsonFilter("depth_4")
     private Coupon coupon;
     private int quantity;
     private java.math.BigDecimal price;
     private BigDecimal finalTotal;
    public Sale() {
    }
    public Sale (Product product, Cart cart){
        this(product);
        this.cart=cart;
        this.quantity=1;
    }
    public Sale(Product product){
        this.product = product;
    }
    public Sale(Cart cart){
        this.cart = cart;
    }
    public BigDecimal getFinalTotal() {
        return finalTotal;
    }

    public void setFinalTotal(BigDecimal finalTotal) {
        this.finalTotal = finalTotal;
    }

    public Coupon getCoupon() {
        return coupon;
    }

    public void setCoupon(Coupon coupon) {
        this.coupon = coupon;
    }
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

    
    public java.math.BigDecimal getPrice() {
        return price;
    }

    public void setPrice(java.math.BigDecimal price) {
        this.price = price;
    }


    // Equals and hashCode methods

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !getClass().isAssignableFrom(o.getClass())) return false;
        Sale sale = (Sale) o;
        return Objects.equals(getCart(), sale.getCart()) && Objects.equals(getProduct(), sale.getProduct());
    }

    @Override
    public int hashCode() {
        return Objects.hash(cart, product);
    }
}
