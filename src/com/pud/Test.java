package com.pud;

import java.io.IOException;
import java.text.ParseException;

public class Test {

    public static void main(String[] args) throws IOException, InterruptedException, ParseException {
        Initializer initializer = new Initializer();
//        initializer.resetTable() ;         // Clear everything except for DiningTypes and 1 test User
//        initializer.initPlaces();             // Add places from Purdue API
//        initializer.addMenu("04-25-2019");    // Add menu for date for all Places
//        initializer.addComments(3);           // Add comments (count) to all DiningTimings
        initializer.addRatings(1);            // Add ratings (count) to all DiningTimings
    }

}
