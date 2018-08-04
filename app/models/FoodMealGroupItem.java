package models;

import com.google.gson.annotations.Expose;

import javax.persistence.*;

/**
 * A goal is a certain amount of calories or nutrient a user wishes to consume daily
 */
@Entity
@Table(name = "food_meal_group_items")
public class FoodMealGroupItem {

    @Id
    @GeneratedValue
    @Expose
    public int id;

    @ManyToOne
    @JoinColumn(name = "food_meal_group_id")
    public FoodMealGroup foodMealGroup;

    @ManyToOne
    @JoinColumn(name = "food_item_id")
    @Expose
    public FoodItem foodItem;

    @ManyToOne
    @JoinColumn(name = "default_serving_size_id")
    @Expose
    public ServingSize defaultServingSize;

    @Column(name = "default_serving_quantity")
    @Expose
    public Double defaultServingQuantity;

    public FoodMealGroupItem() {}
}
