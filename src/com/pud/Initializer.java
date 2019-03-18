package com.pud;

import com.backendless.Backendless;
import com.backendless.BackendlessUser;
import com.backendless.persistence.DataQueryBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pud.model.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created on Mar 2019.
 */
public class Initializer {

    private String mDateString;
    private List<DiningType> mDiningTypes;

    public Initializer() {
        Backendless.initApp(Keys.APP, Keys.API);
        mDiningTypes = Backendless.Data.of(DiningType.class).find();
    }

    public void initPlaces() {
        System.out.println("Adding places...");
        long s = System.currentTimeMillis();

        JsonNode locationsNode = getLocationData();
        for (JsonNode locationNode : locationsNode) {
            Place place = new Place();
            place.setName(locationNode.get("Name").asText());

            JsonNode addressNode = locationNode.get("Address");
            String addressBuilder = addressNode.get("Street").asText() + ", " +
                    addressNode.get("City").asText() + ", " +
                    addressNode.get("State").asText() + " " +
                    addressNode.get("ZipCode").asText();

            place.setAddress(addressBuilder);
            place.setPhone(locationNode.get("PhoneNumber").asText());

            Backendless.Data.save(place);
        }

        System.out.println("Completed: " + (System.currentTimeMillis() - s) / 1000 + "s\n");
    }

    public void addMenu(String date) {
        mDateString = date;
        System.out.println("Adding menu for " + date + "...");
        long s = System.currentTimeMillis();

        List<Place> mPlaces = Backendless.Data.of(Place.class).find();

        int i = 1;
        for (Place place : mPlaces) {
            System.out.println(i++ + "/" + mPlaces.size() + ": " + place.getName());
            JsonNode mealNode = getMenuData(place.getName());
            for (JsonNode menuNode : mealNode) {
                if (menuNode.get("Status").asText().equals("Open"))
                    parseMenuNew(place, menuNode);
            }
//            printByPlace(place);
        }

        System.out.println("Completed: " + (System.currentTimeMillis() - s) / 1000 + "s\n");
    }

    private void parseMenuNew(Place place, JsonNode menuNode) {
        // Find Dining Type
        DiningType diningType = null;
        String name = menuNode.get("Name").asText();
        for (DiningType type : mDiningTypes) {
            if (type.getName().equals(name)) {
                diningType = type;
                break;
            }
        }

        // Intermediate save to Backendless
        DiningTiming diningTiming = new DiningTiming();

        try {
            JsonNode timeNode = menuNode.get("Hours");
            SimpleDateFormat format = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss");

            diningTiming.setFrom(format.parse(mDateString + " " + timeNode.get("StartTime").asText()));
            diningTiming.setTo(format.parse(mDateString + " " + timeNode.get("EndTime").asText()));
        } catch (ParseException e) {
            e.printStackTrace();
        }

        diningTiming = Backendless.Data.save(diningTiming);

        List<DiningType> dt = new ArrayList<>();
        dt.add(diningType);
        diningTiming.setDiningType(diningType);
        Backendless.Data.of(DiningTiming.class).setRelation(diningTiming, "diningType", dt);

        List<Place> pl = new ArrayList<>();
        pl.add(place);
        diningTiming.setOfPlace(place);
        Backendless.Data.of(DiningTiming.class).setRelation(diningTiming, "ofPlace", pl);

        List<DiningTiming> dtime = new ArrayList<>();
        dtime.add(diningTiming);
        place.getDiningTimings().add(diningTiming);
        diningType.getDiningTimings().add(diningTiming);
        Backendless.Data.of(Place.class).addRelation(place, "diningTimings", dtime);
        Backendless.Data.of(DiningType.class).addRelation(diningType, "diningTimings", dtime);

        continueParsing(diningTiming, menuNode);
    }

