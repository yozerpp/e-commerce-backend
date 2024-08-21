package com.yusuf.simpleecommercesite.entities.metadata;

import javax.persistence.*;

@Entity
@Table(name = "taxes")
public class TaxRates {
    public TaxRates() {
    }

    public float getKdv() {
        return kdv;
    }

    public void setKdv(float kdv) {
        this.kdv = kdv;
    }

    public float getOtv() {
        return otv;
    }

    public void setOtv(float otv) {
        this.otv = otv;
    }

    public float getGumruk() {
        return gumruk;
    }

    public void setGumruk(float gumruk) {
        this.gumruk = gumruk;
    }

    private float kdv;
    private float otv;
    private float gumruk;
    public enum TaxType {
        KDV,
        OTV,
        GUMRUK
    }
}
