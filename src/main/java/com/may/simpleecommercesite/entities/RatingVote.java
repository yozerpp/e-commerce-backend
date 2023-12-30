package com.may.simpleecommercesite.entities;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Objects;

public class RatingVote extends Entity implements Serializable {
     Rating ratingId;
     VoteType vote;
     int cookieId;
     boolean ratingIdDirty;
     boolean voteDirty;
     boolean cookieIdDirty;

    public enum VoteType {
        UP, DOWN
    }
    // Constructor for required fields
    public RatingVote(Rating ratingId,  VoteType vote, int cookieId) {
        this(ratingId, cookieId);
        this.vote = vote;
    }
    public RatingVote(Rating ratingId, int cookieId){
        this(ratingId);
        this.cookieId=cookieId;
    }
    public RatingVote(Rating ratingId){
        this.ratingId=ratingId;
    }
    // Empty constructor
    public RatingVote() {
    }

    // Getters and setters (including dirty flags)

    public Rating getRatingId() {
        return ratingId;
    }

    public void setRatingId(Rating ratingId) {
        this.ratingId = ratingId;
        this.ratingIdDirty = true;
    }

    
    public VoteType getVote() {
        return vote;
    }

    public void setVote( VoteType vote) {
        this.vote = vote;
        this.voteDirty = true;
    }

    public int getCookieId() {
        return cookieId;
    }

    public void setCookieId(int cookieId) {
        this.cookieId = cookieId;
        this.cookieIdDirty = true;
    }

    // Equals and hashCode methods

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RatingVote that = (RatingVote) o;
        return cookieId == that.cookieId && ratingId.equals(that.ratingId);
    }

    @Override
    public int hashCode() {
       return Objects.hash(ratingId, cookieId);
    }
}
