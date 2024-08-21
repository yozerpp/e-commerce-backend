package com.yusuf.simpleecommercesite.entities;

import com.yusuf.simpleecommercesite.entities.annotations.Entity;
import com.yusuf.simpleecommercesite.entities.annotations.Metadata;

import javax.persistence.*;
import javax.validation.constraints.Size;

@Metadata
@Entity
public class Brand {

@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "brandId")
    private int id;
    @Size(min = 6, max = 20)
    private String name;

    public Brand() {
    }
    public Brand(int id) {
        this.id = id;
    }
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
