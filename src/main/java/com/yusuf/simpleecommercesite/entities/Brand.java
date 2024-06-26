package com.yusuf.simpleecommercesite.entities;

import com.yusuf.simpleecommercesite.entities.annotations.*;

@Metadata
@Entity
public class Brand {

@Id
@AutoGenerated
    @Column(name = "brandId")
    private int id;
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
