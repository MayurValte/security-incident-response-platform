package com.sirp.user.mapper;

import com.sirp.user.dto.UserResponse;
import com.sirp.user.dto.UserSecurityResponse;
import com.sirp.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(
            target = "team",
            source = "team.teamName"
    )
    UserResponse toDto(User user);

    @Mapping(
            target = "role",
            expression = "java(user.getRole().name())"
    )
    UserSecurityResponse toSecurityDto(User user);

}