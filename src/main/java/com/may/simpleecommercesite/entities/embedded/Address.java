package com.may.simpleecommercesite.entities.embedded;

import com.may.simpleecommercesite.annotations.Embedded;

import java.io.Serializable;

@Embedded
public class Address implements Serializable {
    private String street;
    private String city;
    private String district;
    private String buildingNo;
    private String innerDoorNo;
    public Address(){}
    public Address(String street, String city, String district, String buildingNo, String innerDoorNo){
        this.street=street;
        this.city=city;
        this.district=district;
        this.buildingNo=buildingNo;
        this.innerDoorNo=innerDoorNo;
    }
    public String getBuildingNo() {
        return buildingNo;
    }

    public String getCity() {
        return city;
    }

    public String getDistrict() {
        return district;
    }

    public String getInnerDoorNo() {
        return innerDoorNo;
    }

    public String getStreet() {
        return street;
    }

    public void setBuildingNo(String buildingNo) {
        this.buildingNo = buildingNo;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public void setInnerDoorNo(String innerDoorNo) {
        this.innerDoorNo = innerDoorNo;
    }

    public void setStreet(String street) {
        this.street = street;
    }
}
