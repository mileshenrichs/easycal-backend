package models;

/**
 * Represents an EasyCal user
 */
public class User {
    private int id;
    private String emailAddress;
    private String password;

    public User(int id, String emailAddress, String password) {
        this.id = id;
        this.emailAddress = emailAddress;
        this.password = password;
    }
}
