package controllers;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.time.DateUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import play.Play;
import play.db.jpa.JPA;
import play.mvc.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import models.*;
import util.DatabaseUtil;
import util.InfoUtil;
import util.JSONUtil;
import util.StringUtil;

import javax.json.*;

public class EasyCal extends Controller {

    public static void getConsumptions(String type, int userId) {
        if(type.equals("day")) { // get consumptions for given day
            String date = request.params.get("date");
            Date day = new Date();
            try {
                day = new SimpleDateFormat("yyyy-MM-dd").parse(date);
            } catch (ParseException e) {
                JsonObject errorRes = Json.createObjectBuilder()
                        .add("status", 400)
                        .add("error", "Bad date format")
                        .build();
                renderJSON(errorRes.toString());
            }
            List<Consumption> consumptions = DatabaseUtil.getConsumptionsForDay(userId, day);

            HashMap<Meal, List<Consumption>> meals = new HashMap<>();
            meals.put(Meal.BREAKFAST, new ArrayList<>());
            meals.put(Meal.LUNCH, new ArrayList<>());
            meals.put(Meal.DINNER, new ArrayList<>());
            meals.put(Meal.SNACKS, new ArrayList<>());
            for(Consumption consumption : consumptions) {
                Meal meal = consumption.meal;
                List<Consumption> consumptionsForMeal = meals.get(meal);
                consumptionsForMeal.add(consumption);
                meals.put(meal, consumptionsForMeal);
            }

            JsonObjectBuilder res = Json.createObjectBuilder();
            for(Meal meal : Meal.values()) {
                JsonArrayBuilder items = Json.createArrayBuilder();
                for(Consumption c : meals.get(meal)) {
                    items.add(JSONUtil.buildMealItem(c));
                }
                res.add(meal.name().toLowerCase(), Json.createObjectBuilder()
                        .add("items", items));
            }
            renderJSON(res.build().toString());
        } else if(type.equals("recent")) { // get list of recently added food items
            List<Consumption> consumptions = DatabaseUtil.getRecentConsumptions(userId);
            JsonArrayBuilder items = Json.createArrayBuilder();
            for(Consumption c : consumptions) {
                items.add(JSONUtil.buildMealItem(c));
            }
            renderJSON(items.build().toString());
        }

    }

    public static void deleteConsumption(int consumptionId) {
        response.status = DatabaseUtil.deleteConsumption(consumptionId) ? 204 : 404;
        renderText("");
    }

    public static void updateConsumption(int consumptionId) {
        try {
            String reqBody = IOUtils.toString(request.body, "UTF-8");
            JSONObject reqObj = new JSONObject(reqBody);
            int servingSizeId = reqObj.getJSONObject("selectedServing")
                    .getJSONObject("servingSize").getInt("id");
            double quantity = reqObj.getJSONObject("selectedServing")
                    .getDouble("quantity");
            response.status =
                    DatabaseUtil.updateConsumption(consumptionId, servingSizeId, quantity) ? 200 : 404;
        } catch (IOException e) {
            e.printStackTrace();
            response.status = 404;
        } finally {
            renderText("");
        }
    }

