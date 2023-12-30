package com.may.simpleecommercesite.entities;

import com.may.simpleecommercesite.annotations.Cookie;
import com.may.simpleecommercesite.annotations.Id;

import java.io.Serializable;

public class Customer implements Serializable {
    @Id
    @Cookie
    int cookieId;
    public Customer(){}
    public Customer(int c){
        this.cookieId=c;
    }
    public void setCookieId(int cookieId) {
        this.cookieId = cookieId;
    }

    public int getCookieId() {
        return cookieId;
    }
}
