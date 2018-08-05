package controllers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureException;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import play.Logger;
import play.Play;
import play.db.jpa.JPA;
import play.mvc.*;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import models.*;
import util.*;

import javax.json.*;

public class EasyCal extends Controller {

    /**
     * Ensure that every request is sent with a valid token as query param
     */
    @Before(unless = {"testEndpoint", "searchFoods", "getFoodDetails"})
    static void checkAuth() {
        String token = null;
        boolean authenticated = false;

        if(params.get("token") != null) {
            token = params.get("token");
        }

        if(token != null) {
            try {
                Jwts.parser().setSigningKey(Play.secretKey).parseClaimsJws(token);
                // JWT is valid
                authenticated = true;
            } catch (Exception e) {
                // invalid JWT
                Logger.info("SignatureException: invalid JWT");
            }
        }

        if(!authenticated) {
            response.setHeader("Access-Control-Allow-Origin", "*");
            response.status = 403;
            renderText("");
        }
    }

    public static void getConsumptions(String type, int userId) {
        response.setHeader("Access-Control-Allow-Origin", "*");
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
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.status = DatabaseUtil.deleteConsumption(consumptionId) ? 204 : 404;
        renderText("");
    }

    public static void updateConsumption(int consumptionId) {
        try {
            response.setHeader("Access-Control-Allow-Origin", "*");
            String reqBody = IOUtils.toString(request.body, "UTF-8");

            JSONObject reqObj = new JSONObject(reqBody);
            int servingSizeId = reqObj.getJSONObject("consumption").getJSONObject("selectedServing")
                    .getJSONObject("servingSize").getInt("id");
            double quantity = reqObj.getJSONObject("consumption").getJSONObject("selectedServing")
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
            response.setHeader("Access-Control-Allow-Origin", "*");
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
            for(int i = 0; i < resArr.length() && processedList.length() <= 40; i++) {
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
            if(e.getMessage().equals("JSONObject[\"list\"] not found.")) {
                Logger.info("No results found for search query");
            } else {
                e.printStackTrace();
            }
            response.status = 404;
            renderText("");
        }
    }

    public static void getFoodDetails(String ndbno) {
        try {
            response.setHeader("Access-Control-Allow-Origin", "*");
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

            // make sure there are actually measures provided
            if(measuresArr.length() > 0) {
                for(int i = 0; i < measuresArr.length(); i++) {
                    JSONObject servingObj = measuresArr.getJSONObject(i);
                    ServingLabel label = new ServingLabel();
                    label.labelValue = StringUtil.formatServingSizeLabel(servingObj.getString("label"));
                    ServingSize servingSize = new ServingSize();
                    servingSize.label = label;
                    servingSize.ratio = (double) servingObj.getInt("eqv") / 100;
                    servingSizes.add(servingSize);
                }
            } else { // create default 100 g measure
                ServingSize servingSize = new ServingSize();
                servingSize.label = JPA.em().find(ServingLabel.class, 28); // 28 is id of "g" serving label
                servingSize.ratio = 0.01;
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
            response.setHeader("Access-Control-Allow-Origin", "*");
            String reqBody = IOUtils.toString(request.body, "UTF-8");

            JSONObject consumptionObj = new JSONObject(reqBody).getJSONObject("consumption");
            JSONObject selectedServingObj = consumptionObj.getJSONObject("selectedServing");
            int selectedServingId = -1;

            // get corresponding food item
            FoodItem foodItem = DatabaseUtil.getFoodItem(consumptionObj.getString("foodItemId"));

            if(foodItem == null) {
                // create food item if doesn't exist
                foodItem = new FoodItem();
                foodItem.id = consumptionObj.getString("foodItemId");
                foodItem.name = consumptionObj.getString("name");
                foodItem.calories = consumptionObj.getDouble("calories");
                foodItem.carbs = consumptionObj.getDouble("carbs");
                foodItem.fat = consumptionObj.getDouble("fat");
                foodItem.protein = consumptionObj.getDouble("protein");
                foodItem.fiber = consumptionObj.getDouble("fiber");
                foodItem.sugar = consumptionObj.getDouble("sugar");
                foodItem.sodium = consumptionObj.getDouble("sodium");
                JPA.em().persist(foodItem);

                // create serving size entities
                JSONArray servingSizesArr = consumptionObj.getJSONArray("servingSizes");
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
                    // set selectedServingId only if given serving size was selected
                    if(selectedServingObj.getJSONObject("servingSize").getInt("id") == i) {
                        selectedServingId = servingSize.id;
                    }
                }
            } else {
                selectedServingId = DatabaseUtil.findServingSizeId(foodItem,
                        selectedServingObj.getJSONObject("servingSize").getString("label"));
            }

            // create consumption
            Consumption consumption = new Consumption();
            User user = DatabaseUtil.getUser(consumptionObj.getInt("userId"));
            consumption.user = user;
            consumption.foodItem = foodItem;
            ServingSize servingSize = DatabaseUtil.getServingSize(selectedServingId);
            consumption.servingSize = servingSize;
            consumption.servingQuantity = selectedServingObj.getDouble("quantity");
            consumption.meal = Meal.valueOf(consumptionObj.getString("meal").toUpperCase());
            String day = consumptionObj.getString("day");
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
            response.setHeader("Access-Control-Allow-Origin", "*");
            String reqBody = IOUtils.toString(request.body, "UTF-8");
            JSONObject foodObj = new JSONObject(reqBody).getJSONObject("foodItem");
            int userId = foodObj.getInt("userId");

            FoodItem foodItem = new FoodItem();
            foodItem.id = InfoUtil.generateFoodItemId();
            foodItem.name = foodObj.getString("name");
            foodItem.carbs = StringUtil.processNutrientString(foodObj.getString("carbs"));
            foodItem.fat = StringUtil.processNutrientString(foodObj.getString("fat"));
            foodItem.protein = StringUtil.processNutrientString(foodObj.getString("protein"));
            foodItem.calories = InfoUtil.calculateCalories(foodItem.carbs, foodItem.fat, foodItem.protein);
            foodItem.fiber = StringUtil.processNutrientString(foodObj.getString("fiber"));
            foodItem.sugar = StringUtil.processNutrientString(foodObj.getString("sugar"));
            foodItem.sodium = StringUtil.processNutrientString(foodObj.getString("sodium"));
            foodItem.creator = DatabaseUtil.getUser(userId);
            JPA.em().persist(foodItem);

            StringUtil.UserServingSize ss = StringUtil.processServingSizeString(foodObj.getString("servingsize"));
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
        response.setHeader("Access-Control-Allow-Origin", "*");
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
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.status = DatabaseUtil.deleteUserFoodItem(id) ? 200 : 404;
        renderText("");
    }

    public static void getFoodMealGroup(int foodMealGroupId) {
        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
        FoodMealGroup foodMealGroup = DatabaseUtil.getFoodMealGroupById(foodMealGroupId);
        if(foodMealGroup == null) {
            response.status = 404;
            renderText("");
        } else {
            renderJSON(gson.toJson(foodMealGroup, new TypeToken<FoodMealGroup>(){}.getType()));
        }
    }

    public static void getUserFoodMealGroups(int userId) {
        response.setHeader("Access-Control-Allow-Origin", "*");
        List<FoodMealGroup> userMealGroups = DatabaseUtil.getUserFoodMealGroups(userId);

        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
        String mealGroupsJson = gson.toJson(userMealGroups);
        renderJSON(mealGroupsJson);
    }

    public static void addFoodToMealGroup(String body, int foodMealGroupId) {
        JSONObject reqObj = new JSONObject(body);

        String foodItemId = reqObj.getString("foodItemId");
        int servingSizeId = reqObj.getInt("selectedServingSizeId");
        double servingQuantity = reqObj.getDouble("servingQuantity");

        FoodMealGroupItem newMealGroupItem = new FoodMealGroupItem();
        newMealGroupItem.foodMealGroup = DatabaseUtil.getFoodMealGroupById(foodMealGroupId);
        newMealGroupItem.foodItem = DatabaseUtil.getFoodItem(foodItemId);
        newMealGroupItem.defaultServingSize = DatabaseUtil.getServingSize(servingSizeId);
        newMealGroupItem.defaultServingQuantity = servingQuantity;
        JPA.em().persist(newMealGroupItem);

        ok();
    }

    public static void updateFoodMealGroup(String body) {
        response.setHeader("Access-Control-Allow-Origin", "*");
        Gson gson = new Gson();
        FoodMealGroup updatedMealGroup = gson.fromJson(body, new TypeToken<FoodMealGroup>(){}.getType());

        // clear out all previous food items
        FoodMealGroup originalMealGroup = JPA.em().find(FoodMealGroup.class, updatedMealGroup.id);
        for(FoodMealGroupItem foodItem : originalMealGroup.mealGroupItems) {
            JPA.em().remove(foodItem);
        }

        // rebuild based on updated group
        for(int i = 0; i < updatedMealGroup.mealGroupItems.size(); i++) {
            FoodMealGroupItem newFoodItem = updatedMealGroup.mealGroupItems.get(i);
            FoodItem correspondingFoodItem = JPA.em().find(FoodItem.class, newFoodItem.foodItem.id);
            ServingSize correspondingDefaultServingSize = JPA.em().find(ServingSize.class, newFoodItem.defaultServingSize.id);

            newFoodItem.id = null;
            newFoodItem.foodMealGroup = originalMealGroup;
            newFoodItem.foodItem = correspondingFoodItem;
            newFoodItem.defaultServingSize = correspondingDefaultServingSize;
            JPA.em().persist(newFoodItem);
        }
        ok();
    }

    public static void addOrUpdateExercise() {
        try {
            response.setHeader("Access-Control-Allow-Origin", "*");
            String reqBody = IOUtils.toString(request.body, "UTF-8");
            JSONObject activityObj = new JSONObject(reqBody).getJSONObject("activity");

            int userId = activityObj.getInt("userId");
            String dateStr = activityObj.getString("day").substring(0, 10);
            Date day = new SimpleDateFormat("yyyy-MM-dd").parse(dateStr);
            Exercise exercise = DatabaseUtil.getExerciseForDay(userId, day);
            if(exercise == null) { // create new exercise entry for day
                exercise = new Exercise();
                exercise.user = DatabaseUtil.getUser(userId);
                exercise.caloriesBurned = activityObj.getInt("caloriesBurned");
                exercise.day = day;
                JPA.em().persist(exercise);
            } else { // update existing exercise entry
                if(activityObj.getInt("caloriesBurned") == 0) {
                    JPA.em().remove(exercise); // just delete exercise entry if set to 0
                } else {
                    exercise.caloriesBurned = activityObj.getInt("caloriesBurned");
                }
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }

    public static void getExercise(int userId, String date) {
        try {
            response.setHeader("Access-Control-Allow-Origin", "*");
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

    public static void setGoals() {
        try {
            response.setHeader("Access-Control-Allow-Origin", "*");
            String reqBody = IOUtils.toString(request.body, "UTF-8");
            JSONObject goalsObj = new JSONObject(reqBody).getJSONObject("goals");
            int userId = goalsObj.getInt("userId");

            User user = JPA.em().find(User.class, userId);
            List<Goal> userGoals = user.goals;
            HashMap<Goal.GoalCategory, Goal> goalMap = new HashMap<>();
            for(Goal goal : userGoals) {
                goalMap.put(goal.goalCategory, goal);
            }

            for(Goal.GoalCategory category : Goal.GoalCategory.values()) {
                String categoryString = category.toString().toLowerCase();
                if(goalsObj.has(categoryString)) {
                    if(goalMap.containsKey(category)) {
                        Goal categoryGoal = goalMap.get(category);
                        if(categoryGoal.goalValue != goalsObj.getInt(categoryString)) {
                            categoryGoal.goalValue = goalsObj.getInt(categoryString);
                        }
                    } else {
                        Goal categoryGoal = new Goal();
                        categoryGoal.user = user;
                        categoryGoal.goalCategory = category;
                        categoryGoal.goalValue = goalsObj.getInt(categoryString);
                        JPA.em().persist(categoryGoal);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            response.status = 404;
        }
        renderText("");
    }

    public static void getUserGoals(int userId) {
        response.setHeader("Access-Control-Allow-Origin", "*");
        List<Goal> userGoals = JPA.em().find(User.class, userId).goals;
        HashMap<Goal.GoalCategory, Goal> goalMap = new HashMap<>();
        for(Goal goal : userGoals) {
            goalMap.put(goal.goalCategory, goal);
        }

        JsonObjectBuilder res = Json.createObjectBuilder()
                .add("calories", goalMap.containsKey(Goal.GoalCategory.CALORIES) ? goalMap.get(Goal.GoalCategory.CALORIES).goalValue : -1)
                .add("carbs", goalMap.containsKey(Goal.GoalCategory.CARBS) ? goalMap.get(Goal.GoalCategory.CARBS).goalValue : -1)
                .add("fat", goalMap.containsKey(Goal.GoalCategory.FAT) ? goalMap.get(Goal.GoalCategory.FAT).goalValue : -1)
                .add("protein", goalMap.containsKey(Goal.GoalCategory.PROTEIN) ? goalMap.get(Goal.GoalCategory.PROTEIN).goalValue : -1)
                .add("fiber", goalMap.containsKey(Goal.GoalCategory.FIBER) ? goalMap.get(Goal.GoalCategory.FIBER).goalValue : -1)
                .add("sugar", goalMap.containsKey(Goal.GoalCategory.SUGAR) ? goalMap.get(Goal.GoalCategory.SUGAR).goalValue : -1)
                .add("sodium", goalMap.containsKey(Goal.GoalCategory.SODIUM) ? goalMap.get(Goal.GoalCategory.SODIUM).goalValue : -1);
        renderJSON(res.build().toString());
    }

    public static void getCumulativeStatistics(int userId, String dateFrom, String dateTo) {
        try {
            response.setHeader("Access-Control-Allow-Origin", "*");
            Date fromDay = new SimpleDateFormat("yyyy-MM-dd").parse(dateFrom.substring(0, 10));
            Date toDay = new SimpleDateFormat("yyyy-MM-dd").parse(dateTo.substring(0, 10));
            List<Consumption> consumptions = DatabaseUtil.getConsumptionsInRange(userId, fromDay, toDay);

            // initialize JSON response objects
            JsonArrayBuilder totalsArr = Json.createArrayBuilder();
            JsonObjectBuilder overallTotalsObj = Json.createObjectBuilder();
            JsonObjectBuilder averagesObj = Json.createObjectBuilder();

            // check if there are any consumptions in given time frame
            if(consumptions.size() > 0) {
                List<List<Consumption>> consumptionsByDay = new ArrayList<>();
                List<Consumption> dailyConsumptions = new ArrayList<>();
                Date currentDay = consumptions.get(0).day;
                for(Consumption consumption : consumptions) {
                    Date consumptionDate = consumption.day;
                    if(consumptionDate.compareTo(currentDay) == 0) {
                        dailyConsumptions.add(consumption);
                    } else {
                        consumptionsByDay.add(dailyConsumptions);
                        dailyConsumptions = new ArrayList<>();
                        dailyConsumptions.add(consumption);
                        currentDay = consumptionDate;
                    }
                }
                consumptionsByDay.add(dailyConsumptions);

                // create list of DayTotals objects for each day
                List<InfoUtil.DayTotals> dayTotals = new ArrayList<>();
                for(List<Consumption> consumptionList : consumptionsByDay) {
                    // hashmap to accumulate totals for given day
                    HashMap<Goal.GoalCategory, Integer> totals = new HashMap<>();
                    for(Goal.GoalCategory category : Goal.GoalCategory.values()) {
                        totals.put(category, 0);
                    }

                    for(Consumption consumption : consumptionList) {
                        for(Goal.GoalCategory category : Goal.GoalCategory.values()) {
                            totals.put(category, totals.get(category) + consumption.calculateCategoryValue(category));
                        }
                    }
                    dayTotals.add(new InfoUtil.DayTotals(consumptionList.get(0).day, totals.get(Goal.GoalCategory.CARBS), totals.get(Goal.GoalCategory.FAT),
                            totals.get(Goal.GoalCategory.PROTEIN), totals.get(Goal.GoalCategory.FIBER), totals.get(Goal.GoalCategory.SUGAR),
                            totals.get(Goal.GoalCategory.SODIUM), totals.get(Goal.GoalCategory.CALORIES)));
                }

                // calculate averages
                InfoUtil.DayTotals averages = new InfoUtil.DayTotals(null, 0, 0, 0, 0, 0, 0, 0);
                for(InfoUtil.DayTotals dayTotal : dayTotals) {
                    averages.carbs += dayTotal.carbs;
                    averages.fat += dayTotal.fat;
                    averages.protein += dayTotal.protein;
                    averages.fiber += dayTotal.fiber;
                    averages.sugar += dayTotal.sugar;
                    averages.sodium += dayTotal.sodium;
                    averages.calories += dayTotal.calories;
                }
                // build overall totals object before dividing to find averages
                overallTotalsObj.add("carbs", averages.carbs)
                        .add("fat", averages.fat)
                        .add("protein", averages.protein)
                        .add("fiber", averages.fiber)
                        .add("sugar", averages.sugar)
                        .add("sodium", averages.sodium)
                        .add("calories", averages.calories);
                int daysInRange = dayTotals.size();
                averages.carbs /= daysInRange;
                averages.fat /= daysInRange;
                averages.protein /= daysInRange;
                averages.fiber /= daysInRange;
                averages.sugar /= daysInRange;
                averages.sodium /= daysInRange;
                averages.calories /= daysInRange;

                // create totals JSON array
                for(InfoUtil.DayTotals totals : dayTotals) {
                    String dateStr = new SimpleDateFormat("yyyy-MM-dd").format(totals.day);
                    String displayDateStr = new SimpleDateFormat("EEEE, MMMMM d").format(totals.day);
                    totalsArr.add(Json.createObjectBuilder()
                            .add("day", dateStr)
                            .add("displayDay", displayDateStr)
                            .add("carbs", totals.carbs)
                            .add("fat", totals.fat)
                            .add("protein", totals.protein)
                            .add("fiber", totals.fiber)
                            .add("sugar", totals.sugar)
                            .add("sodium", totals.sodium)
                            .add("calories", totals.calories));
                }

                // create averages JSON object
                averagesObj.add("calories", averages.calories)
                        .add("carbs", averages.carbs)
                        .add("fat", averages.fat)
                        .add("protein", averages.protein)
                        .add("fiber", averages.fiber)
                        .add("sugar", averages.sugar)
                        .add("sodium", averages.sodium);
            }

            // create exercise JSON array
            List<Exercise> userExercise = DatabaseUtil.getExercisesForDateRange(userId, fromDay, toDay);
            JsonArrayBuilder exerciseArr = Json.createArrayBuilder();
            for(Exercise exercise : userExercise) {
                exerciseArr.add(Json.createObjectBuilder()
                        .add("day", new SimpleDateFormat("yyyy-MM-dd").format(exercise.day))
                        .add("caloriesBurned", exercise.caloriesBurned));
            }

            JsonObjectBuilder res = Json.createObjectBuilder()
                    .add("totals", totalsArr)
                    .add("overallTotals", overallTotalsObj)
                    .add("averages", averagesObj)
                    .add("exercise", exerciseArr);
            renderJSON(res.build().toString());
        } catch(ParseException e) {
            e.printStackTrace();
            response.status = 404;
            renderText("");
        }
    }

    public static void testEndpoint() {
        response.setHeader("Access-Control-Allow-Origin", "*");
        List<Consumption> recentConsumptions = DatabaseUtil.getRecentConsumptions(1);
        Consumption mostRecent = recentConsumptions.get(0);
        JsonObjectBuilder res = Json.createObjectBuilder()
                .add("test", true)
                .add("working", true)
                .add("mostRecentConsumption", Json.createObjectBuilder()
                        .add("id", mostRecent.id)
                        .add("name", mostRecent.foodItem.name)
                        .add("quantity", mostRecent.servingQuantity));
        renderJSON(res.build().toString());
    }

}