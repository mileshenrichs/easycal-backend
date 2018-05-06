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
    public String id;

    public String name;

    public double calories;

    public double carbs;

    public double fat;

    public double protein;

    public double fiber;

    public double sugar;

    public double sodium;

    @OneToMany(mappedBy = "foodItem")
    public List<Consumption> consumptions;

    @OneToMany(mappedBy = "foodItem")
    public List<ServingSize> servingSizes;

    public FoodItem(String id, String name, List<ServingSize> servingSizes, double calories, double carbs, double fat, double protein,
                    double fiber, double sugar, double sodium) {
        this.id = id;
        this.name = name;
        this.servingSizes = servingSizes;
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