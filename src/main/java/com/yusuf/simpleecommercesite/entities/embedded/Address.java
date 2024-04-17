package com.yusuf.simpleecommercesite.entities.embedded;

import com.yusuf.simpleecommercesite.entities.annotations.Embedded;

import java.util.Objects;

@Embedded
public class Address {
    public Address() {
    }

    String city;

    public Address(String city, String district, String street, String innerDoorNo, String buildingNo, String neighborhood) {
        this.city = city;
        this.district = district;
        this.street = street;
        this.innerDoorNo = innerDoorNo;
        BuildingNo = buildingNo;
        this.neighborhood = neighborhood;
    }

    String district;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Address address = (Address) o;
        return Objects.equals(city, address.city) && Objects.equals(district, address.district) && Objects.equals(street, address.street) && Objects.equals(innerDoorNo, address.innerDoorNo) && Objects.equals(BuildingNo, address.BuildingNo) && Objects.equals(neighborhood, address.neighborhood);
    }

    @Override
    public int hashCode() {
        return Objects.hash(city, district, street, innerDoorNo, BuildingNo, neighborhood);
    }

    String street;
    String innerDoorNo;
    String BuildingNo;
    String neighborhood;

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getInnerDoorNo() {
        return innerDoorNo;
    }

    public void setInnerDoorNo(String innerDoorNo) {
        this.innerDoorNo = innerDoorNo;
    }

    public String getBuildingNo() {
        return BuildingNo;
    }

    public void setBuildingNo(String buildingNo) {
        BuildingNo = buildingNo;
    }

    public String getNeighborhood() {
        return neighborhood;
    }

    public void setNeighborhood(String neighborhood) {
        this.neighborhood = neighborhood;
    }
}
