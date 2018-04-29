package models;

import javax.persistence.*;

import java.util.Date;

/**
 * Represents a Food Item consumed by a user (part of daily log)
 */
@Entity
@Table(name = "consumption")
public class Consumption {

    @Id
    @GeneratedValue
    public int id;

    @JoinColumn(name = "user_id")
    @ManyToOne
    public User user;

    @JoinColumn(name = "food_item_id")
    @ManyToOne
    public FoodItem foodItem;

    @JoinColumn(name = "serving_size_id")
    @ManyToOne
    public ServingSize servingSize;

    @Column(name = "serving_quantity")
    public double servingQuantity;

    @Enumerated(EnumType.ORDINAL)
    public Meal meal;

    @Temporal(TemporalType.DATE)
    public Date day;

    public Consumption(int id, User user, FoodItem foodItem, ServingSize servingSize,
                       double servingQuantity, Meal meal, Date day) {
        this.id = id;
        this.user = user;
        this.foodItem = foodItem;
        this.servingSize = servingSize;
        this.servingQuantity = servingQuantity;
        this.meal = meal;
        this.day = day;
    }

    public Consumption() {}
}
