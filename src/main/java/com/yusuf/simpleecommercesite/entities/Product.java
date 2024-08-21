package com.yusuf.simpleecommercesite.entities;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;
import javax.validation.constraints.Max;
import javax.validation.constraints.Size;

import com.yusuf.simpleecommercesite.entities.annotations.Entity;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;

@Entity
public class Product implements Serializable {
    @Id
    @Column(name = "productId")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
     private int id;
    @JsonIgnore
    @OneToMany(mappedBy = "productId")
     private List<Image> images;
    private Date dateAdded;
    @Size(min = 6, max = 25)
    private String title;
    @Lob
     private String description;
     private BigDecimal originalPrice;
     private @Max(100) BigInteger discount;
     @GeneratedValue
     private BigDecimal avgRating;
     @GeneratedValue
     private int ratingCount;
     @ManyToOne
     @Column(name = "sellerId")
     @JsonFilter("depth_1")
    private Seller seller;
    @ManyToOne
     @Column(name= "brandId")
     private Brand brand;
     @JsonFilter("depth_1")
    @ManyToOne
     @Column(name = "categoryId")
     private Category category;
     @JsonIgnore
     @JsonFilter("depth_1")
     @OneToMany(mappedBy = "productId")
     private List<Rating> ratings;
     @GeneratedValue
    private    BigDecimal taxedPrice;
     @GeneratedValue
     private int saleCount;

    public BigDecimal getUsd() {
        return usd;
    }

    public void setUsd(BigDecimal usd) {
        this.usd = usd;
    }

    public BigDecimal getEur() {
        return eur;
    }

    public void setEur(BigDecimal eur) {
        this.eur = eur;
    }

    public BigDecimal getPound() {
        return pound;
    }

    public void setPound(BigDecimal pound) {
        this.pound = pound;
    }

    public BigDecimal getYen() {
        return yen;
    }

    public void setYen(BigDecimal yen) {
        this.yen = yen;
    }
    @GeneratedValue(strategy = GenerationType.AUTO)
     private BigDecimal usd;
    @GeneratedValue(strategy = GenerationType.AUTO)
     private BigDecimal eur;
    @GeneratedValue(strategy = GenerationType.AUTO)
     private BigDecimal pound;
    @GeneratedValue(strategy = GenerationType.AUTO)
     private BigDecimal yen;
    // Empty constructor
    public Product() {
    }

    public Product(int id){
        this.id = id;
    }
    // Constructor for required fields
    public Product(int id, String title, BigDecimal originalPrice, BigDecimal avgRating, BigDecimal taxedPrice) {
        this.id = id;
        this.title = title;
        this.originalPrice = originalPrice;
        this.avgRating = avgRating;
        this.taxedPrice = taxedPrice;
    }
    public Date getDateAdded() {
        return dateAdded;
    }
    public Brand getBrand() {
        return brand;
    }

    public void setBrand(Brand brand) {
        this.brand = brand;
    }

    public void setDateAdded(Date dateAdded) {
        this.dateAdded = dateAdded;
    }
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }

    public List<Image> getImages() {
        return this.images;
    }
    public int getRatingCount() {return ratingCount;}
    public void setRatingCount(int ratingCount) {this.ratingCount = ratingCount;}
    public void setImages(List<Image> image) {
        this.images = image;
    }
    public List<Rating> getRatings(){
        return this.ratings;
    }
    public void setRatings(List<Rating> ratings){
        this.ratings=ratings;
    }
    public String getTitle() {
        return title;
    }
    public void setTitle( String title) {
        this.title = title;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }
    public BigDecimal getOriginalPrice() {
        return originalPrice;
    }

    public void setOriginalPrice(BigDecimal originalPrice) {
        this.originalPrice = originalPrice;
    }

    public @Max(100) BigInteger getDiscount() {
        return discount;
    }

    public void setDiscount(@Max(100) BigInteger discount) {
        this.discount = discount;
    }

    
    public BigDecimal getAvgRating() {
        return avgRating;
    }

    public void setAvgRating( BigDecimal avgRating) {
        this.avgRating = avgRating;
    }

    public Seller getSeller() {
        return seller;
    }

    public void setSeller(Seller seller) {
        this.seller = seller;
    }

    
    public BigDecimal getTaxedPrice() {
        return taxedPrice;
    }

    public void setTaxedPrice(BigDecimal taxedPrice) {
        this.taxedPrice = taxedPrice;
    }

    public int getSaleCount() {
        return saleCount;
    }

    public void setSaleCount(int saleCount) {
        this.saleCount = saleCount;
    }

    // Equals and hashCode methods

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !getClass().isAssignableFrom(o.getClass())) return false;

        Product product = (Product) o;

        return getId() == product.getId();
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}
