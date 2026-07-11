package com.sirp.workflow.mapper;

import com.sirp.workflow.dto.request.CreateWorkflowRequest;
import com.sirp.workflow.dto.response.WorkflowResponse;
import com.sirp.workflow.entity.WorkflowEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface WorkflowMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "severity", source = "priority")
    @Mapping(target = "workflowStatus", constant = "CREATED")
    @Mapping(target = "escalationLevel", constant = "0")
    @Mapping(target = "resolvedAt", ignore = true)
    @Mapping(target = "closedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    WorkflowEntity toEntity(CreateWorkflowRequest request);

    @Mapping(target = "priority", source = "severity")
    WorkflowResponse toResponse(WorkflowEntity entity);

}