    private void continueParsing(DiningTiming diningTiming, JsonNode menuNode) {
        List<MenuSection> mSections = new ArrayList<>();
        JsonNode stations = menuNode.get("Stations");

        // Fetch ALL Menu Items
        // Have to do it this way because of 100 page limit
        DataQueryBuilder itemQueryBuilder = DataQueryBuilder.create();
        itemQueryBuilder.setPageSize(100);

        List<MenuItem> menuItemList = Backendless.Data.of(MenuItem.class).find(itemQueryBuilder);
        List<MenuItem> moreMenuItemList;

        for (int i = 0; i < 10; i++) {
            itemQueryBuilder.prepareNextPage();
            moreMenuItemList = Backendless.Data.of(MenuItem.class).find(itemQueryBuilder);
            if (moreMenuItemList.size() == 0) break;
            menuItemList.addAll(moreMenuItemList);
        }

        // Continue Parsing
        for (JsonNode stationNode : stations) {
            MenuSection menuSection = new MenuSection();
            menuSection.setName(stationNode.get("Name").asText().trim());
            menuSection = Backendless.Data.save(menuSection);
            menuSection.setMenuItems(new ArrayList<>());

            List<MenuItem> menuItemsToCreate = new ArrayList<>();

            JsonNode itemsNode = stationNode.get("Items");
            for (JsonNode itemNode : itemsNode) {
                MenuItem menuItem = new MenuItem();
                menuItem.setName(itemNode.get("Name").asText().trim());
                if (itemNode.get("IsVegetarian").asBoolean()) menuItem.setAllergens("Vegetarian");

                int i = menuItemList.indexOf(menuItem);
                if (i > -1) {
                    menuItem = menuItemList.get(i);
                } else {
                    menuItemList.add(menuItem);
                    menuItemsToCreate.add(menuItem);
                }

                menuSection.getMenuItems().add(menuItem);
            }

            List<String> newIDs = Backendless.Data.of(MenuItem.class).create(menuItemsToCreate);
            for (int i = 0; i < menuItemsToCreate.size(); i++) {
                menuItemsToCreate.get(i).setObjectId(newIDs.get(i));
            }

            menuSection.getMenuItems().addAll(menuItemsToCreate);
            Backendless.Data.of(MenuSection.class).addRelation(menuSection, "menuItems", menuSection.getMenuItems());
            mSections.add(menuSection);
        }

        diningTiming.setMenuSections(mSections);
        Backendless.Data.of(DiningTiming.class).addRelation(diningTiming, "menuSections", diningTiming.getMenuSections());
    }

    public void addComments(int count) {
        System.out.println("Adding comments...\n");
        List<DiningTiming> diningTimings = Backendless.Data.of(DiningTiming.class).find();
        BackendlessUser backendlessUser = Backendless.Data.of(BackendlessUser.class).findFirst();
        Random random = new Random();

        for (DiningTiming diningTiming : diningTimings) {
            int label = diningTiming.getComments().size();
            for (int i = 0; i < count; i++) {
                Comment comment = new Comment();

                String comText = diningTiming.getOfPlace().getName() + " - " + diningTiming.getDiningType().getName() + " - " + (++label);
                comment.setText(comText);
                comment.setRating(random.nextInt(9));

                comment = Backendless.Data.save(comment);

                List<BackendlessUser> ut = new ArrayList<>();
                ut.add(backendlessUser);
                Backendless.Data.of(Comment.class).setRelation(comment, "byUser", ut);

                List<Comment> ct = new ArrayList<>();
                ct.add(comment);
                Backendless.Data.of(DiningTiming.class).addRelation(diningTiming, "comments", ct);

                List<DiningTiming> dt = new ArrayList<>();
                dt.add(diningTiming);
                Backendless.Data.of(Comment.class).setRelation(comment, "ofDiningTiming", dt);
            }
        }
    }

    public void addRatings(int count) {
        System.out.println("Adding ratings...\n");
        List<DiningTiming> diningTimings = Backendless.Data.of(DiningTiming.class).find();
        Random random = new Random();

        for (DiningTiming diningTiming : diningTimings) {
            for (int i = 0; i < count; i++) {
                Rating rating = new Rating();
                rating.setRating(random.nextInt(9));
                rating = Backendless.Data.save(rating);

                List<DiningTiming> dt = new ArrayList<>();
                dt.add(diningTiming);
                Backendless.Data.of(Rating.class).setRelation(rating, "ofDiningTiming", dt);

                List<Rating> rt = new ArrayList<>();
                rt.add(rating);
                Backendless.Data.of(DiningTiming.class).addRelation(diningTiming, "ratings", rt);
            }
        }
    }

    public void resetTable() {
        System.out.println("Resetting database...\n");
        Backendless.Data.of(MenuItem.class).remove("created > 02-01-2000");
        Backendless.Data.of(MenuSection.class).remove("created > 02-01-2000");
        Backendless.Data.of(Place.class).remove("created > 02-01-2000");
        Backendless.Data.of(DiningTiming.class).remove("created > 02-01-2000");
        Backendless.Data.of(Comment.class).remove("created > 02-01-2000");
        Backendless.Data.of(Rating.class).remove("created > 02-01-2000");
        Backendless.Data.of(BackendlessUser.class).remove("email != 'test@purdue.edu'");
    }

