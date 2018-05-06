package models;

import javax.persistence.*;
import java.util.List;

/**
 * Represents an EasyCal user
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue
    public int id;

    @Column(name = "email_address")
    public String emailAddress;

    public String password;

    @OneToMany(mappedBy = "user")
    public List<Consumption> consumptions;

    @OneToMany(mappedBy = "user")
    public List<Goal> goals;

    @OneToMany(mappedBy = "user")
    public List<Exercise> exercise;

    @OneToMany
    @JoinTable(name = "created_food",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "food_item_id"))
    public List<FoodItem> createdFoods;

    public User(int id, String emailAddress, String password) {
        this.id = id;
        this.emailAddress = emailAddress;
        this.password = password;
    }

    public User() {}
}
