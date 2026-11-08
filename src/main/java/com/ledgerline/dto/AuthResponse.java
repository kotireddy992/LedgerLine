package com.ledgerline.dto;

public class AuthResponse {
    private String token;
    private String email;
    private String role;
    private Long userId;

    public AuthResponse() {}
    private AuthResponse(Builder b) {
        this.token = b.token; this.email = b.email; this.role = b.role; this.userId = b.userId;
    }

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private String token, email, role;
        private Long userId;
        public Builder token(String t)   { this.token = t; return this; }
        public Builder email(String e)   { this.email = e; return this; }
        public Builder role(String r)    { this.role = r; return this; }
        public Builder userId(Long id)   { this.userId = id; return this; }
        public AuthResponse build()      { return new AuthResponse(this); }
    }

    public String getToken()  { return token; }
    public String getEmail()  { return email; }
    public String getRole()   { return role; }
    public Long getUserId()   { return userId; }
}
