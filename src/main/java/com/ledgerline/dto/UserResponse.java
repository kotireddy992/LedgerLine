package com.ledgerline.dto;

import com.ledgerline.enums.Role;
import java.time.Instant;

public class UserResponse {
    private Long id;
    private String name;
    private String email;
    private Role role;
    private Instant createdAt;

    public UserResponse() {}
    private UserResponse(Builder b) {
        this.id = b.id; this.name = b.name; this.email = b.email;
        this.role = b.role; this.createdAt = b.createdAt;
    }

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private Long id;
        private String name, email;
        private Role role;
        private Instant createdAt;
        public Builder id(Long id)             { this.id = id; return this; }
        public Builder name(String name)       { this.name = name; return this; }
        public Builder email(String email)     { this.email = email; return this; }
        public Builder role(Role role)         { this.role = role; return this; }
        public Builder createdAt(Instant t)    { this.createdAt = t; return this; }
        public UserResponse build()            { return new UserResponse(this); }
    }

    public Long getId()           { return id; }
    public String getName()       { return name; }
    public String getEmail()      { return email; }
    public Role getRole()         { return role; }
    public Instant getCreatedAt() { return createdAt; }
}
