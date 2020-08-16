package util;

import org.json.JSONObject;

public class FoodDetailsExtractorFactory {
    public FoodDetailsExtractor getExtractorForFoodDetails(JSONObject foodObj) {
        switch(foodObj.getString("dataType")) {
            case "Branded":
                return new BrandedFoodDetailsExtractor();
            case "Survey (FNDDS)":
                return new SurveyFoodDetailsExtractor();
            default:
                return new SRLegacyFoodDetailsExtractor();
        }
    }
}
