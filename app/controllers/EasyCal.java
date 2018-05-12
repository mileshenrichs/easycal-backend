package controllers;

import org.apache.commons.io.IOUtils;
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
            nutritionMap.put("carbs", -1.0);
            nutritionMap.put("fat", -1.0);
            nutritionMap.put("protein", -1.0);
            nutritionMap.put("fiber", -1.0);
            nutritionMap.put("sugar", -1.0);
            nutritionMap.put("sodium", -1.0);
            int i = 0;
            while(InfoUtil.containsNegative(new double[]
                    {nutritionMap.get("carbs"), nutritionMap.get("fat"), nutritionMap.get("protein"),
                    nutritionMap.get("fiber"), nutritionMap.get("sugar"), nutritionMap.get("sodium")})) {
                JSONObject nutrientObj = nutrientsArr.getJSONObject(i);
                switch(nutrientObj.getString("name")) {
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
                // if don't have values for some nutrient(s), set all default -1.0 values to 0
                if(i == nutrientsArr.length()) {
                    for(String nutrient : nutritionMap.keySet()) {
                        if(nutritionMap.get(nutrient) < 0) {
                            nutritionMap.put(nutrient, 0.0);
                        }
                    }
                }
            }
            // calculate calories from macros (database is often inaccurate)
            double calories = InfoUtil.calculateCalories(nutritionMap.get("carbs"), nutritionMap.get("fat"), nutritionMap.get("protein"));
            FoodItem foodItem = new FoodItem(foodItemId, name, servingSizes,
                    calories, nutritionMap.get("carbs"), nutritionMap.get("fat"),
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
                // create food item if doesn't exist
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
                    String servingLabelString = servingObj.getString("label");
                    ServingLabel servingLabel = DatabaseUtil.getServingLabelByValue(servingLabelString);
                    if(servingLabel == null) {
                        servingLabel = new ServingLabel();
                        servingLabel.labelValue = servingLabelString;
                        JPA.em().persist(servingLabel);
                    }

                    ServingSize servingSize = new ServingSize();
                    servingSize.foodItem = foodItem;
                    servingSize.label = servingLabel;
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
            String day = reqObj.getString("day");
            consumption.day = new SimpleDateFormat("yyyy-MM-dd").parse(day);
            JPA.em().persist(consumption);
        } catch (Exception e) {
            e.printStackTrace();
            response.status = 404;
        } finally {
            renderText("");
        }
    }

    public static void createFood() {
        try {
            String reqBody = IOUtils.toString(request.body, "UTF-8");
            JSONObject reqObj = new JSONObject(reqBody);
            int userId = reqObj.getInt("userId");

            FoodItem foodItem = new FoodItem();
            foodItem.id = InfoUtil.generateFoodItemId();
            foodItem.name = reqObj.getString("name");
            foodItem.carbs = StringUtil.processNutrientString(reqObj.getString("carbs"));
            foodItem.fat = StringUtil.processNutrientString(reqObj.getString("fat"));
            foodItem.protein = StringUtil.processNutrientString(reqObj.getString("protein"));
            foodItem.calories = InfoUtil.calculateCalories(foodItem.carbs, foodItem.fat, foodItem.protein);
            foodItem.fiber = StringUtil.processNutrientString(reqObj.getString("fiber"));
            foodItem.sugar = StringUtil.processNutrientString(reqObj.getString("sugar"));
            foodItem.sodium = StringUtil.processNutrientString(reqObj.getString("sodium"));
            foodItem.creator = DatabaseUtil.getUser(userId);
            JPA.em().persist(foodItem);

            StringUtil.UserServingSize ss = StringUtil.processServingSizeString(reqObj.getString("servingsize"));
            ServingLabel servingLabel = DatabaseUtil.getServingLabelByValue(ss.label);
            if(servingLabel == null) {
                servingLabel = new ServingLabel();
                servingLabel.labelValue = ss.label;
                JPA.em().persist(servingLabel);
            }
            ServingSize servingSize = new ServingSize();
            servingSize.foodItem = foodItem;
            servingSize.label = servingLabel;
            servingSize.ratio = 1.0 / ss.amount;
            JPA.em().persist(servingSize);
        } catch (Exception e) {
            e.printStackTrace();
            response.status = 404;
        }
        renderText("");
    }

    public static void getUserCreatedFoods(int userId) {
        List<FoodItem> userFoods = DatabaseUtil.getUserFoodItems(userId);
        if(userFoods.size() > 0) {
            JsonArrayBuilder arr = Json.createArrayBuilder();
            for (FoodItem item : userFoods) {
                arr.add(JSONUtil.buildMealItem(item));
            }
            renderJSON(arr.build().toString());
        } else {
            response.status = 404;
            renderText("");
        }
    }

    public static void deleteUserCreatedFood(String id) {
        response.status = DatabaseUtil.deleteUserFoodItem(id) ? 200 : 404;
        renderText("");
    }

    public static void addOrUpdateExercise() {
        try {
            String reqBody = IOUtils.toString(request.body, "UTF-8");
            System.out.println(reqBody);
            JSONObject reqObj = new JSONObject(reqBody);

            int userId = reqObj.getInt("userId");
            String dateStr = reqObj.getString("day").substring(0, 10);
            Date day = new SimpleDateFormat("yyyy-MM-dd").parse(dateStr);
            Exercise exercise = DatabaseUtil.getExerciseForDay(userId, day);
            if(exercise == null) { // create new exercise entry for day
                exercise = new Exercise();
                exercise.user = DatabaseUtil.getUser(userId);
                exercise.caloriesBurned = reqObj.getInt("caloriesBurned");
                exercise.day = day;
                JPA.em().persist(exercise);
            } else { // update existing exercise entry
                if(reqObj.getInt("caloriesBurned") == 0) {
                    JPA.em().remove(exercise); // just delete exercise entry if set to 0
                } else {
                    exercise.caloriesBurned = reqObj.getInt("caloriesBurned");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public static void getExercise(int userId, String date) {
        try {
            Date day = new SimpleDateFormat("yyyy-MM-dd").parse(date);
            Exercise exercise = DatabaseUtil.getExerciseForDay(userId, day);
            int caloriesBurned = exercise != null ? exercise.caloriesBurned : 0;
            JsonObjectBuilder res = Json.createObjectBuilder()
                    .add("caloriesBurned", caloriesBurned);
            renderJSON(res.build().toString());
        } catch (ParseException e) {
            e.printStackTrace();
            response.status = 404;
            renderText("");
        }
    }

}