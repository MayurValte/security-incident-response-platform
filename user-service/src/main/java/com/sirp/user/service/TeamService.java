package com.sirp.user.service;

import com.sirp.user.dto.CreateTeamRequest;
import com.sirp.user.dto.TeamResponse;
import com.sirp.user.dto.UpdateTeamRequest;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TeamService {

    TeamResponse create(CreateTeamRequest request);

    TeamResponse get(UUID id);

    Page<TeamResponse> getAll(Pageable pageable);

    TeamResponse update(UUID id, UpdateTeamRequest request);

    void delete(UUID id);
}