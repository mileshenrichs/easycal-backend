package models;

import org.joda.time.LocalDate;

/**
 * A simple object containing a calorie count and a day
 */
public class Exercise {
    private int id;
    private User user;
    private int caloriesBurned;
    private LocalDate day;

    public Exercise(int id, User user, int caloriesBurned, LocalDate day) {
        this.id = id;
        this.user = user;
        this.caloriesBurned = caloriesBurned;
        this.day = day;
    }
}
