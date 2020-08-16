package util;

import models.FoodItem;

import models.ServingLabel;
import models.ServingSize;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SurveyFoodDetailsExtractor extends NonBrandedFoodDetailsExtractor implements FoodDetailsExtractor {
    @Override
    public FoodItem extract(JSONObject foodObj) {
        // Extract id and name.
        String foodItemId = String.valueOf(foodObj.getInt("fdcId"));
        String name = StringUtil.formatFoodItemName(foodObj.getString("description"));

        // Extract the serving sizes from provided food portions.
        List<ServingSize> servingSizes = new ArrayList<>();
        JSONArray portionsArr = foodObj.getJSONArray("foodPortions");
        for(int i = 0; i < portionsArr.length(); i++) {
            JSONObject portionObj = portionsArr.getJSONObject(i);
            String servingText = portionObj.getString("portionDescription");
            if("Quantity not specified".equals(servingText)) continue;
            int firstSpaceIndex = servingText.indexOf(' ');
            double quantity = Double.parseDouble(servingText.substring(0, firstSpaceIndex));
            String label = StringUtil.formatServingSizeLabel(servingText.substring(firstSpaceIndex + 1));
            double perUnitGramWeight = portionObj.getDouble("gramWeight") / quantity;
            // Nutrient information is provided per-100 grams, so ratio is compared to this standard.
            double ratio = perUnitGramWeight / 100.0;

            ServingSize servingSize = new ServingSize();
            ServingLabel servingLabel = new ServingLabel();
            servingLabel.labelValue = label;
            servingSize.label = servingLabel;
            servingSize.ratio = ratio;
            servingSizes.add(servingSize);
        }

        // Extract the nutrients.
        JSONArray nutrientsArr = foodObj.getJSONArray("foodNutrients");
        Map<String, Double> nutritionMap = buildNutritionMap(nutrientsArr);

        // Calculate calories from macros (database is often inaccurate).
        double calories = InfoUtil.calculateCalories(nutritionMap.get("carbs"), nutritionMap.get("fat"), nutritionMap.get("protein"));
        return new FoodItem(foodItemId, name, servingSizes, calories, nutritionMap.get("carbs"),
                nutritionMap.get("fat"), nutritionMap.get("protein"), nutritionMap.get("fiber"),
                nutritionMap.get("sugar"), nutritionMap.get("sodium"));
    }
}
