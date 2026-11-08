package com.ledgerline.entity;

import com.ledgerline.enums.Role;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_users_email", columnList = "email")
})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public User() {}

    private User(Builder b) {
        this.id = b.id;
        this.name = b.name;
        this.email = b.email;
        this.passwordHash = b.passwordHash;
        this.role = b.role;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private String name, email, passwordHash;
        private Role role;

        public Builder id(Long id)                     { this.id = id; return this; }
        public Builder name(String name)               { this.name = name; return this; }
        public Builder email(String email)             { this.email = email; return this; }
        public Builder passwordHash(String ph)         { this.passwordHash = ph; return this; }
        public Builder role(Role role)                 { this.role = role; return this; }
        public User build()                            { return new User(this); }
    }

    public Long getId()             { return id; }
    public String getName()         { return name; }
    public String getEmail()        { return email; }
    public String getPasswordHash() { return passwordHash; }
    public Role getRole()           { return role; }
    public Instant getCreatedAt()   { return createdAt; }

    public void setName(String name)               { this.name = name; }
    public void setEmail(String email)             { this.email = email; }
    public void setPasswordHash(String ph)         { this.passwordHash = ph; }
    public void setRole(Role role)                 { this.role = role; }
}
