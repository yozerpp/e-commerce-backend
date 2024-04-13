package com.yusuf.simpleecommercesite.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.yusuf.simpleecommercesite.entities.annotations.Column;
import com.yusuf.simpleecommercesite.entities.annotations.Entity;
import com.yusuf.simpleecommercesite.entities.annotations.Id;
import com.yusuf.simpleecommercesite.entities.annotations.ManyToOne;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Objects;
@Entity
public class Coupon implements Serializable {
    @Column(name = "couponId")
    @Id
            @NotNull
     String id;
     int discount;
     Date expiryDate;
     @JsonIgnore
     @ManyToOne(joinColumn = "sellerId")
    Seller seller;
    public Coupon(String id, int discountAmount, Timestamp expiryDate) {
        this.id = id;
        this.expiryDate = expiryDate;
        this.discount = discountAmount;
    }
    public Coupon(String id) {
        this.id = id;
    }
    public Coupon() {
    }
    public Seller getSeller() {
        return seller;
    }
    public void setSeller(Seller seller) {
        this.seller = seller;
    }
    // Getters and setters (including dirty flags)
    public String getId() {
        return id;
    }
    public void setId(String code) {
        this.id = code;
    }
    public int getDiscount() {
        return discount;
    }
    public void setDiscount( int discountAmount) {
        this.discount = discountAmount;
    }

    public Date getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(Date expiryDate) {
        this.expiryDate = expiryDate;
    }
    // Equals and hashCode methods

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !getClass().isAssignableFrom(o.getClass())) return false;
        Coupon coupon = (Coupon) o;
        return Objects.equals(getId(), coupon.getId());
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
