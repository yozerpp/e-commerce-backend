package com.may.simpleecommercesite.entities;

import com.may.simpleecommercesite.annotations.Id;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Blob;

public class Product extends Entity implements Serializable {
    @Id
     public int productId;

     public byte[] image;

    
    public String title;

     public String description;

    
        public java.math.BigDecimal basePrice;

     public int baseDiscount;

    
     public java.math.BigDecimal avgRating;

     public int sellerId;

    
    public   java.math.BigDecimal discountedPrice;

     public int bought;

     boolean productIdDirty;
     boolean imageDirty;
     boolean titleDirty;
     boolean descriptionDirty;
     boolean basePriceDirty;
     boolean baseDiscountDirty;
     boolean avgRatingDirty;
     boolean sellerIdDirty;
     boolean discountedPriceDirty;
     boolean boughtDirty;

    // Empty constructor
    public Product() {
    }
    public Product(int productId){
        this.productId=productId;
    }
    // Constructor for required fields
    public Product(int productId, String title, String description, BigDecimal basePrice, int baseDiscount, BigDecimal avgRating, int sellerId, BigDecimal discountedPrice, int bought) {
        this.productId = productId;
        this.title = title;
        this.description = description;
        this.basePrice = basePrice;
        this.baseDiscount = baseDiscount;
        this.avgRating = avgRating;
        this.sellerId = sellerId;
        this.discountedPrice = discountedPrice;
        this.bought = bought;
    }
    public Product( int productId,  String title,  java.math.BigDecimal basePrice,  java.math.BigDecimal avgRating,  java.math.BigDecimal discountedPrice) {
        this.productId = productId;
        this.title = title;
        this.basePrice = basePrice;
        this.avgRating = avgRating;
        this.discountedPrice = discountedPrice;
    }

    // Constructors for combinations of fields that don't have  annotation
    public Product( int productId,  String title,  java.math.BigDecimal basePrice,  java.math.BigDecimal avgRating,  java.math.BigDecimal discountedPrice, String description) {
        this(productId, title, basePrice, avgRating, discountedPrice);
        this.description = description;
    }

    public Product( int productId,  String title,  java.math.BigDecimal basePrice,  java.math.BigDecimal avgRating,  java.math.BigDecimal discountedPrice, int baseDiscount) {
        this(productId, title, basePrice, avgRating, discountedPrice);
        this.baseDiscount = baseDiscount;
    }

    public Product( int productId,  String title,  java.math.BigDecimal basePrice,  java.math.BigDecimal avgRating,  java.math.BigDecimal discountedPrice, int sellerId, int bought) {
        this(productId, title, basePrice, avgRating, discountedPrice);
        this.sellerId = sellerId;
        this.bought = bought;
    }



    // Getters and setters (including dirty flags)

    public int getProductId() {
        return productId;
    }

    public void setProductId ( int productId) {
        this.productId = productId;
        this.productIdDirty = true;
    }

    public byte[] getImage() {
        return image;
    }

    public void setImage(byte[] image) {
        this.image = image;
        this.imageDirty = true;
    }

    
    public String getTitle() {
        return title;
    }

    public void setTitle( String title) {
        this.title = title;
        this.titleDirty = true;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        this.descriptionDirty = true;
    }

    
    public java.math.BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice( java.math.BigDecimal basePrice) {
        this.basePrice = basePrice;
        this.basePriceDirty = true;
    }

    public int getBaseDiscount() {
        return baseDiscount;
    }

    public void setBaseDiscount(int baseDiscount) {
        this.baseDiscount = baseDiscount;
        this.baseDiscountDirty = true;
    }

    
    public java.math.BigDecimal getAvgRating() {
        return avgRating;
    }

    public void setAvgRating( java.math.BigDecimal avgRating) {
        this.avgRating = avgRating;
        this.avgRatingDirty = true;
    }

    public int getSellerId() {
        return sellerId;
    }

    public void setSellerId(int sellerId) {
        this.sellerId = sellerId;
        this.sellerIdDirty = true;
    }

    
    public java.math.BigDecimal getDiscountedPrice() {
        return discountedPrice;
    }

    public void setDiscountedPrice( java.math.BigDecimal discountedPrice) {
        this.discountedPrice = discountedPrice;
        this.discountedPriceDirty = true;
    }

    public int getBought() {
        return bought;
    }

    public void setBought(int bought) {
        this.bought = bought;
        this.boughtDirty = true;
    }

    // Equals and hashCode methods

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Product product = (Product) o;

        return productId == product.productId;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(productId);
    }
}
