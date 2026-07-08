package com.sirp.user.service;

import com.sirp.user.dto.CreateUserRequest;
import com.sirp.user.dto.UpdateUserRequest;
import com.sirp.user.dto.UserResponse;
import com.sirp.user.dto.UserSecurityResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {

    UserResponse create(CreateUserRequest request);

    UserResponse get(Long id);

    Page<UserResponse> getAll(Pageable pageable);

    UserResponse update(Long id,
                        UpdateUserRequest request);

    void delete(Long id);

    UserSecurityResponse findByEmail(String email);

    UserSecurityResponse findSecurityUser(
            Long id
    );

}
