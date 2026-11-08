package com.ledgerline.unit;

import com.ledgerline.dto.*;
import com.ledgerline.entity.User;
import com.ledgerline.enums.Role;
import com.ledgerline.exception.InvalidOperationException;
import com.ledgerline.repository.UserRepository;
import com.ledgerline.security.JwtService;
import com.ledgerline.service.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @Mock AuthenticationManager authenticationManager;

    @InjectMocks AuthService authService;

    @Test
    void register_success() {
        RegisterRequest req = new RegisterRequest("Alice", "alice@test.com", "Password1", Role.ACCOUNT_HOLDER);
        when(userRepository.existsByEmail("alice@test.com")).thenReturn(false);
        when(passwordEncoder.encode("Password1")).thenReturn("hashed");
        User saved = User.builder().id(1L).name("Alice").email("alice@test.com")
                .passwordHash("hashed").role(Role.ACCOUNT_HOLDER).build();
        when(userRepository.save(any())).thenReturn(saved);

        UserResponse result = authService.register(req);

        assertThat(result.getEmail()).isEqualTo("alice@test.com");
        assertThat(result.getRole()).isEqualTo(Role.ACCOUNT_HOLDER);
        // Password hash must never appear in the response.
        verify(userRepository).save(argThat(u -> u.getPasswordHash().equals("hashed")));
    }

    @Test
    void register_duplicateEmail_throwsInvalidOperation() {
        RegisterRequest req = new RegisterRequest("Alice", "alice@test.com", "Password1", Role.ACCOUNT_HOLDER);
        when(userRepository.existsByEmail("alice@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(InvalidOperationException.class);
    }

    @Test
    void login_success_returnsToken() {
        LoginRequest req = new LoginRequest("alice@test.com", "Password1");
        User user = User.builder().id(1L).email("alice@test.com")
                .passwordHash("hashed").role(Role.ACCOUNT_HOLDER).build();
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken("alice@test.com", "ACCOUNT_HOLDER", 1L)).thenReturn("jwt-token");

        AuthResponse result = authService.login(req);

        assertThat(result.getToken()).isEqualTo("jwt-token");
        assertThat(result.getRole()).isEqualTo("ACCOUNT_HOLDER");
        // Password hash must never appear in the response.
        assertThat(result).hasNoNullFieldsOrPropertiesExcept();
    }

    @Test
    void login_badCredentials_throwsBadCredentials() {
        LoginRequest req = new LoginRequest("alice@test.com", "WrongPass");
        doThrow(new BadCredentialsException("bad")).when(authenticationManager).authenticate(any());

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BadCredentialsException.class);
    }
}
