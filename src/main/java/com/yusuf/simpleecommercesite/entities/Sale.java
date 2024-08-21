package com.yusuf.simpleecommercesite.entities;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.yusuf.simpleecommercesite.entities.annotations.Entity;

import javax.annotation.Generated;
import javax.persistence.*;
import javax.validation.constraints.Max;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;
@Entity
public class Sale implements Serializable {
     @Id
     @ManyToOne
     @JsonIgnore
     @JsonFilter("depth_4")
     @Column(name = "cartId")
     private Cart cart;
    @Id
     @OneToOne
             (optional = false)
    @Column(name = "productId")
     @JsonFilter("depth_4")
     private Product product;
    @ManyToOne
    @Column(name = "couponId")
     @JsonFilter("depth_4")
    @GeneratedValue
     private Coupon coupon;
    private @Max(100) BigInteger quantity;
     @GeneratedValue
     private java.math.BigDecimal price;
     @GeneratedValue
     private BigDecimal finalTotal;
    public Sale() {
    }
    public Sale (Product product, Cart cart){
        this(product);
        this.cart=cart;
        this.quantity=BigInteger.ONE;
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

    
    public @Max(100) BigInteger getQuantity() {
        return quantity;
    }

    public void setQuantity(@Max(100) BigInteger quantity) {
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
