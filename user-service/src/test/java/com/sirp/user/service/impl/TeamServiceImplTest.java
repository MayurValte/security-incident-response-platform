package com.sirp.user.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sirp.common.exception.DuplicateResourceException;
import com.sirp.user.dto.CreateTeamRequest;
import com.sirp.user.dto.TeamResponse;
import com.sirp.user.dto.UpdateTeamRequest;
import com.sirp.user.entity.Team;
import com.sirp.user.exception.ResourceNotFoundException;
import com.sirp.user.exception.TeamInUseException;
import com.sirp.user.mapper.TeamMapper;
import com.sirp.user.repository.TeamRepository;
import com.sirp.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class TeamServiceImplTest {

    @Mock
    private TeamRepository repository;

    @Mock
    private TeamMapper mapper;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TeamServiceImpl teamService;

    private UUID teamId;
    private Team team;

    @BeforeEach
    void setUp() {
        teamId = UUID.randomUUID();
        team = Team.builder().id(teamId).teamName("Bootstrap Team").build();
    }

    @Nested
    class Create {

        @Test
        void savesTeamWhenNameNotTaken() {
            CreateTeamRequest request = new CreateTeamRequest("Bootstrap Team");
            TeamResponse expected = new TeamResponse(teamId, "Bootstrap Team");

            when(repository.existsByTeamName("Bootstrap Team")).thenReturn(false);
            when(repository.save(any(Team.class))).thenReturn(team);
            when(mapper.toDto(team)).thenReturn(expected);

            TeamResponse result = teamService.create(request);

            assertThat(result).isEqualTo(expected);
            verify(repository).save(any(Team.class));
        }

        @Test
        void throwsDuplicateResourceWhenNameAlreadyExists() {
            CreateTeamRequest request = new CreateTeamRequest("Bootstrap Team");
            when(repository.existsByTeamName("Bootstrap Team")).thenReturn(true);

            assertThatThrownBy(() -> teamService.create(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Bootstrap Team");

            verify(repository, never()).save(any());
        }
    }

    @Nested
    class Get {

        @Test
        void returnsMappedTeamWhenFound() {
            TeamResponse expected = new TeamResponse(teamId, "Bootstrap Team");
            when(repository.findById(teamId)).thenReturn(Optional.of(team));
            when(mapper.toDto(team)).thenReturn(expected);

            assertThat(teamService.get(teamId)).isEqualTo(expected);
        }

        @Test
        void throwsResourceNotFoundWhenMissing() {
            when(repository.findById(teamId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> teamService.get(teamId))
                .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Test
    void getAllMapsEveryPageEntry() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Team> teamPage = new PageImpl<>(List.of(team), pageable, 1);
        TeamResponse mapped = new TeamResponse(teamId, "Bootstrap Team");
        when(repository.findAll(pageable)).thenReturn(teamPage);
        when(mapper.toDto(team)).thenReturn(mapped);

        Page<TeamResponse> result = teamService.getAll(pageable);

        assertThat(result.getContent()).containsExactly(mapped);
    }

    @Nested
    class Update {

        @Test
        void overwritesTeamNameAndReturnsMappedResult() {
            UpdateTeamRequest request = new UpdateTeamRequest("Renamed Team");
            TeamResponse expected = new TeamResponse(teamId, "Renamed Team");

            when(repository.findById(teamId)).thenReturn(Optional.of(team));
            when(repository.save(team)).thenReturn(team);
            when(mapper.toDto(team)).thenReturn(expected);

            TeamResponse result = teamService.update(teamId, request);

            assertThat(result).isEqualTo(expected);
            assertThat(team.getTeamName()).isEqualTo("Renamed Team");
        }

        @Test
        void throwsWhenTeamMissing() {
            UpdateTeamRequest request = new UpdateTeamRequest("Renamed Team");
            when(repository.findById(teamId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> teamService.update(teamId, request))
                .isInstanceOf(ResourceNotFoundException.class);

            verify(repository, never()).save(any());
        }
    }

    @Nested
    class Delete {

        @Test
        void deletesTeamWhenNoUsersAssigned() {
            when(repository.findById(teamId)).thenReturn(Optional.of(team));
            when(userRepository.existsByTeamId(teamId)).thenReturn(false);

            teamService.delete(teamId);

            verify(repository, times(1)).delete(team);
        }

        @Test
        void throwsTeamInUseWhenUsersStillAssigned() {
            when(repository.findById(teamId)).thenReturn(Optional.of(team));
            when(userRepository.existsByTeamId(teamId)).thenReturn(true);

            assertThatThrownBy(() -> teamService.delete(teamId))
                .isInstanceOf(TeamInUseException.class);

            verify(repository, never()).delete(any());
        }

        @Test
        void throwsResourceNotFoundWhenTeamMissing() {
            when(repository.findById(teamId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> teamService.delete(teamId))
                .isInstanceOf(ResourceNotFoundException.class);

            verify(userRepository, never()).existsByTeamId(any());
            verify(repository, never()).delete(any());
        }
    }
}
