package com.yusuf.simpleecommercesite.entities.metadata;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "exchange_rates")
public class ExchangeRates {
    public ExchangeRates() {
    }

    public double getUsd() {
        return usd;
    }

    public void setUsd(double usd) {
        this.usd = usd;
    }

    public double getEur() {
        return eur;
    }

    public void setEur(double eur) {
        this.eur = eur;
    }

    public double getGbp() {
        return gbp;
    }
    public void setGbp(double gbp) {
        this.gbp = gbp;
    }

    public double getJpy() {
        return jpy;
    }

    public void setJpy(double jpy) {
        this.jpy = jpy;
    }
    public Date getLast_updated() {
        return last_updated;
    }

    public void setLast_updated(Date last_updated) {
        this.last_updated = last_updated;
    }

    public String getBase() {
        return base;
    }

    public void setBase(String base) {
        this.base = base;
    }

    public ExchangeRates(String base) {
        this.base = base;
    }

    @Id
    private String base;
    private double usd;
    private double eur;
    @Column(name = "pound")
    private double gbp;
    @Column(name = "yen")
    private double jpy;
    private Date last_updated;
}
