package com.sirp.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sirp.common.exception.DuplicateResourceException;
import com.sirp.user.dto.CreateTeamRequest;
import com.sirp.user.dto.TeamResponse;
import com.sirp.user.dto.UpdateTeamRequest;
import com.sirp.user.exception.GlobalExceptionHandler;
import com.sirp.user.exception.ResourceNotFoundException;
import com.sirp.user.exception.TeamInUseException;
import com.sirp.user.service.TeamService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@ExtendWith(MockitoExtension.class)
class TeamControllerTest {

    @Mock
    private TeamService service;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        TeamController controller = new TeamController(service);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .setValidator(new LocalValidatorFactoryBean())
            .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
            .build();
    }

    @Test
    void create_returns201WithCreatedTeam() throws Exception {
        CreateTeamRequest request = new CreateTeamRequest("Bootstrap Team");
        UUID teamId = UUID.randomUUID();
        TeamResponse response = new TeamResponse(teamId, "Bootstrap Team");
        when(service.create(eq(request))).thenReturn(response);

        mockMvc.perform(post("/api/v1/teams")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(teamId.toString()))
            .andExpect(jsonPath("$.teamName").value("Bootstrap Team"));
    }

    @Test
    void create_returns400WhenTeamNameBlank() throws Exception {
        CreateTeamRequest request = new CreateTeamRequest(" ");

        mockMvc.perform(post("/api/v1/teams")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void create_returns409WhenTeamNameAlreadyExists() throws Exception {
        CreateTeamRequest request = new CreateTeamRequest("Bootstrap Team");
        when(service.create(eq(request)))
            .thenThrow(new DuplicateResourceException("Team already exists : Bootstrap Team"));

        mockMvc.perform(post("/api/v1/teams")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict());
    }

    @Test
    void get_returns200WithTeam() throws Exception {
        UUID teamId = UUID.randomUUID();
        TeamResponse response = new TeamResponse(teamId, "Bootstrap Team");
        when(service.get(teamId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/teams/{id}", teamId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.teamName").value("Bootstrap Team"));
    }

    @Test
    void get_returns404WhenTeamMissing() throws Exception {
        UUID teamId = UUID.randomUUID();
        when(service.get(teamId)).thenThrow(new ResourceNotFoundException("Team not found with id : " + teamId));

        mockMvc.perform(get("/api/v1/teams/{id}", teamId))
            .andExpect(status().isNotFound());
    }

    @Test
    void getAll_returns200WithPageContent() throws Exception {
        TeamResponse response = new TeamResponse(UUID.randomUUID(), "Bootstrap Team");
        when(service.getAll(any())).thenReturn(new PageImpl<>(List.of(response), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/teams"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].teamName").value("Bootstrap Team"));
    }

    @Test
    void update_returns200WithRenamedTeam() throws Exception {
        UUID teamId = UUID.randomUUID();
        UpdateTeamRequest request = new UpdateTeamRequest("Renamed Team");
        TeamResponse response = new TeamResponse(teamId, "Renamed Team");
        when(service.update(eq(teamId), eq(request))).thenReturn(response);

        mockMvc.perform(put("/api/v1/teams/{id}", teamId)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.teamName").value("Renamed Team"));
    }

    @Test
    void delete_returns204() throws Exception {
        UUID teamId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/teams/{id}", teamId))
            .andExpect(status().isNoContent());

        verify(service).delete(teamId);
    }

    @Test
    void delete_returns409WhenTeamStillInUse() throws Exception {
        UUID teamId = UUID.randomUUID();
        Mockito.doThrow(new TeamInUseException(teamId)).when(service).delete(teamId);

        mockMvc.perform(delete("/api/v1/teams/{id}", teamId))
            .andExpect(status().isConflict());
    }

    @Test
    void delete_returns404WhenTeamMissing() throws Exception {
        UUID teamId = UUID.randomUUID();
        Mockito.doThrow(new ResourceNotFoundException("Team not found with id : " + teamId))
            .when(service).delete(teamId);

        mockMvc.perform(delete("/api/v1/teams/{id}", teamId))
            .andExpect(status().isNotFound());
    }
}
