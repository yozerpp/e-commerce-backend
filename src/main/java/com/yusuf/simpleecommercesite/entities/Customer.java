package com.yusuf.simpleecommercesite.entities;

import javax.persistence.*;

import com.yusuf.simpleecommercesite.entities.annotations.Cookie;
import com.yusuf.simpleecommercesite.entities.embedded.Address;
import com.yusuf.simpleecommercesite.entities.annotations.Entity;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Date;
@Entity
public class Customer implements Serializable,IUser {
     @Cookie
     @Id
     @Column(name = "customerId")
     @GeneratedValue(strategy = GenerationType.IDENTITY)
     int id;
     @Email
     String email;
     @Size(min = 12, max = 50)
     String password;
     @Size(min = 4, max = 15)
     String firstName;
     Date birthDate;
    @Size(min = 4, max = 15)
     String lastName;
     Address address;
     @NotNull
     @OneToOne
             @Column(name = "cartId")
     Cart cart;
    public Customer() {}
    public Customer(String firstName, String lastName, String email, String password, Timestamp birthDate){
    this.firstName=firstName;
    this.lastName=lastName;
    this.email=email;
    this.password = password;
    this.birthDate = birthDate;
    }
    public Customer(String email, String password){
        this.email=email;
        this.password = password;
    }
    public Customer(String email) {
        this.email=email;
    }
    public Customer(int id){this.id = id;}
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public String getEmail() {
        return email;
    }
    public void setEmail( String email) {
        this.email = email;
    }

    public Date getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(Date birthDate) {
        this.birthDate = birthDate;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    
    public String getFirstName() {
        return firstName;
    }

    public void setFirstName( String firstName) {
        this.firstName = firstName;
    }

    
    public String getLastName() {
        return lastName;
    }

    public void setLastName( String lastName) {
        this.lastName = lastName;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public Address getAddress() {
        return address;
    }

    public Cart getCart() {
        return cart;
    }
    public void setCart(Cart cart) {
        this.cart = cart;
    }

    // Equals and hashCode methods

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !getClass().isAssignableFrom(o.getClass())) return false;

        Customer customer = (Customer) o;

        return getId()==customer.getId();
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}