    public static void searchFoods(String q) {
        try {
            URL endpoint = new URL(
                String.format("https://api.nal.usda.gov/ndb/search/?format=json&q=%s&sort=r&max=50&api_key=%s",
                q, Play.configuration.getProperty("usda.apikey")));
            HttpURLConnection con = (HttpURLConnection) endpoint.openConnection();

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer res = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                res.append(inputLine);
            }
            in.close();

            JSONObject resObj = new JSONObject(res.toString());
            JSONArray resArr = resObj.getJSONObject("list").getJSONArray("item");

            JSONArray processedList = new JSONArray();
            for(int i = 0; i < resArr.length() && processedList.length() <= 25; i++) {
                JSONObject item = resArr.getJSONObject(i);
                String itemName = StringUtil.formatFoodItemName(item.getString("name"));
                // filter out items with super long names
                if(itemName.length() <= 80) {
                    processedList.put(new JSONObject()
                            .put("name", itemName)
                            .put("foodItemId", item.getString("ndbno")));
                }
            }
            renderJSON(processedList.toString());
        } catch (Exception e) {
            e.printStackTrace();
            response.status = 404;
            renderText("");
        }
    }

    public static void getFoodDetails(String ndbno) {
        try {
            URL endpoint = new URL(
                    String.format("https://api.nal.usda.gov/ndb/V2/reports?ndbno=%s&type=f&format=json&api_key=%s",
                            ndbno, Play.configuration.getProperty("usda.apikey")));
            HttpURLConnection con = (HttpURLConnection) endpoint.openConnection();

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer res = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                res.append(inputLine);
            }
            in.close();

            JSONObject resObj = new JSONObject(res.toString());
            JSONObject foodObj = resObj.getJSONArray("foods").getJSONObject(0).getJSONObject("food");

            String foodItemId = foodObj.getJSONObject("desc").getString("ndbno");
            String name = StringUtil.formatFoodItemName(foodObj.getJSONObject("desc").getString("name"));
            JSONArray nutrientsArr = foodObj.getJSONArray("nutrients");

            // get list of serving sizes
            JSONArray measuresArr = nutrientsArr.getJSONObject(0).getJSONArray("measures");
            List<ServingSize> servingSizes = new ArrayList<>();
            for(int i = 0; i < measuresArr.length(); i++) {
                JSONObject servingObj = measuresArr.getJSONObject(i);
                ServingLabel label = new ServingLabel();
                label.labelValue = StringUtil.formatServingSizeLabel(servingObj.getString("label"));
                ServingSize servingSize = new ServingSize();
                servingSize.label = label;
                servingSize.ratio = (double) servingObj.getInt("eqv") / 100;
                servingSizes.add(servingSize);
            }

            // get nutrition information
            HashMap<String, Double> nutritionMap = new HashMap<>();
            nutritionMap.put("calories", -1.0);
            nutritionMap.put("carbs", -1.0);
            nutritionMap.put("fat", -1.0);
            nutritionMap.put("protein", -1.0);
            nutritionMap.put("fiber", -1.0);
            nutritionMap.put("sugar", -1.0);
            nutritionMap.put("sodium", -1.0);
            int i = 0;
            while(InfoUtil.containsNegative(new double[]
                    {nutritionMap.get("calories"), nutritionMap.get("carbs"), nutritionMap.get("fat"),
                     nutritionMap.get("protein"), nutritionMap.get("fiber"), nutritionMap.get("sugar"), nutritionMap.get("sodium")})) {
                JSONObject nutrientObj = nutrientsArr.getJSONObject(i);
                switch(nutrientObj.getString("name")) {
                    case "Energy":
                        nutritionMap.put("calories", nutrientObj.getDouble("value"));
                        break;
                    case "Carbohydrate, by difference":
                        nutritionMap.put("carbs", nutrientObj.getDouble("value"));
                        break;
                    case "Total lipid (fat)":
                        nutritionMap.put("fat", nutrientObj.getDouble("value"));
                        break;
                    case "Protein":
                        nutritionMap.put("protein", nutrientObj.getDouble("value"));
                        break;
                    case "Fiber, total dietary":
                        nutritionMap.put("fiber", nutrientObj.getDouble("value"));
                        break;
                    case "Sugars, total":
                        nutritionMap.put("sugar", nutrientObj.getDouble("value"));
                        break;
                    case "Sodium, Na":
                        nutritionMap.put("sodium", nutrientObj.getDouble("value"));
                        break;
                }
                i++;
                if(i == nutrientsArr.length()) { // don't have values for some nutrient(s)
                    for(String nutrient : nutritionMap.keySet()) {
                        // set all default -1.0 values to 0
                        if(nutritionMap.get(nutrient) < 0) {
                            nutritionMap.put(nutrient, 0.0);
                        }
                    }
                }
            }
            FoodItem foodItem = new FoodItem(foodItemId, name, servingSizes,
                    nutritionMap.get("calories"), nutritionMap.get("carbs"), nutritionMap.get("fat"),
                    nutritionMap.get("protein"), nutritionMap.get("fiber"), nutritionMap.get("sugar"), nutritionMap.get("sodium"));
            JsonObjectBuilder item = JSONUtil.buildMealItem(foodItem);

            renderJSON(item.build().toString());
        } catch (Exception e) {
            e.printStackTrace();
            response.status = 404;
            renderText("");
        }
    }

    public static void createNewConsumption() {
        try {
            String reqBody = IOUtils.toString(request.body, "UTF-8");
            JSONObject reqObj = new JSONObject(reqBody);
            JSONObject selectedServingObj = reqObj.getJSONObject("selectedServing");
            int selectedServingId = -1;

            // get corresponding food item
            FoodItem foodItem = DatabaseUtil.getFoodItem(reqObj.getString("foodItemId"));

            if(foodItem == null) {
                // create food item
                foodItem = new FoodItem();
                foodItem.id = reqObj.getString("foodItemId");
                foodItem.name = reqObj.getString("name");
                foodItem.calories = reqObj.getDouble("calories");
                foodItem.carbs = reqObj.getDouble("carbs");
                foodItem.fat = reqObj.getDouble("fat");
                foodItem.fiber = reqObj.getDouble("fiber");
                foodItem.sugar = reqObj.getDouble("sugar");
                foodItem.sodium = reqObj.getDouble("sodium");
                JPA.em().persist(foodItem);

                // create serving size entities
                JSONArray servingSizesArr = reqObj.getJSONArray("servingSizes");
                for(int i = 0; i < servingSizesArr.length(); i++) {
                    JSONObject servingObj = servingSizesArr.getJSONObject(i);
                    String servingLabel = servingObj.getString("label");
                    ServingLabel label = new ServingLabel();
                    label.labelValue = servingLabel;
                    JPA.em().persist(label);

                    ServingSize servingSize = new ServingSize();
                    servingSize.foodItem = foodItem;
                    servingSize.label = label;
                    servingSize.ratio = servingObj.getDouble("ratio");
                    JPA.em().persist(servingSize);
                    selectedServingId = servingSize.id;
                }
            } else {
                selectedServingId = DatabaseUtil.findServingSizeId(foodItem,
                        selectedServingObj.getJSONObject("servingSize").getString("label"));
            }

            // create consumption
            Consumption consumption = new Consumption();
            User user = DatabaseUtil.getUser(reqObj.getInt("userId"));
            consumption.user = user;
            consumption.foodItem = foodItem;
            ServingSize servingSize = DatabaseUtil.getServingSize(selectedServingId);
            consumption.servingSize = servingSize;
            consumption.servingQuantity = selectedServingObj.getDouble("quantity");
            consumption.meal = Meal.valueOf(reqObj.getString("meal").toUpperCase());
            consumption.day = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);
            JPA.em().persist(consumption);
        } catch (Exception e) {
            e.printStackTrace();
            response.status = 404;
        } finally {
            renderText("");
        }
    }

}