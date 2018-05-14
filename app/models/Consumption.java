package models;

import javax.persistence.*;

import java.lang.reflect.Field;
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

    public Consumption() {}

    /**
     * Calculate amount of given nutrient (category) consumed in this consumption
     * @param category specified nutrient
     * @return amount of nutrient consumed
     */
    public int calculateCategoryValue(Goal.GoalCategory category) {
        try {
            Field field = FoodItem.class.getField(category.toString().toLowerCase());
            double fieldValue = (double) field.get(this.foodItem);
            return (int) Math.round(this.servingQuantity * this.servingSize.ratio * fieldValue);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return 0;
    }
}
