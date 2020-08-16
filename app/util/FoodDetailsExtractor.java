package util;

import org.json.JSONObject;

import models.FoodItem;

public interface FoodDetailsExtractor {
    FoodItem extract(JSONObject foodDetailsObj);
}
