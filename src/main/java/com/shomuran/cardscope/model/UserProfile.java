package com.shomuran.cardscope.model;

import jakarta.persistence.*;
import java.util.List;

@Entity
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    private String name;

    private String passwordHash; // hashed password

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_cards", joinColumns = @JoinColumn(name = "user_id"))
    private List<UserCard> userCards; // 👈 Now holds objects, not strings

    // getters & setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public List<UserCard> getUserCards() { return userCards; }
    public void setUserCards(List<UserCard> userCards) { this.userCards = userCards; }
}
