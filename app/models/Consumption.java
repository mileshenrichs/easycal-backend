package models;

import org.joda.time.LocalDate;

/**
 * Represents a Food Item consumed by a user (part of daily log)
 */
public class Consumption {
    private int id;
    private User user;
    private FoodItem foodItem;
    private ServingSize servingSize;
    private double servingQuantity;
    private Meal meal;
    private LocalDate day;

    public Consumption(int id, User user, FoodItem foodItem, ServingSize servingSize,
                       double servingQuantity, Meal meal, LocalDate day) {
        this.id = id;
        this.user = user;
        this.foodItem = foodItem;
        this.servingSize = servingSize;
        this.servingQuantity = servingQuantity;
        this.meal = meal;
        this.day = day;
    }
}
