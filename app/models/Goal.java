package models;

/**
 * A goal is a certain amount of calories or nutrient a user wishes to consume daily
 */
public class Goal {
    private int id;
    private User user;
    private GoalCategory goalCategory;
    private int goalValue;

    public Goal(int id, User user, GoalCategory goalCategory, int goalValue) {
        this.id = id;
        this.user = user;
        this.goalCategory = goalCategory;
        this.goalValue = goalValue;
    }

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
