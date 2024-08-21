package com.yusuf.simpleecommercesite.entities;

import javax.persistence.*;
import javax.validation.constraints.Size;

import com.yusuf.simpleecommercesite.entities.annotations.Metadata;
import com.yusuf.simpleecommercesite.entities.metadata.TaxRates;
import com.yusuf.simpleecommercesite.entities.annotations.Entity;
import java.util.Set;

@Entity
@Metadata
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
            @Column(name = "categoryId")
    int id;
    @ManyToOne
    Category parent;
    @Size(min = 4, max = 20)
    String name;
    @OneToMany(mappedBy = "parent")
    Set<Category> subCategories;

    public Set<TaxRates.TaxType> getTaxing() {
        return taxing;
    }

    public void setTaxing(Set<TaxRates.TaxType> taxing) {
        this.taxing = taxing;
    }
    Set<TaxRates.TaxType> taxing;

   public Category(){}
    public Category(int id){
       this.id = id;
    }
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public Category getParent() {
        return parent;
    }
    public void setParent(Category parent) {
        this.parent = parent;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public Set<Category> getSubCategories() {
        return subCategories;
    }
    public void setSubCategories(Set<Category> subCategories) {
        this.subCategories = subCategories;
    }
}
