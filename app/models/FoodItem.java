package models;

import javax.persistence.*;
import java.util.List;

/**
 * Represents a food item, which can be consumed by a user
 */
@Entity
@Table(name = "food_item")
public class FoodItem {

    @Id
    private String id;

    private String name;

    private double calories;

    private double carbs;

    private double fat;

    private double protein;

    private double fiber;

    private double sugar;

    private double sodium;

    @OneToMany(mappedBy = "foodItem")
    private List<Consumption> consumptions;

    @OneToMany(mappedBy = "foodItem")
    private List<ServingSize> servingSizes;

    public FoodItem(String id, String name, double calories, double carbs, double fat, double protein,
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

    public FoodItem() {}
}