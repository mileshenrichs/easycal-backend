package util;

import models.FoodItem;

/**
 * Object to be returned from DatabaseUtil.createNewFoodItem()
 */
public class CreatedFoodItemWithSelectedServing {
    public FoodItem foodItem;
    public int selectedServingSizeId;

    public CreatedFoodItemWithSelectedServing(FoodItem foodItem, int selectedServingSizeId) {
        this.foodItem = foodItem;
        this.selectedServingSizeId = selectedServingSizeId;
    }
}
