package com.example.Entity;



import javax.persistence.*;

@Entity
@Table(name = "Benutzer")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="USERID")
    private long userId;

    @Column(name = "NAME")
    private String name;


    public User() {
    }

    public User(long userId, String name) {
        this.userId = userId;
        this.name = name;
    }

    //Setter and Getter
    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
