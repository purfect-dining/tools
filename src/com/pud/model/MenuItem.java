package com.pud.model;

public class MenuItem {

    private String objectId;
    private String name;
    private String allergens;

    public String getObjectId() {
        return objectId;
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAllergens() {
        return allergens;
    }

    public void setAllergens(String allergens) {
        this.allergens = allergens;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof MenuItem && this.name.equals(((MenuItem) o).getName());
    }
}