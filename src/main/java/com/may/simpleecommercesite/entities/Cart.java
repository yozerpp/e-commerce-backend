package com.may.simpleecommercesite.entities;

import com.may.simpleecommercesite.annotations.Cookie;
import com.may.simpleecommercesite.annotations.Id;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

public class Cart extends Entity implements Serializable {
     @Id
     @Cookie
     public int cartId;
     public java.math.BigDecimal total= BigDecimal.valueOf(0);
       public int ordered;
     boolean cartIdDirty;
     boolean totalDirty;
     boolean orderedDirty;

    // Empty constructor
    public Cart() {
    }
    // Constructor for required fields
    public Cart(int cartId,  java.math.BigDecimal total, int ordered) {
        this.cartId = cartId;
        this.total = total;
        this.ordered = ordered;
    }
    public Cart(int cartId,  java.math.BigDecimal total) {
        this.cartId = cartId;
        this.total = total;
    }
    // Constructors for combinations of fields that don't have  annotation
    public Cart(int cartId) {
        this.cartId = cartId;
    }

    // Getters and setters (including dirty flags)
    public int getCartId() {
        return cartId;
    }
    public void setCartId(int cartId) {
        this.cartId = cartId;
        this.cartIdDirty = true;
    }
    public java.math.BigDecimal getTotal() {
        return total;
    }
    public void setTotal( java.math.BigDecimal total) {
        this.total = total;
        this.totalDirty = true;
    }
    public int getOrdered() {
        return ordered;
    }
    public void setOrdered(int ordered) {
        this.ordered = ordered;
        this.orderedDirty = true;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Cart cart = (Cart) o;

        return cartId == cart.cartId;
    }
    @Override
    public int hashCode() {
        return Objects.hash(cartId);
    }
}
