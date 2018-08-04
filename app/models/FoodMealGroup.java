package models;

import com.google.gson.annotations.Expose;

import javax.persistence.*;
import java.util.List;

/**
 * A goal is a certain amount of calories or nutrient a user wishes to consume daily
 */
@Entity
@Table(name = "food_meal_group")
public class FoodMealGroup {

    @Id
    @GeneratedValue
    @Expose
    public int id;

    @Expose
    public String name;

    @JoinColumn(name = "user_id")
    @ManyToOne
    public User user;

    @OneToMany(mappedBy = "foodMealGroup", fetch = FetchType.EAGER)
    @Expose
    public List<FoodMealGroupItem> mealGroupItems;

    public FoodMealGroup() {}
}
