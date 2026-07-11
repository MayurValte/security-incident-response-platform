package com.sirp.user.mapper;

import com.sirp.user.dto.TeamResponse;
import com.sirp.user.entity.Team;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TeamMapper {

    TeamResponse toDto(Team team);

}