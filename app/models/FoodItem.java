package models;

/**
 * Represents a food item, which can be consumed by a user
 */
public class FoodItem {
    private int id;
    private String name;
    private double calories;
    private double carbs;
    private double fat;
    private double protein;
    private double fiber;
    private double sugar;
    private double sodium;

    public FoodItem(int id, String name, double calories, double carbs, double fat, double protein,
                    double fiber, double sugar, double sodium) {
        this.id = id;
        this.name = name;
        this.calories = calories;
        this.carbs = carbs;
        this.fat = fat;
        this.protein = protein;
        this.fiber = fiber;
        this.sugar = sugar;
        this.sodium = sodium;
    }
}