package util;

import models.FoodItem;
import org.json.JSONArray;
import org.json.JSONObject;

public class SRLegacyFoodDetailsExtractor extends NonBrandedFoodDetailsExtractor implements FoodDetailsExtractor {
    @Override
    public FoodItem extract(JSONObject foodDetailsObj) {
        return null;
    }
}
