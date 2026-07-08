package com.sirp.user.service;

import com.sirp.user.dto.CreateUserRequest;
import com.sirp.user.dto.UpdateUserRequest;
import com.sirp.user.dto.UserNotificationResponse;
import com.sirp.user.dto.UserResponse;
import com.sirp.user.dto.UserSecurityResponse;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {

    UserResponse create(CreateUserRequest request);

    UserResponse get(UUID id);

    Page<UserResponse> getAll(Pageable pageable);

    UserResponse update(UUID id, UpdateUserRequest request);

    void delete(UUID id);

    UserSecurityResponse findByEmail(String email);

    UserSecurityResponse findSecurityUser(UUID id);

    UserNotificationResponse findNotificationUser(UUID id);
}
