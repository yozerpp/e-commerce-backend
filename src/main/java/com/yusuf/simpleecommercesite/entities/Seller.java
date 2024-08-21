package com.yusuf.simpleecommercesite.entities;

import javax.persistence.*;
import javax.validation.constraints.Email;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.yusuf.simpleecommercesite.entities.annotations.Entity;
import com.yusuf.simpleecommercesite.entities.embedded.Address;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Seller implements Serializable, IUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
            @Column(name = "sellerId")
     int id;
    @Size(max = 25)
     String name;
     Address address;
    @JsonFilter("depth_1")
    public List<Product> getProducts() {
        return products;
    }

    public void setProducts(List<Product> products) {
        this.products = products;
    }
    @OneToMany(mappedBy = "sellerId")
    List<Product> products;
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
    @Email
    String email;
     String password;

    public BigDecimal getAvgRating() {
        return avgRating;
    }

    public void setAvgRating(BigDecimal avgRating) {
        this.avgRating = avgRating;
    }

    public BigDecimal getRatingCount() {
        return ratingCount;
    }

    public void setRatingCount(BigDecimal ratingCount) {
        this.ratingCount = ratingCount;
    }
    @GeneratedValue
    private BigDecimal avgRating;
    @GeneratedValue
     private BigDecimal ratingCount;
    public List<Coupon> getCoupons() {
        return coupons;
    }

    public void setCoupons(List<Coupon> coupons) {
        this.coupons = coupons;
    }

    @OneToMany(mappedBy = "sellerId")
     private List<Coupon> coupons;
    // Constructor for required fields
    public Seller(int id) {
        this.id = id;
    }
    public Seller() {
    }
    // Constructors for combinations of fields that don't have  annotation


    // Getters and setters (including dirty flags)

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    // Equals and hashCode methods

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !getClass().isAssignableFrom(o.getClass())) return false;

        Seller seller = (Seller) o;

        return getId()==seller.getId();
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}
