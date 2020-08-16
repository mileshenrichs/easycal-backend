package util;

import models.ServingLabel;
import models.ServingSize;
import models.FoodItem;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class BrandedFoodDetailsExtractor implements FoodDetailsExtractor {
    private static final String HOUSEHOLD_SERVING_TEXT_KEY = "householdServingFullText";

    @Override
    public FoodItem extract(JSONObject foodObj) {
        // Extract id and name.
        String foodItemId = String.valueOf(foodObj.getInt("fdcId"));
        String name = StringUtil.formatFoodItemName(foodObj.getString("description"));

        // Extract the serving sizes (base serving size and household serving size).
        List<ServingSize> servingSizes = new ArrayList<>();
        // The base serving size is provided with a common measuring unit such as grams or milliliters. All nutrient
        // amounts are normalized to 1 of this measuring unit (i.e. how much protein is in 1 gram of applesauce).
        ServingSize baseServingSize = new ServingSize();
        double baseServingSizeQuantity = foodObj.getDouble("servingSize");
        ServingLabel label = new ServingLabel();
        label.labelValue = foodObj.getString("servingSizeUnit");
        baseServingSize.label = label;
        baseServingSize.ratio = 1.0;
        servingSizes.add(baseServingSize);
        // The household serving size is what you might find on the nutrition label (i.e. ".2 pizza").
        if(householdServingSizeIsAvailable(foodObj)) {
            ServingSize householdServingSize =
                    parseHouseholdServingText(foodObj.getString(HOUSEHOLD_SERVING_TEXT_KEY), baseServingSizeQuantity);
            servingSizes.add(0, householdServingSize);
        }

        // Extract the nutrients.
        JSONArray nutrientsArr = foodObj.getJSONArray("foodNutrients");
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
            String nutrientName = nutrientObj.getJSONObject("nutrient").getString("name");
            switch(nutrientName) {
                case "Carbohydrate, by difference":
                    nutritionMap.put("carbs", nutrientObj.getDouble("amount") / baseServingSizeQuantity);
                    break;
                case "Total lipid (fat)":
                    nutritionMap.put("fat", nutrientObj.getDouble("amount") / baseServingSizeQuantity);
                    break;
                case "Protein":
                    nutritionMap.put("protein", nutrientObj.getDouble("amount") / baseServingSizeQuantity);
                    break;
                case "Fiber, total dietary":
                    nutritionMap.put("fiber", nutrientObj.getDouble("amount") / baseServingSizeQuantity);
                    break;
                case "Sugars, total":
                case "Sugars, total including NLEA":
                    nutritionMap.put("sugar", nutrientObj.getDouble("amount") / baseServingSizeQuantity);
                    break;
                case "Sodium, Na":
                    nutritionMap.put("sodium", nutrientObj.getDouble("amount") / baseServingSizeQuantity);
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

        // Calculate calories from macros (database is often inaccurate).
        double calories = InfoUtil.calculateCalories(nutritionMap.get("carbs"), nutritionMap.get("fat"), nutritionMap.get("protein"));
        return new FoodItem(foodItemId, name, servingSizes, calories, nutritionMap.get("carbs"),
                nutritionMap.get("fat"), nutritionMap.get("protein"), nutritionMap.get("fiber"),
                nutritionMap.get("sugar"), nutritionMap.get("sodium"));
    }

    private static boolean householdServingSizeIsAvailable(JSONObject foodObj) {
        return foodObj.has(HOUSEHOLD_SERVING_TEXT_KEY) && !foodObj.isNull(HOUSEHOLD_SERVING_TEXT_KEY)
                && !foodObj.getString(HOUSEHOLD_SERVING_TEXT_KEY).trim().isEmpty();
    }

    /**
     * Parses the serving size string found on the back of a box ("i.e. .2 pizza") into a ServingSize object.
     * @param servingText The serving size string. The quantity and label will need to be separated.
     * @param baseQuantity The quantity of the base serving size (i.e. if base serving size is 100 grams, this will be
     *                     100), which is used to calculate the ratio of the household serving size.
     * @return The household serving size.
     */
    private static ServingSize parseHouseholdServingText(String servingText, double baseQuantity) {
        int firstSpaceIndex = servingText.indexOf(' ');
        double quantity = Double.parseDouble(servingText.substring(0, firstSpaceIndex));
        String label = StringUtil.formatServingSizeLabel(servingText.substring(firstSpaceIndex + 1));
        // Calculate this serving size's ratio as {base serving size quantity} * (1 / {household quantity}). Its ratio
        // indicates how many of the base units are in one of the household units. As an example, say we have a pizza
        // whose base serving size is 126 grams. The household label indicates that this is equivalent to .2 of a pizza.
        // So to figure out how many grams are in an entire pizza, we multiply by 5 (which is 1 / .2).
        double ratio = baseQuantity * (1.0 / quantity);
        ServingSize householdServingSize = new ServingSize();
        ServingLabel servingLabel = new ServingLabel();
        servingLabel.labelValue = label;
        householdServingSize.label = servingLabel;
        householdServingSize.ratio = ratio;
        return householdServingSize;
    }
}