    public void initSchema() {
        DiningType type = new DiningType();
        type.setName("Breakfast");
        Backendless.Data.save(type);

        DiningType type1 = new DiningType();
        type1.setName("Lunch");
        Backendless.Data.save(type1);

        DiningType type2 = new DiningType();
        type2.setName("Late Lunch");
        Backendless.Data.save(type2);

        DiningType type3 = new DiningType();
        type3.setName("Dinner");
        Backendless.Data.of(DiningType.class).save(type3);

        Place p = new Place();
        p.setName("dd");
        p.setPhone("dd");
        p.setAddress("sdsd");
        Backendless.Data.save(p);

        Rating r = new Rating();
        r.setRating(4);
        Backendless.Data.save(r);

        Comment c = new Comment();
        c.setRating(87);
        c.setText("Sdcsd");
        Backendless.Data.save(c);

        MenuSection ms = new MenuSection();
        ms.setName("dfdfb");
        Backendless.Data.save(ms);

        MenuItem mi = new MenuItem();
        mi.setName("Dfb");
        mi.setAllergens("DFng");
        Backendless.Data.save(mi);

        DiningTiming g = new DiningTiming();
        g.setDiningType(new DiningType());
        Backendless.Data.save(g);
    }

    private JsonNode getLocationData() {
        JsonNode node = null;
        try {
            String urlS = "https://api.hfs.purdue.edu/menus/v2/locations/";

            StringBuilder result = new StringBuilder();
            URL url = new URL(urlS);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));

            String line;
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }

            rd.close();

            ObjectMapper objectMapper = new ObjectMapper();
            node = objectMapper.readTree(result.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return node.get("Location");
    }

    private JsonNode getMenuData(String place) {
        JsonNode node = null;
        try {
            String urlS = "https://api.hfs.purdue.edu/menus/v2/locations/" + place.replace(" ", "%20") + "/" + mDateString + "/";

            StringBuilder result = new StringBuilder();
            URL url = new URL(urlS);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));

            String line;
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }

            rd.close();

            ObjectMapper objectMapper = new ObjectMapper();
            node = objectMapper.readTree(result.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return node.get("Meals");
    }

    private void printByPlace(Place place) {
        System.out.println(place.getName());
        for (DiningTiming diningTiming : place.getDiningTimings()) {
            System.out.println("    " + diningTiming.getDiningType().getName());
            System.out.println("    " + diningTiming.getFrom() + "  -  " + diningTiming.getTo());
            for (MenuSection menuSection : diningTiming.getMenuSections()) {
                System.out.println("        " + menuSection.getName());
                for (MenuItem menuItem : menuSection.getMenuItems()) {
                    System.out.println("            " + menuItem.getName());
                    if (menuItem.getAllergens() != null)
                        System.out.println("             - " + menuItem.getAllergens());
                }
            }
        }
    }

    private void printByType(DiningType diningType) {
        System.out.println(diningType.getName());
        for (DiningTiming diningTiming : diningType.getDiningTimings()) {
            System.out.println("    " + diningTiming.getOfPlace().getName());
            System.out.println("    " + diningTiming.getFrom() + "  -  " + diningTiming.getTo());
            for (MenuSection menuSection : diningTiming.getMenuSections()) {
                System.out.println("        " + menuSection.getName());
                for (MenuItem menuItem : menuSection.getMenuItems()) {
                    System.out.println("            " + menuItem.getName());
                    if (menuItem.getAllergens() != null)
                        System.out.println("             - " + menuItem.getAllergens());
                }
            }
        }
    }

    public void addMenuOld() {
        System.out.println("Adding menu...");
        long s = System.currentTimeMillis();

        JsonNode locationsNode = getLocationData();
        for (JsonNode locationNode : locationsNode) {
            Place place = new Place();
            place.setName(locationNode.get("Name").asText());

            JsonNode addressNode = locationNode.get("Address");
            String addressBuilder = addressNode.get("Street").asText() + ", " +
                    addressNode.get("City").asText() + ", " +
                    addressNode.get("State").asText() + " " +
                    addressNode.get("ZipCode").asText();

            place.setAddress(addressBuilder);
            place.setPhone(locationNode.get("PhoneNumber").asText());

            place = Backendless.Data.save(place);
            place.setDiningTimings(new ArrayList<>());

            JsonNode mealNode = getMenuData(place.getName());
            for (JsonNode menuNode : mealNode) {
                if (menuNode.get("Status").asText().equals("Open"))
                    parseMenuNew(place, menuNode);
            }

            place = Backendless.Data.of(Place.class).findById(place.getObjectId());
            printByPlace(place);
        }

        System.out.println((System.currentTimeMillis() - s) / 1000 + "s");
//        printByType();
    }

}
