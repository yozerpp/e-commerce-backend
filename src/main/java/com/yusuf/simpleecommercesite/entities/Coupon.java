package com.yusuf.simpleecommercesite.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import javax.persistence.*;
import com.yusuf.simpleecommercesite.entities.annotations.Entity;
import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Objects;
@Entity
public class Coupon implements Serializable {
    @Column(name = "couponId")
    @Id
            @NotNull
    @Size(min = 6, max = 32)
    private String id;
    private @Max(100) BigInteger discount;
    private Date expiryDate;
     @JsonIgnore
     @Column(name = "sellerId")
     @ManyToOne
    Seller seller;
    public Coupon(String id, @Max(100) BigInteger discountAmount, Timestamp expiryDate) {
        this.id = id;
        this.expiryDate = expiryDate;
        this.discount = discountAmount;
    }
    public Coupon(String id) {
        this.id = id;
    }
    public Coupon() {}
    public Seller getSeller() {
        return seller;
    }
    public void setSeller(Seller seller) {
        this.seller = seller;
    }
    public String getId() {
        return id;
    }
    public void setId(String code) {
        this.id = code;
    }
    public @Max(100) BigInteger getDiscount() {
        return discount;
    }
    public void setDiscount(@Max(100) BigInteger discountAmount) {
        this.discount = discountAmount;
    }
    public Date getExpiryDate() {
        return expiryDate;
    }
    public void setExpiryDate(Date expiryDate) {
        this.expiryDate = expiryDate;
    }
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
