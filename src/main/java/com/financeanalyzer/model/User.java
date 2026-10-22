package com.financeanalyzer.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entity representing an authenticated user account.
 * Implements Serializable for secure backup snapshots.
 */
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String username;
    private String passwordHash;
    private boolean recommendationsEnabled;
    private LocalDateTime createdAt;

    public User() {
        this.recommendationsEnabled = true;
        this.createdAt = LocalDateTime.now();
    }

    public User(Long id, String username, String passwordHash, boolean recommendationsEnabled, LocalDateTime createdAt) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.recommendationsEnabled = recommendationsEnabled;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    public User(String username, String passwordHash) {
        this(null, username, passwordHash, true, LocalDateTime.now());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public boolean isRecommendationsEnabled() {
        return recommendationsEnabled;
    }

    public void setRecommendationsEnabled(boolean recommendationsEnabled) {
        this.recommendationsEnabled = recommendationsEnabled;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User user)) return false;
        return Objects.equals(id, user.id) && Objects.equals(username, user.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, username);
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", recommendationsEnabled=" + recommendationsEnabled +
                '}';
    }
}
