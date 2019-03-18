package com.pud.model;

public class Rating {

    private String objectId;
    private int rating;
    private DiningTiming ofDiningTiming;

    public String getObjectId() {
        return objectId;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public DiningTiming getOfDiningTiming() {
        return ofDiningTiming;
    }

    public void setOfDiningTiming(DiningTiming ofDiningTiming) {
        this.ofDiningTiming = ofDiningTiming;
    }

}