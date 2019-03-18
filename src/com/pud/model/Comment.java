package com.pud.model;

import com.backendless.BackendlessUser;

import java.util.Date;

public class Comment {

    private String objectId;
    private String text;
    private int rating;
    private BackendlessUser byUser;
    private Date created;

    public String getObjectId() {
        return objectId;
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public BackendlessUser getByUser() {
        return byUser;
    }

    public void setByUser(BackendlessUser byUser) {
        this.byUser = byUser;
    }

    public Date getCreated() {
        return created;
    }

}
