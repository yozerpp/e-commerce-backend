package com.may.simpleecommercesite.entities;


import com.may.simpleecommercesite.annotations.Id;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Objects;

public class Invoice extends Entity implements Serializable {
    @Id
     int invoiceId;
     Cart cartId;
     Timestamp dateOrdered;
     Status invoiceStatus;
     PaymentMethod paymentMethod;
     String deliveryAddress;
     java.math.BigDecimal paid;
     RegisteredCustomer email;
     String unregEmail;

     boolean cartIdDirty;
     boolean dateOrderedDirty;
     boolean invoiceStatusDirty;
     boolean paymentMethodDirty;
     boolean deliveryAddressDirty;
     boolean paidDirty;
     boolean invoiceIdDirty;
     boolean emailDirty;
     boolean unregEmailDirty;
    public Invoice(){}
    // Empty constructor
    public Invoice(String unregEmail) {
        this.unregEmail=unregEmail;
    }
    public Invoice (RegisteredCustomer email){
        this.email=email;
    }
    public Invoice(int invoiceId){
        this.invoiceId=invoiceId;
    }
    public Invoice(int invoiceId, RegisteredCustomer email){
        this(invoiceId);
        this.email=email;
    }
    // Constructor for required fields
    public Invoice(Cart cartId, String deliveryAddress, BigDecimal paid) {
        this.cartId = cartId;
        this.deliveryAddress = deliveryAddress;
        this.paid = paid;
    }
    public Invoice(Cart cartId, String deliveryAddress, RegisteredCustomer email, PaymentMethod paymentMethod){
        this.cartId=cartId;
        this.deliveryAddress=deliveryAddress;
        this.email=email;
        this.paymentMethod=paymentMethod;
    }
    public Invoice(Cart cartId, String deliveryAddress, PaymentMethod paymentMethod, String unregEmail){
        this.cartId=cartId;
        this.deliveryAddress=deliveryAddress;
        this.paymentMethod=paymentMethod;
        this.unregEmail=unregEmail;
    }
    // Constructors for combinations of fields that don't have  annotation
    public Invoice(Cart cartId, Timestamp dateOrdered, Status invoiceStatus, String deliveryAddress, BigDecimal paid) {
        this.cartId = cartId;
        this.dateOrdered = dateOrdered;
        this.invoiceStatus = invoiceStatus;
        this.deliveryAddress = deliveryAddress;
        this.paid = paid;
    }

    public Invoice(Cart cartId, Timestamp dateOrdered, Status invoiceStatus, String deliveryAddress, BigDecimal paid, RegisteredCustomer email, String unregEmail) {
        this.cartId = cartId;
        this.dateOrdered = dateOrdered;
        this.invoiceStatus = invoiceStatus;
        this.deliveryAddress = deliveryAddress;
        this.paid = paid;
        this.email = email;
        this.unregEmail = unregEmail;
    }

    // Getters and setters (including dirty flags)

    public Cart getCartId() {
        return (Cart) cartId.fetch();
    }
    public void setCartId(Cart cartId) {
        this.cartId = cartId;
        this.cartIdDirty = true;
    }
    public Timestamp getDateOrdered() {
        return dateOrdered;
    }
    public void setDateOrdered(Timestamp dateOrdered) {
        this.dateOrdered = dateOrdered;
        this.dateOrderedDirty = true;
    }
    public Status getInvoiceStatus() {
        return invoiceStatus;
    }

    public void setInvoiceStatus( Status invoiceStatus) {
        this.invoiceStatus = invoiceStatus;
        this.invoiceStatusDirty = true;
    }
    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }
    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
        this.paymentMethodDirty=true;
    }
    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
        this.deliveryAddressDirty = true;
    }

    
    public java.math.BigDecimal getPaid() {
        return paid;
    }

    public void setPaid( java.math.BigDecimal paid) {
        this.paid = paid;
        this.paidDirty = true;
    }

    public int getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(int invoiceId) {
        this.invoiceId = invoiceId;
        this.invoiceIdDirty = true;
    }

    
    public RegisteredCustomer getEmail() {
        return (RegisteredCustomer) email.fetch();
    }

    public void setEmail( RegisteredCustomer email) {
        this.email = email;
        this.emailDirty = true;
    }

    
    public String getUnregEmail() {
        return unregEmail;
    }

    public void setUnregEmail( String unregEmail) {
        this.unregEmail = unregEmail;
        this.unregEmailDirty = true;
    }

    // Equals and hashCode methods

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Invoice invoice = (Invoice) o;

        return cartId == invoice.cartId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(cartId, invoiceId);
    }
    public enum PaymentMethod{
        onlineCard,
        atDoorCard,
        atDoorCash
    }
    public enum Status{
        InProgress,
        Complete,
        Canceled
    }
}
