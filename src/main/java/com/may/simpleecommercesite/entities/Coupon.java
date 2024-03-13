package com.may.simpleecommercesite.entities;

import com.may.simpleecommercesite.annotations.Entity;
import com.may.simpleecommercesite.annotations.Id;
import com.may.simpleecommercesite.annotations.OneToOne;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Objects;
@Entity
public class Coupon implements Serializable {

    @Id
     String couponCode;

    
     int discount;
     Timestamp validUntil;
     @OneToOne(joinColumn = "sellerId")
     Seller seller;
    // Constructor for required fields
    public Coupon(String couponCode, int discountAmount, Timestamp validUntil) {
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
        this.seller =seller;
    }

    // Getters and setters (including dirty flags)
    public String getCouponCode() {
        return couponCode;
    }
    public void setCouponCode( String code) {
        this.couponCode = code;
    }
    public int getDiscount() {
        return discount;
    }
    public void setDiscount( int discountAmount) {
        this.discount = discountAmount;
    }

    public Timestamp getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(Timestamp validUntil) {
        this.validUntil = validUntil;
    }
    public Seller getSeller(){
        return seller;
    }
    public void setSeller(Seller seller){
        this.seller =seller;
    }

    // Equals and hashCode methods

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !getClass().isAssignableFrom(o.getClass())) return false;
        Coupon coupon = (Coupon) o;
        return Objects.equals(getCouponCode(), coupon.getCouponCode());
    }

    @Override
    public int hashCode() {
        return couponCode.hashCode();
    }
}
