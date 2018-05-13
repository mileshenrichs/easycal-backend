package models;

import javax.persistence.*;

/**
 * A goal is a certain amount of calories or nutrient a user wishes to consume daily
 */
@Entity
@Table(name = "goals")
public class Goal {

    @Id
    @GeneratedValue
    public int id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    public User user;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "goal_category")
    public GoalCategory goalCategory;

    @Column(name = "goal_value")
    public int goalValue;

    public Goal() {}

    public enum GoalCategory {
        CALORIES,
        CARBS,
        FAT,
        PROTEIN,
        FIBER,
        SUGAR,
        SODIUM
    }
}
