package com.yusuf.simpleecommercesite.entities;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.yusuf.simpleecommercesite.entities.annotations.AggregateMember;

import javax.persistence.*;
import javax.validation.constraints.Max;
import javax.validation.constraints.Size;

import com.yusuf.simpleecommercesite.entities.annotations.Entity;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Objects;


@Entity
@AggregateMember(in = Product.class)
public class Rating implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
            @Column(name = "ratingId")
     int id;
     @JsonIgnore
     @ManyToOne(optional = false)
             @Column(name = "productId")
    Product product;
     @JsonIgnore
     @ManyToOne(optional = false)
             @Column(name = "customerId")
    Customer customer;
     @Size(max = 25)
     String firstName;
    @Size(max = 25)
     String lastName;
     Date dateRated;
     @Max(5)
     BigDecimal rating;
     String comment;
     boolean confirmed;
     int upVotes;
     int downVotes;

    // Empty constructor
    public Rating() {}
    public Rating(int id){
        this.id = id;
    }
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Date getDateRated() {
        return dateRated;
    }

    public void setDateRated(Date dateRated) {
        this.dateRated = dateRated;
    }

    public BigDecimal getRating() {
        return rating;
    }

    public void setRating(BigDecimal rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public void setConfirmed(boolean confirmed) {
        this.confirmed = confirmed;
    }

    public int getUpVotes() {
        return upVotes;
    }

    public void setUpVotes(int upVotes) {
        this.upVotes = upVotes;
    }

    public int getDownVotes() {
        return downVotes;
    }

    public void setDownVotes(int downVotes) {
        this.downVotes = downVotes;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !getClass().isAssignableFrom(o.getClass())) return false;

        Rating rating = (Rating) o;

        return Objects.equals(getCustomer(), rating.getCustomer()) && Objects.equals(getProduct(), rating.getProduct());
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}
