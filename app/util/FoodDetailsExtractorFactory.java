package util;

import org.json.JSONObject;

public class FoodDetailsExtractorFactory {
    public FoodDetailsExtractor getExtractorForFoodDetails(JSONObject foodObj) {
        switch(foodObj.getString("dataType")) {
            case "Branded":
                return new BrandedFoodDetailsExtractor();
        }
        return null;
    }
}
