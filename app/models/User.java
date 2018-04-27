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
    private int id;

    @Column(name = "email_address")
    private String emailAddress;

    private String password;

    @OneToMany(mappedBy = "user")
    private List<Consumption> consumptions;

    @OneToMany(mappedBy = "user")
    private List<Goal> goals;

    @OneToMany(mappedBy = "user")
    private List<Exercise> exercise;

    public User(int id, String emailAddress, String password) {
        this.id = id;
        this.emailAddress = emailAddress;
        this.password = password;
    }

    public User() {}
}
