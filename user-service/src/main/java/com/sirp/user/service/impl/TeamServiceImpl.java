package com.sirp.user.service.impl;

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
import com.sirp.user.service.TeamService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class TeamServiceImpl implements TeamService {

    private final TeamRepository repository;
    private final TeamMapper mapper;
    private final UserRepository userRepository;

    @Override
    public TeamResponse create(CreateTeamRequest request) {
        if (repository.existsByTeamName(request.teamName())) {
            throw new DuplicateResourceException("Team already exists : " + request.teamName());
        }
        Team team = Team.builder().teamName(request.teamName()).build(), saved = repository.save(team);
        return mapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public TeamResponse get(UUID id) {
        Team team = repository.findById(id).orElseThrow(
            () -> new ResourceNotFoundException("Team not found with id : " + id));
        return mapper.toDto(team);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TeamResponse> getAll(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toDto);
    }

    @Override
    public TeamResponse update(UUID id, UpdateTeamRequest request) {
        Team team = repository.findById(id).orElseThrow(
            () -> new ResourceNotFoundException("Team not found with id : " + id));
        team.setTeamName(request.teamName());
        Team updated = repository.save(team);
        return mapper.toDto(updated);
    }

    @Override
    public void delete(UUID id) {
        Team team = repository.findById(id).orElseThrow(
            () -> new ResourceNotFoundException("Team not found with id : " + id));
        if (userRepository.existsByTeamId(id)) {
            throw new TeamInUseException(id);
        }
        repository.delete(team);
    }
}