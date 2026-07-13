package com.sirp.auth.security;

public interface LoginAttemptService {

    void checkNotLocked(String email);

    void recordFailure(String email);

    void recordSuccess(String email);
}
