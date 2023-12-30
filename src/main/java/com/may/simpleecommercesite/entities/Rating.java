package com.may.simpleecommercesite.entities;
import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;


public class Rating extends Entity implements Serializable {
     int ratingId;
    Product productId;
     String firstName;

    
     String lastName;

    
     Timestamp dateRated;

    
     BigDecimal rate;

     String comment;

     int authenticated;

     String answer;

     int upCount;

     int downCount;

     int cookieId;

     boolean ratingIdDirty;
     boolean firstNameDirty;
     boolean lastNameDirty;
     boolean dateRatedDirty;
     boolean rateDirty;
     boolean commentDirty;
     boolean authenticatedDirty;
     boolean answerDirty;
     boolean upCountDirty;
     boolean downCountDirty;
     boolean productIdDirty;
     boolean cookieIdDirty;

    // Empty constructor
    public Rating() {
    }

    // Constructor for required fields
    public Rating(int ratingId, String firstName, String lastName, Timestamp dateRated, BigDecimal rate, Product productId, int cookieId) {
        this.ratingId = ratingId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateRated = dateRated;
        this.rate = rate;
        this.productId = productId;
        this.cookieId = cookieId;
    }

    // Constructors for combinations of fields that don't have  annotation
    public Rating(int ratingId, String firstName, String lastName, Timestamp dateRated, BigDecimal rate, Product productId, int cookieId, String comment) {
        this(ratingId, firstName, lastName, dateRated, rate, productId, cookieId);
        this.comment = comment;
    }

    public Rating(int ratingId, String firstName, String lastName, Timestamp dateRated, BigDecimal rate, Product productId, int cookieId, String comment, int authenticated, String answer) {
        this(ratingId, firstName, lastName, dateRated, rate, productId, cookieId, comment);
        this.authenticated = authenticated;
        this.answer = answer;
    }
    public Rating(int ratingId){
        this.ratingId=ratingId;
    }
    public Rating(int ratingId, Product productId){
        this(ratingId);
        this.productId=productId;
    }
    // Getters and setters (including dirty flags)

    public int getRatingId() {
        return ratingId;
    }

    public void setRatingId(int ratingId) {
        this.ratingId = ratingId;
        this.ratingIdDirty = true;
    }

    public Product getProductId() {
        return productId;
    }

    public void setProductId(Product productId) {
        this.productId = productId;
        this.productIdDirty = true;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
        this.firstNameDirty = true;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
        this.lastNameDirty = true;
    }

    public Timestamp getDateRated() {
        return dateRated;
    }

    public void setDateRated(Timestamp dateRated) {
        this.dateRated = dateRated;
        this.dateRatedDirty = true;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public void setRate(BigDecimal rate) {
        this.rate = rate;
        this.rateDirty = true;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
        this.commentDirty = true;
    }

    public int getAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(int authenticated) {
        this.authenticated = authenticated;
        this.authenticatedDirty = true;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
        this.answerDirty = true;
    }

    public int getUpCount() {
        return upCount;
    }

    public void setUpCount(int upCount) {
        this.upCount = upCount;
        this.upCountDirty = true;
    }

    public int getDownCount() {
        return downCount;
    }

    public void setDownCount(int downCount) {
        this.downCount = downCount;
        this.downCountDirty = true;
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

        Rating rating = (Rating) o;

        return ratingId == rating.ratingId;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(ratingId);
    }
}
