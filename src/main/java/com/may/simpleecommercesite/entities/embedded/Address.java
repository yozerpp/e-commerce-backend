package com.may.simpleecommercesite.entities.embedded;

import com.may.simpleecommercesite.annotations.Embedded;

import java.io.Serializable;

@Embedded
public class Address implements Serializable {
    private String street;
    private String city;
    private String neighborhood;
    private String buildingNo;
    private String innerDoorNo;
    private String district;
    public Address(){}
    public Address( String city,String district, String neighborhood,String street, String buildingNo, String innerDoorNo){
        this.street=street;
        this.city=city;
        this.neighborhood = neighborhood;
        this.buildingNo=buildingNo;
        this.innerDoorNo=innerDoorNo;
        this.district=district;
    }
    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getBuildingNo() {
        return buildingNo;
    }

    public String getCity() {
        return city;
    }

    public String getNeighborhood() {
        return neighborhood;
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

    public void setNeighborhood(String neighborhood) {
        this.neighborhood = neighborhood;
    }

    public void setInnerDoorNo(String innerDoorNo) {
        this.innerDoorNo = innerDoorNo;
    }

    public void setStreet(String street) {
        this.street = street;
    }
}
