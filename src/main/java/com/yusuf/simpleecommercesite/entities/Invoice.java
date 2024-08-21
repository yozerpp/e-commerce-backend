package com.yusuf.simpleecommercesite.entities;


import com.fasterxml.jackson.annotation.JsonFilter;
import com.yusuf.simpleecommercesite.entities.annotations.AggregateMember;
import com.yusuf.simpleecommercesite.entities.annotations.Unique;
import com.yusuf.simpleecommercesite.entities.embedded.Address;
import javax.persistence.*;
import javax.validation.Constraint;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Objects;
import com.yusuf.simpleecommercesite.entities.annotations.Entity;
@Entity
@AggregateMember(in = Product.class)
public class Invoice implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
            @Column(name = "invoiceId")
     int id;
    @JsonFilter("depth_3")
    @OneToOne(optional = false)
            @NotNull
            @Column(name = "cartId")
            @Unique
     Cart cart;
     Date creationDate;
     Status status;
     PaymentMethod paymentMethod;
     Address deliveryAddress;
     BigDecimal paid;
     @Size(max = 30)
     String email;
     @JsonFilter("depth_1")
    @ManyToOne
    @Column(name = "customerId")
      @NotNull
     Customer customer;
    public Invoice(){}
    public Invoice(int id){
        this.id = id;
    }
    public Cart getCart() {
        return (Cart) cart;
    }
    public void setCart(Cart cart) {
        this.cart = cart;
    }
    public Date getCreationDate() {
        return creationDate;
    }
    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }
    public Status getStatus() {
        return status;
    }
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public void setStatus(Status status) {
        this.status = status;
    }
    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }
    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
    public Address getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(Address deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    
    public BigDecimal getPaid() {
        return paid;
    }

    public void setPaid( BigDecimal paid) {
        this.paid = paid;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    
    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }
    // Equals and hashCode methods

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !getClass().isAssignableFrom(o.getClass())) return false;

        Invoice invoice = (Invoice) o;

        return Objects.equals(hashCode(), invoice.hashCode());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    public enum PaymentMethod{
        OnlineCard,
        AtDoorCard,
        AtDoorCash
    }
    public enum Status{
        InProgress,
        Completed,
        Canceled
    }
}
