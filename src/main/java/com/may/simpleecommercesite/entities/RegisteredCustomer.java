package com.may.simpleecommercesite.entities;

import com.may.simpleecommercesite.annotations.Cookie;
import com.may.simpleecommercesite.annotations.Id;

import java.io.Serializable;
import java.sql.Timestamp;

public class RegisteredCustomer extends Entity implements Serializable {
     @Cookie
     int cookieId;

    @Id
     String email;

    
     String credential;

    
     String firstName;
     Timestamp dateOfBirth;
     String lastName;
     @Cookie
     Cart cartId;

     boolean cookieIdDirty;
     boolean emailDirty;
     boolean credentialDirty;
     boolean firstNameDirty;
     boolean lastNameDirty;
     boolean addressDirty;
     boolean cartIdDirty;

    // Empty constructor
    public RegisteredCustomer() {
    }

    // Constructor for required fields
    public RegisteredCustomer(int cookieId,  String email,  String credential,  String firstName,  String lastName) {
        this.cookieId = cookieId;
        this.email = email;
        this.credential = credential;
        this.firstName = firstName;
        this.lastName = lastName;
    }
    public RegisteredCustomer(String credential, Timestamp dateOfBirth ,String email, String firstName, String lastName){
        this(email, credential);
        this.dateOfBirth=dateOfBirth;
        this.firstName=firstName;
        this.lastName=lastName;
    }
    RegisteredCustomer(String email, String credential){
        this.email=email;
        this.credential=credential;
    }
    public RegisteredCustomer(String email, String credential,int cookieId){
        this(email, cookieId);
        this.credential=credential;
    }
    public RegisteredCustomer(String email, int cookieId){
        this(email);
        this.cookieId=cookieId;
    }
    public RegisteredCustomer(String email) {
        this.email=email;
    }
    // Getters and setters (including dirty flags)

    public int getCookieId() {
        return cookieId;
    }

    public void setCookieId(int cookieId) {
        this.cookieId = cookieId;
        this.cookieIdDirty = true;
    }

    
    public String getEmail() {
        return email;
    }

    public void setEmail( String email) {
        this.email = email;
        this.emailDirty = true;
    }

    public Timestamp getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(Timestamp dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getCredential() {
        return credential;
    }

    public void setCredential( String credential) {
        this.credential = credential;
        this.credentialDirty = true;
    }

    
    public String getFirstName() {
        return firstName;
    }

    public void setFirstName( String firstName) {
        this.firstName = firstName;
        this.firstNameDirty = true;
    }

    
    public String getLastName() {
        return lastName;
    }

    public void setLastName( String lastName) {
        this.lastName = lastName;
        this.lastNameDirty = true;
    }

    public Cart getCartId() {
        return cartId;
    }
    public void setCartId(Cart cartId) {
        this.cartId = cartId;
        this.cartIdDirty = true;
    }

    // Equals and hashCode methods

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RegisteredCustomer customer = (RegisteredCustomer) o;

        return cookieId == customer.cookieId && email.equals(customer.email);
    }

    @Override
    public int hashCode() {
        int result = Integer.hashCode(cookieId);
        result = 31 * result + email.hashCode();
        return result;
    }
}

