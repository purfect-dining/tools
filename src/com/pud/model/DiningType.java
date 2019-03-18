package com.pud.model;

import java.util.List;

public class DiningType {

    private String objectId;
    private String name;
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

    public List<DiningTiming> getDiningTimings() {
        return diningTimings;
    }

    public void setDiningTimings(List<DiningTiming> diningTimings) {
        this.diningTimings = diningTimings;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof DiningType && this.name.equals(((DiningType) o).getName());
    }
}