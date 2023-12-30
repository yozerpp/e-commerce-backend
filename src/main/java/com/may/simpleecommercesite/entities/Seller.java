package com.may.simpleecommercesite.entities;

import javax.validation.constraints.NotNull;
import java.util.Objects;
import java.io.Serializable;

public class Seller extends Entity implements Serializable {
    
     int sellerId;

    
     String shopName;

     String address;

     boolean sellerIdDirty;
     boolean shopNameDirty;
     boolean addressDirty;

    // Constructor for required fields
    public Seller( int sellerId,  String shopName, String address) {
        this(sellerId, shopName);
        this.address = address;
    }
    public Seller( int sellerId,  String shopName) {
        this(sellerId);
        this.shopName = shopName;
    }

    public Seller(int sellerId) {
        this.sellerId=sellerId;
    }
    public Seller() {
    }
    // Constructors for combinations of fields that don't have  annotation


    // Getters and setters (including dirty flags)

    public int getSellerId() {
        return sellerId;
    }

    public void setSellerId( int sellerId) {
        this.sellerId = sellerId;
        this.sellerIdDirty = true;
    }

    
    public String getShopName() {
        return shopName;
    }

    public void setShopName( String shopName) {
        this.shopName = shopName;
        this.shopNameDirty = true;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
        this.addressDirty = true;
    }

    // Equals and hashCode methods

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Seller seller = (Seller) o;

        return sellerId == seller.sellerId;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(sellerId);
    }
}
