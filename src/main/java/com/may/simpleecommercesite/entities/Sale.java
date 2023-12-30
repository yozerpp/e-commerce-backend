package com.may.simpleecommercesite.entities;

import com.may.simpleecommercesite.annotations.Cookie;
import com.may.simpleecommercesite.annotations.Id;
import com.may.simpleecommercesite.annotations.SecondId;

import javax.validation.constraints.Null;
import java.io.Serializable;
import java.util.Objects;

public class Sale extends Entity implements Serializable {
     @Id
     @Cookie
     public Cart cartId;
     @SecondId
     public Product productId;

    
     public int quantity;

    
     public java.math.BigDecimal totalPrice;

     public Coupon couponCode;

     boolean cartIdDirty;
     boolean productIdDirty;
     boolean quantityDirty;
     boolean totalPriceDirty;
     boolean couponCodeDirty;
    // Constructor for required fields
    public Sale(Cart cartId,Coupon couponCode, Product productId,  int quantity) {
        this(productId, quantity);
        this.cartId = cartId;
        this.couponCode = couponCode;
    }
    public Sale(Cart cartId, Coupon couponCode, Product productId) {
        this(productId);
        this.cartId = cartId;
        this.couponCode = couponCode;
    }
    public Sale (Product productId, int quantity){
        this(productId);
        this.quantity=quantity;
    }
    public Sale(Product productId){
        this.productId=productId;
    }
    public Sale(Cart cartId){
        this.cartId=cartId;
    }
    public Sale() {
    }
    // Constructors for combinations of fields that don't have  annotation


    // Getters and setters (including dirty flags)

    public Cart getCartId() {
        return (Cart) cartId.fetch();
    }
    public void setCartId(Cart cartId) {
        this.cartId = cartId;
        this.cartIdDirty = true;
    }

    public Product getProductId() {
        if (productId==null) return productId;
        else return (Product) productId.fetch();
    }
    public void setProductId(Product productId) {
        this.productId = productId;
        this.productIdDirty = true;
    }

    
    public int getQuantity() {
        return quantity;
    }

    public void setQuantity( int quantity) {
        this.quantity = quantity;
        this.quantityDirty = true;
    }

    
    public java.math.BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice( java.math.BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
        this.totalPriceDirty = true;
    }
    public Coupon getCouponCode() {
        if (couponCode== null) return null;
        else return (Coupon) couponCode.fetch();
    }
    public void setCouponCode(Coupon couponCode) {
        this.couponCode = couponCode;
        this.couponCodeDirty = true;
    }

    // Equals and hashCode methods

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Sale sale = (Sale) o;

        return cartId.equals(sale.cartId) && productId.equals( sale.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cartId.hashCode(), productId.hashCode());
    }
}
