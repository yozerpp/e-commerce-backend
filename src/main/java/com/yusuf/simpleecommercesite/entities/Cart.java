package com.yusuf.simpleecommercesite.entities;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.yusuf.simpleecommercesite.entities.annotations.Cookie;
import com.yusuf.simpleecommercesite.entities.annotations.Entity;
import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
@JsonFilter("depth_5")
@Entity
public class Cart implements Serializable {
     @Id
     @GeneratedValue(strategy = GenerationType.IDENTITY)
     @Cookie
             @Column(name = "cartId")
     int id;
     @GeneratedValue
      java.math.BigDecimal total= BigDecimal.ZERO;
      @GeneratedValue
      boolean ordered=false;
     @OneToMany(mappedBy = "cartId")
     List<Sale> sales;

    // Empty constructor
    public Cart() {
    }
    // Constructors for combinations of fields that don't have  annotation
    public Cart(int id) {
        this.id = id;
    }

    // Getters and setters (including dirty flags)
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public java.math.BigDecimal getTotal() {
        return total;
    }
    public void setTotal( java.math.BigDecimal total) {
        this.total = total;
    }
    public boolean isOrdered() {
        return ordered;
    }
    public void setOrdered(boolean ordered) {
        this.ordered = ordered;
    }
    public List<Sale> getSales(){
        return this.sales;
    }
    public void setSales(List<Sale> sales) {
        this.sales = sales;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !getClass().isAssignableFrom(o.getClass())) return false;

        Cart cart = (Cart) o;

        return Objects.equals(getId(), cart.getId());
    }
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
