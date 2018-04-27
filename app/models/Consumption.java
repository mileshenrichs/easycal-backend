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
    private int id;

    @JoinColumn(name = "user_id")
    @ManyToOne
    private User user;

    @JoinColumn(name = "food_item_id")
    @ManyToOne
    private FoodItem foodItem;

    @JoinColumn(name = "serving_size_id")
    @ManyToOne
    private ServingSize servingSize;

    @Column(name = "serving_quantity")
    private double servingQuantity;

    @Enumerated(EnumType.ORDINAL)
    private Meal meal;

    @Temporal(TemporalType.DATE)
    private Date day;

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
