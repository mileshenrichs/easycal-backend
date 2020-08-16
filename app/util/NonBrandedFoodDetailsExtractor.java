package util;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;

/** Shared nutrient facts extraction logic for non-branded food types ("SR Legacy" and "Survey"). */
public abstract class NonBrandedFoodDetailsExtractor {
    HashMap<String, Double> buildNutritionMap(JSONArray nutrientsArr) {
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
                    nutritionMap.put("carbs", nutrientObj.getDouble("amount"));
                    break;
                case "Total lipid (fat)":
                    nutritionMap.put("fat", nutrientObj.getDouble("amount"));
                    break;
                case "Protein":
                    nutritionMap.put("protein", nutrientObj.getDouble("amount"));
                    break;
                case "Fiber, total dietary":
                    nutritionMap.put("fiber", nutrientObj.getDouble("amount"));
                    break;
                case "Sugars, total":
                case "Sugars, total including NLEA":
                    nutritionMap.put("sugar", nutrientObj.getDouble("amount"));
                    break;
                case "Sodium, Na":
                    nutritionMap.put("sodium", nutrientObj.getDouble("amount"));
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
        return nutritionMap;
    }
}
