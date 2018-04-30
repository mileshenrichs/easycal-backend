package controllers;

import play.mvc.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

import models.*;
import util.DatabaseUtil;

import javax.json.*;

public class EasyCal extends Controller {

    public static void getConsumptions(int userId, String date) {
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
                // build list of serving sizes
                JsonArrayBuilder servingSizes = Json.createArrayBuilder();
                for(ServingSize servingSize : c.foodItem.servingSizes) {
                    servingSizes.add(Json.createObjectBuilder()
                            .add("id", servingSize.id)
                            .add("label", servingSize.label.labelValue)
                            .add("ratio", servingSize.ratio));
                }

                // construct meal item JSON
                JsonObjectBuilder item = Json.createObjectBuilder();
                item.add("consumptionId", c.id)
                    .add("foodItemId", c.foodItem.id)
                    .add("name", c.foodItem.name)
                    .add("selectedServing",
                            Json.createObjectBuilder()
                                .add("servingSize", Json.createObjectBuilder()
                                        .add("id", c.servingSize.id)
                                        .add("label", c.servingSize.label.labelValue)
                                        .add("ratio", c.servingSize.ratio))
                                .add("quantity", c.servingQuantity))
                    .add("servingSizes", servingSizes)
                    .add("calories", c.foodItem.calories)
                    .add("carbs", c.foodItem.carbs)
                    .add("fat", c.foodItem.fat)
                    .add("protein", c.foodItem.protein)
                    .add("fiber", c.foodItem.fiber)
                    .add("sugar", c.foodItem.sugar)
                    .add("sodium", c.foodItem.sodium);
                items.add(item);
            }
            res.add(meal.name().toLowerCase(), Json.createObjectBuilder()
                            .add("items", items));
        }
        renderJSON(res.build().toString());
    }

    public static void deleteConsumption(int consumptionId) {
        response.status = DatabaseUtil.deleteConsumption(consumptionId) ? 204 : 404;
        renderText("");
    }

}