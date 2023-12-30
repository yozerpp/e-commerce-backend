package com.may.simpleecommercesite.entities;

import com.may.simpleecommercesite.annotations.Id;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Objects;

public class Coupon extends Entity implements Serializable {

    @Id
     String couponCode;

    
     java.math.BigDecimal discount;

     Timestamp validUntil;
     Seller sellerId;
     boolean couponCodeDirty;
     boolean discountDirty;
     boolean validUntilDirty;
     boolean sellerIdDirty;
    // Constructor for required fields
    public Coupon(String couponCode, BigDecimal discountAmount, Timestamp validUntil) {
        this.couponCode = couponCode;
        this.validUntil = validUntil;
        this.discount = discountAmount;
    }
    public Coupon(String couponCode) {
        this.couponCode=couponCode;
    }
    public Coupon() {
    }

    public Coupon(String couponCode, Seller seller) {
        this(couponCode);
        this.sellerId=seller;
    }

    // Getters and setters (including dirty flags)
    public String getCouponCode() {
        return couponCode;
    }
    public void setCouponCode( String code) {
        this.couponCode = code;
        this.couponCodeDirty = true;
    }
    public java.math.BigDecimal getDiscount() {
        return discount;
    }
    public void setDiscount( java.math.BigDecimal discountAmount) {
        this.discount = discountAmount;
        this.discountDirty = true;
    }

    public Timestamp getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(Timestamp validUntil) {
        this.validUntil = validUntil;
        this.validUntilDirty = true;
    }
    public Seller getSellerId(){

        return (Seller) this.sellerId.fetch();
    }
    public void setSellerId(Seller seller){
        this.sellerId=seller;
        this.sellerIdDirty=true;
    }

    // Equals and hashCode methods

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Coupon coupon = (Coupon) o;

        return Objects.equals(couponCode, coupon.couponCode);
    }

    @Override
    public int hashCode() {
        return couponCode.hashCode();
    }
}
