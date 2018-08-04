package models;

import com.google.gson.annotations.Expose;

import javax.persistence.*;
import java.util.List;

/**
 * Represents a food item, which can be consumed by a user
 */
@Entity
@Table(name = "food_item")
public class FoodItem {

    @Id
    @Expose
    public String id;

    @Expose
    public String name;

    @Expose
    public double calories;

    @Expose
    public double carbs;

    @Expose
    public double fat;

    @Expose
    public double protein;

    @Expose
    public double fiber;

    @Expose
    public double sugar;

    @Expose
    public double sodium;

    @ManyToOne
    @JoinTable(name="created_food",
            joinColumns = @JoinColumn(name = "food_item_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"))
    @Expose
    public User creator;

    @OneToMany(mappedBy = "foodItem")
    public List<Consumption> consumptions;

    @OneToMany(mappedBy = "foodItem")
    @Expose
    public List<ServingSize> servingSizes;

    @OneToMany(mappedBy = "foodItem")
    public List<FoodMealGroupItem> mealGroupItems;

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