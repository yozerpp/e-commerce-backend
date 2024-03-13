package com.may.simpleecommercesite.entities;
import com.may.simpleecommercesite.annotations.Cookie;
import com.may.simpleecommercesite.annotations.Entity;
import com.may.simpleecommercesite.annotations.Id;
import com.may.simpleecommercesite.annotations.OneToOne;

import java.io.Serializable;
import java.util.Objects;
@Entity
public class RatingVote implements Serializable {
    @Id
    @OneToOne(joinColumn = "ratingId")
     Rating rating;
     VoteType vote;
     @Cookie
     int cookieId;
    public enum VoteType {
        UP, DOWN
    }
    public RatingVote(Rating rating, int cookieId){
        this(rating);
        this.cookieId=cookieId;
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

    public int getCookieId() {
        return cookieId;
    }

    public void setCookieId(int cookieId) {
        this.cookieId = cookieId;
    }

    // Equals and hashCode methods

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !getClass().isAssignableFrom(o.getClass())) return false;
        RatingVote that = (RatingVote) o;
        return getCookieId()==that.getCookieId() && Objects.equals(getRating(), that.getRating());
    }

    @Override
    public int hashCode() {
       return Objects.hash(rating, cookieId);
    }
}
