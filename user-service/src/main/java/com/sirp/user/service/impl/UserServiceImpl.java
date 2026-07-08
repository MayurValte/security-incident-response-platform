package com.sirp.user.service.impl;

import com.sirp.user.dto.CreateUserRequest;
import com.sirp.user.dto.UpdateUserRequest;
import com.sirp.user.dto.UserNotificationResponse;
import com.sirp.user.dto.UserResponse;
import com.sirp.user.dto.UserSecurityResponse;
import com.sirp.user.entity.Team;
import com.sirp.user.entity.User;
import com.sirp.user.exception.ResourceNotFoundException;
import com.sirp.user.exception.UserNotFoundException;
import com.sirp.user.mapper.UserMapper;
import com.sirp.user.repository.TeamRepository;
import com.sirp.user.repository.UserRepository;
import com.sirp.user.service.UserService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository repository;
    private final UserMapper mapper;
    private final PasswordEncoder passwordEncoder;
    private final TeamRepository teamRepository;

    @Override
    public UserResponse create(CreateUserRequest request) {
        Team team = teamRepository.findById(request.teamId()).orElseThrow(
            () -> new ResourceNotFoundException("Team not found with id : " + request.teamId()));
        User user = User.builder()
                        .username(request.username())
                        .email(request.email())
                        .password(passwordEncoder.encode(request.password()))
                        .enabled(true)
                        .role(request.role())
                        .team(team)
                        .build(), saved = repository.save(user);
        return mapper.toDto(saved);
    }

    @Override
    public UserResponse get(UUID id) {
        User user = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return mapper.toDto(user);
    }

    @Override
    public Page<UserResponse> getAll(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toDto);
    }

    @Override
    public UserResponse update(UUID id, UpdateUserRequest request) {
        User user = repository.findById(id).orElseThrow(
            () -> new ResourceNotFoundException("User not found with id : " + id));
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setRole(request.role());
        User updated = repository.save(user);
        return mapper.toDto(updated);
    }

    @Override
    public void delete(UUID id) {
        User user = repository.findById(id).orElseThrow(
            () -> new ResourceNotFoundException("User not found with id : " + id));
        repository.delete(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserSecurityResponse findByEmail(String email) {
        User user = repository.findByEmail(email).orElseThrow(() -> new UserNotFoundException("User not found"));
        return mapper.toSecurityDto(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserSecurityResponse findSecurityUser(UUID id) {
        User user = repository.findById(id).orElseThrow(
            () -> new UserNotFoundException("User not found with id : " + id));
        return mapper.toSecurityDto(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserNotificationResponse findNotificationUser(UUID id) {
        User user = repository.findById(id).orElseThrow(
            () -> new UserNotFoundException("User not found with id : " + id));
        return mapper.toNotificationDto(user);
    }
}
