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

    UserResponse create(CreateUserRequest request, UUID actorId);

    UserResponse get(UUID id);

    Page<UserResponse> getAll(Pageable pageable);

    UserResponse update(UUID id, UpdateUserRequest request, UUID actorId);

    void delete(UUID id, UUID actorId);

    UserSecurityResponse findByEmail(String email);

    UserSecurityResponse findSecurityUser(UUID id);

    UserNotificationResponse findNotificationUser(UUID id);
}
