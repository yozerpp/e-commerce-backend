package com.yusuf.simpleecommercesite.entities;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.yusuf.simpleecommercesite.entities.annotations.AggregateMember;
import com.yusuf.simpleecommercesite.entities.annotations.Entity;
import javax.persistence.*;

import java.io.Serializable;
import java.util.Objects;
@Entity
@AggregateMember(in=Rating.class)
public class RatingVote implements Serializable {
    @Id
    @OneToOne(optional=false)
            @Column(name = "ratingId")
     Rating rating;
     VoteType vote;
     @JsonIgnore
     @Id
     @OneToOne(optional=false)
     @Column(name = "customerId")
     Customer customer;
    public enum VoteType {
        UP, DOWN
    }
    public RatingVote(Rating rating, Customer customerId){
        this(rating);
        this.customer = customerId;
    }
    public RatingVote(Rating rating){
        this.rating = rating;
    }
    // Empty constructor
    public RatingVote() {
    }

    // Getters and setters (including dirty flags)

    public Rating getRating() {
        return rating;
    }

    public void setRating(Rating rating) {
        this.rating = rating;
    }

    
    public VoteType getVote() {
        return vote;
    }

    public void setVote( VoteType vote) {
        this.vote = vote;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customerId) {
        this.customer = customerId;
    }

    // Equals and hashCode methods

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !getClass().isAssignableFrom(o.getClass())) return false;
        RatingVote that = (RatingVote) o;
        return Objects.equals(getCustomer(),that.getCustomer()) && Objects.equals(getRating(), that.getRating());
    }

    @Override
    public int hashCode() {
       return Objects.hash(rating, customer);
    }
}
