package com.shomuran.cardscope.model;

import jakarta.persistence.*;
import java.util.List;

@Entity
public class UserProfile extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    private String name;

    private String passwordHash; // hashed password

    // ✅ NEW FIELDS for OAuth-based signups
    private String provider;    // "local", "google", etc.
    private String providerId;  // Google account ID if applicable

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_cards", joinColumns = @JoinColumn(name = "user_id"))
    private List<UserCard> userCards; // holds issuer + product pair

    // ---------- Getters & Setters ----------

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public List<UserCard> getUserCards() {
        return userCards;
    }

    public void setUserCards(List<UserCard> userCards) {
        this.userCards = userCards;
    }
}
