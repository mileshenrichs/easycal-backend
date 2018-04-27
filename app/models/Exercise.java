package models;

import javax.persistence.*;

import java.util.Date;

/**
 * A simple object containing a calorie count and a day
 */
@Entity
@Table(name = "exercise")
public class Exercise {

    @Id
    @GeneratedValue
    private int id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "calories_burned")
    private int caloriesBurned;

    @Temporal(TemporalType.DATE)
    private Date day;

    public Exercise(int id, User user, int caloriesBurned, Date day) {
        this.id = id;
        this.user = user;
        this.caloriesBurned = caloriesBurned;
        this.day = day;
    }

    public Exercise() {}
}
