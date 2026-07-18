package com.sirp.user.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sirp.common.events.UserCreatedEvent;
import com.sirp.common.events.UserDeletedEvent;
import com.sirp.common.events.UserUpdatedEvent;
import com.sirp.user.dto.CreateUserRequest;
import com.sirp.user.dto.UpdateUserRequest;
import com.sirp.user.dto.UserNotificationResponse;
import com.sirp.user.dto.UserResponse;
import com.sirp.user.dto.UserSecurityResponse;
import com.sirp.user.entity.Team;
import com.sirp.user.entity.User;
import com.sirp.user.enums.Role;
import com.sirp.user.exception.ResourceNotFoundException;
import com.sirp.user.exception.UserNotFoundException;
import com.sirp.user.kafka.producer.UserEventProducer;
import com.sirp.user.mapper.UserMapper;
import com.sirp.user.repository.TeamRepository;
import com.sirp.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository repository;

    @Mock
    private UserMapper mapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private UserEventProducer userEventProducer;

    @InjectMocks
    private UserServiceImpl userService;

    private UUID userId;
    private UUID teamId;
    private UUID actorId;
    private Team team;
    private User user;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        teamId = UUID.randomUUID();
        actorId = UUID.randomUUID();
        team = Team.builder().id(teamId).teamName("Bootstrap Team").build();
        user = User.builder()
            .id(userId)
            .username("jdoe")
            .email("jdoe@sirp.local")
            .password("encoded-password")
            .enabled(true)
            .role(Role.ENGINEER)
            .team(team)
            .build();
    }

    @Nested
    class Create {

        @Test
        void savesEncodedPasswordAndPublishesCreatedEvent() {
            CreateUserRequest request = new CreateUserRequest("jdoe", "jdoe@sirp.local", "Passw0rd!",
                Role.ENGINEER, teamId);
            UserResponse expected = new UserResponse(userId, "jdoe", "jdoe@sirp.local", Role.ENGINEER,
                "Bootstrap Team");

            when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
            when(passwordEncoder.encode("Passw0rd!")).thenReturn("encoded-password");
            when(repository.save(any(User.class))).thenReturn(user);
            when(mapper.toDto(user)).thenReturn(expected);

            UserResponse result = userService.create(request, actorId);

            assertThat(result).isEqualTo(expected);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(repository).save(userCaptor.capture());
            User savedArg = userCaptor.getValue();
            assertThat(savedArg.getUsername()).isEqualTo("jdoe");
            assertThat(savedArg.getEmail()).isEqualTo("jdoe@sirp.local");
            assertThat(savedArg.getPassword()).isEqualTo("encoded-password");
            assertThat(savedArg.getEnabled()).isTrue();
            assertThat(savedArg.getTeam()).isEqualTo(team);

            ArgumentCaptor<UserCreatedEvent> eventCaptor = ArgumentCaptor.forClass(UserCreatedEvent.class);
            verify(userEventProducer).publishCreated(eventCaptor.capture());
            UserCreatedEvent publishedEvent = eventCaptor.getValue();
            assertThat(publishedEvent.userId()).isEqualTo(userId);
            assertThat(publishedEvent.username()).isEqualTo("jdoe");
            assertThat(publishedEvent.email()).isEqualTo("jdoe@sirp.local");
            assertThat(publishedEvent.role()).isEqualTo("ENGINEER");
            assertThat(publishedEvent.createdBy()).isEqualTo(actorId);
        }

        @Test
        void throwsWhenTeamDoesNotExist() {
            CreateUserRequest request = new CreateUserRequest("jdoe", "jdoe@sirp.local", "Passw0rd!",
                Role.ENGINEER, teamId);
            when(teamRepository.findById(teamId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.create(request, actorId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(teamId.toString());

            verify(repository, never()).save(any());
            verify(userEventProducer, never()).publishCreated(any());
        }
    }

    @Nested
    class Get {

        @Test
        void returnsMappedUserWhenFound() {
            UserResponse expected = new UserResponse(userId, "jdoe", "jdoe@sirp.local", Role.ENGINEER,
                "Bootstrap Team");
            when(repository.findById(userId)).thenReturn(Optional.of(user));
            when(mapper.toDto(user)).thenReturn(expected);

            UserResponse result = userService.get(userId);

            assertThat(result).isEqualTo(expected);
        }

        @Test
        void throwsResourceNotFoundWhenMissing() {
            when(repository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.get(userId))
                .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Test
    void getAllMapsEveryPageEntry() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<User> userPage = new PageImpl<>(List.of(user), pageable, 1);
        UserResponse mapped = new UserResponse(userId, "jdoe", "jdoe@sirp.local", Role.ENGINEER, "Bootstrap Team");
        when(repository.findAll(pageable)).thenReturn(userPage);
        when(mapper.toDto(user)).thenReturn(mapped);

        Page<UserResponse> result = userService.getAll(pageable);

        assertThat(result.getContent()).containsExactly(mapped);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Nested
    class Update {

        @Test
        void overwritesUsernameEmailAndRoleThenPublishesUpdatedEvent() {
            UpdateUserRequest request = new UpdateUserRequest("jdoe2", "jdoe2@sirp.local", Role.MANAGER);
            UserResponse expected = new UserResponse(userId, "jdoe2", "jdoe2@sirp.local", Role.MANAGER,
                "Bootstrap Team");

            when(repository.findById(userId)).thenReturn(Optional.of(user));
            when(repository.save(user)).thenReturn(user);
            when(mapper.toDto(user)).thenReturn(expected);

            UserResponse result = userService.update(userId, request, actorId);

            assertThat(result).isEqualTo(expected);
            assertThat(user.getUsername()).isEqualTo("jdoe2");
            assertThat(user.getEmail()).isEqualTo("jdoe2@sirp.local");
            assertThat(user.getRole()).isEqualTo(Role.MANAGER);

            ArgumentCaptor<UserUpdatedEvent> eventCaptor = ArgumentCaptor.forClass(UserUpdatedEvent.class);
            verify(userEventProducer).publishUpdated(eventCaptor.capture());
            assertThat(eventCaptor.getValue().updatedBy()).isEqualTo(actorId);
            assertThat(eventCaptor.getValue().role()).isEqualTo("MANAGER");
        }

        @Test
        void throwsWhenUserMissing() {
            UpdateUserRequest request = new UpdateUserRequest("jdoe2", "jdoe2@sirp.local", Role.MANAGER);
            when(repository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.update(userId, request, actorId))
                .isInstanceOf(ResourceNotFoundException.class);

            verify(repository, never()).save(any());
            verify(userEventProducer, never()).publishUpdated(any());
        }
    }

    @Nested
    class Delete {

        @Test
        void deletesUserAndPublishesDeletedEvent() {
            when(repository.findById(userId)).thenReturn(Optional.of(user));

            userService.delete(userId, actorId);

            verify(repository, times(1)).delete(user);
            ArgumentCaptor<UserDeletedEvent> eventCaptor = ArgumentCaptor.forClass(UserDeletedEvent.class);
            verify(userEventProducer).publishDeleted(eventCaptor.capture());
            assertThat(eventCaptor.getValue().userId()).isEqualTo(userId);
            assertThat(eventCaptor.getValue().deletedBy()).isEqualTo(actorId);
        }

        @Test
        void throwsWhenUserMissingAndNeverPublishes() {
            when(repository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.delete(userId, actorId))
                .isInstanceOf(ResourceNotFoundException.class);

            verify(repository, never()).delete(any());
            verify(userEventProducer, never()).publishDeleted(any());
        }
    }

    @Nested
    class FindByEmail {

        @Test
        void returnsSecurityDtoWhenFound() {
            UserSecurityResponse expected = new UserSecurityResponse(userId, "jdoe", "jdoe@sirp.local",
                "encoded-password", "ENGINEER", true);
            when(repository.findByEmail("jdoe@sirp.local")).thenReturn(Optional.of(user));
            when(mapper.toSecurityDto(user)).thenReturn(expected);

            UserSecurityResponse result = userService.findByEmail("jdoe@sirp.local");

            assertThat(result).isEqualTo(expected);
        }

        @Test
        void throwsUserNotFoundWhenEmailUnknown() {
            when(repository.findByEmail("missing@sirp.local")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.findByEmail("missing@sirp.local"))
                .isInstanceOf(UserNotFoundException.class);
        }
    }

    @Nested
    class FindSecurityUser {

        @Test
        void returnsSecurityDtoWhenFound() {
            UserSecurityResponse expected = new UserSecurityResponse(userId, "jdoe", "jdoe@sirp.local",
                "encoded-password", "ENGINEER", true);
            when(repository.findById(userId)).thenReturn(Optional.of(user));
            when(mapper.toSecurityDto(user)).thenReturn(expected);

            assertThat(userService.findSecurityUser(userId)).isEqualTo(expected);
        }

        @Test
        void throwsUserNotFoundWhenMissing() {
            when(repository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.findSecurityUser(userId))
                .isInstanceOf(UserNotFoundException.class);
        }
    }

    @Nested
    class FindNotificationUser {

        @Test
        void returnsNotificationDtoWhenFound() {
            UserNotificationResponse expected = new UserNotificationResponse(userId, "jdoe", null, null,
                "jdoe@sirp.local", null, true);
            when(repository.findById(userId)).thenReturn(Optional.of(user));
            when(mapper.toNotificationDto(user)).thenReturn(expected);

            assertThat(userService.findNotificationUser(userId)).isEqualTo(expected);
        }

        @Test
        void throwsUserNotFoundWhenMissing() {
            when(repository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.findNotificationUser(userId))
                .isInstanceOf(UserNotFoundException.class);
        }
    }
}
