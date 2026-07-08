package com.sirp.audit.audit.dto.response;

import java.util.List;

public record AuditPageResponse(

    List<AuditSummaryResponse> content,

    Integer page,

    Integer size,

    Long totalElements,

    Integer totalPages,

    Boolean first,

    Boolean last

) {

}