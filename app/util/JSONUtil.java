package util;

import models.Consumption;
import models.FoodItem;
import models.ServingSize;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;

/**
 * A util class to build JSON objects and responses
 */
public class JSONUtil {
    public static JsonObjectBuilder buildMealItem(Consumption c) {
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
        return item;
    }

    /**
     * Build meal item JSON for food being added through /add
     * @param f the FoodItem being added
     * @return JSON object builder representing food item
     */
    public static JsonObjectBuilder buildMealItem(FoodItem f) {
        // build list of serving sizes
        JsonArrayBuilder servingSizes = Json.createArrayBuilder();
        for(ServingSize servingSize : f.servingSizes) {
            servingSizes.add(Json.createObjectBuilder()
                    .add("id", f.servingSizes.indexOf(servingSize)) // temp ids for ServingSelect component
                    .add("label", servingSize.label.labelValue)
                    .add("ratio", servingSize.ratio));
        }

        // construct meal item JSON
        int servingSizeId = f.servingSizes.size() > 0 ? f.servingSizes.get(0).id : 0;
        JsonObjectBuilder item = Json.createObjectBuilder();
        item.add("foodItemId", f.id)
                .add("name", f.name)
                .add("selectedServing",
                        Json.createObjectBuilder()
                                .add("servingSize", Json.createObjectBuilder()
                                        .add("id", servingSizeId) // temp id
                                        .add("label", f.servingSizes.get(0).label.labelValue)
                                        .add("ratio", f.servingSizes.get(0).ratio))
                                .add("quantity", 1))
                .add("servingSizes", servingSizes)
                .add("calories", f.calories)
                .add("carbs", f.carbs)
                .add("fat", f.fat)
                .add("protein", f.protein)
                .add("fiber", f.fiber)
                .add("sugar", f.sugar)
                .add("sodium", f.sodium);
        return item;
    }
}
