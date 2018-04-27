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
    private int id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "goal_category")
    private GoalCategory goalCategory;

    @Column(name = "goal_value")
    private int goalValue;

    public Goal(int id, User user, GoalCategory goalCategory, int goalValue) {
        this.id = id;
        this.user = user;
        this.goalCategory = goalCategory;
        this.goalValue = goalValue;
    }

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
