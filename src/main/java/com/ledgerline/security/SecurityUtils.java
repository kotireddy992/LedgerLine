package com.ledgerline.security;

import com.ledgerline.entity.User;
import com.ledgerline.exception.UnauthorizedException;
import com.ledgerline.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Resolves the currently authenticated user from the SecurityContext.
 *
 * NEVER accept userId or role from the request body/params.
 * Always call this to determine who is making the request.
 */
@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private final UserRepository userRepository;

    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new UnauthorizedException("Not authenticated");
        }
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Authenticated user not found in database"));
    }

    public Long getCurrentUserId() {
        return getCurrentUser().getId();
    }
}
