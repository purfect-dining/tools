package com.pud.model;

import java.util.List;

public class Place {

    private String objectId;
    private String name;
    private String phone;
    private String address;
    private List<DiningTiming> diningTimings;

    public String getObjectId() {
        return objectId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public List<DiningTiming> getDiningTimings() {
        return diningTimings;
    }

    public void setDiningTimings(List<DiningTiming> diningTimings) {
        this.diningTimings = diningTimings;
    }

}