package com.yusuf.simpleecommercesite.entities;

import javax.persistence.*;
import com.yusuf.simpleecommercesite.entities.annotations.Entity;
@Entity
public class Image {
    @Id
            @GeneratedValue(strategy = GenerationType.IDENTITY)
            @Column(name = "imageId")
    private int id;
    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    @ManyToOne
    @Column(name = "productId")
    private Product product;

    public void setData(byte[] data) {
        this.data = data;
    }

    public void setMain(boolean main) {
        this.main = main;
    }
    @Lob
    private byte[] data;
    private boolean main;
    public Image(byte[] data, boolean main) {
        this.data = data;
        this.main = main;
    }

    public byte[] getData() {
        return data;
    }

    public boolean isMain() {
        return main;
    }
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Image(){
    }
    Image(byte[] data){
        this.data=data;
    }
}
