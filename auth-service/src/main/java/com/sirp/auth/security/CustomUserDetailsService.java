package com.sirp.auth.security;

import com.sirp.auth.dto.response.UserSecurityResponse;
import com.sirp.auth.feign.ResilientUserClient;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Used ONLY during login, by AuthenticationManager/DaoAuthenticationProvider,
 * to fetch a user's stored credentials for password comparison. Not involved
 * in validating already-issued tokens - that's entirely the shared library's
 * job now.
 *
 * Any Feign failure while looking up the user (404 because the email
 * doesn't exist, or a 5xx from user-service) is converted to
 * UsernameNotFoundException so it flows through GlobalExceptionHandler's
 * AuthenticationException handler as a 401 - previously an unmatched email
 * on login surfaced the raw FeignException and fell through to the generic
 * 500 handler instead.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final ResilientUserClient userClient;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        try {
            UserSecurityResponse user = userClient.findByEmail(email);
            return new UserPrincipal(user);
        } catch (FeignException ex) {
            // Only a genuine 404 means "no such user" - a real 5xx/timeout from
            // user-service isn't a bad-credentials case and shouldn't be reported
            // as one. Circuit-open failures surface as UserServiceUnavailableException
            // instead (thrown by ResilientUserClient's fallback), which isn't a
            // FeignException at all and so isn't caught here - it propagates to
            // GlobalExceptionHandler as a 503.
            if (ex.status() == 404) {
                throw new UsernameNotFoundException("User not found: " + email, ex);
            }
            throw ex;
        }
    }
}
