package com.ledgerline.dto;

import com.ledgerline.enums.Role;
import jakarta.validation.constraints.*;

public class RegisterRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be 2–100 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @NotNull(message = "Role is required")
    private Role role;

    public RegisterRequest() {}
    public RegisterRequest(String name, String email, String password, Role role) {
        this.name = name; this.email = email; this.password = password; this.role = role;
    }

    public String getName()     { return name; }
    public String getEmail()    { return email; }
    public String getPassword() { return password; }
    public Role getRole()       { return role; }
    public void setName(String name)         { this.name = name; }
    public void setEmail(String email)       { this.email = email; }
    public void setPassword(String password) { this.password = password; }
    public void setRole(Role role)           { this.role = role; }
}
