package com.sirp.audit.audit.service.impl;

import com.sirp.audit.audit.dto.response.AuditPageResponse;
import com.sirp.audit.audit.dto.response.AuditResponse;
import com.sirp.audit.audit.entity.AggregateType;
import com.sirp.audit.audit.entity.AuditEvent;
import com.sirp.audit.audit.entity.AuditEventType;
import com.sirp.audit.audit.exception.AuditNotFoundException;
import com.sirp.audit.audit.mapper.AuditMapper;
import com.sirp.audit.audit.repository.AuditEventRepository;
import com.sirp.audit.audit.service.AuditService;
import com.sirp.audit.audit.specification.AuditSpecification;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditServiceImpl implements AuditService {

  private final AuditEventRepository repository;

  private final AuditMapper mapper;

  @Override
  public AuditResponse getById(UUID id) {

    AuditEvent event = repository.findById(id)

                                 .orElseThrow(

                                     () -> new AuditNotFoundException(id)

                                             );

    return mapper.toResponse(event);

  }

  @Override
  public AuditPageResponse getByAggregateId(

      UUID aggregateId,

      Integer page,

      Integer size

                                           ) {

    Pageable pageable = buildPageable(

        page,

        size

                                     );

    Page<AuditEvent> result = repository.findByAggregateId(

        aggregateId,

        pageable

                                                          );

    return mapper.toPageResponse(

        result

                                );

  }

  @Override
  public AuditPageResponse getByPerformedBy(

      UUID performedBy,

      Integer page,

      Integer size

                                           ) {

    Pageable pageable = buildPageable(

        page,

        size

                                     );

    Page<AuditEvent> result = repository.findByPerformedBy(

        performedBy,

        pageable

                                                          );

    return mapper.toPageResponse(

        result

                                );

  }

  @Override
  public AuditPageResponse getByEventType(

      AuditEventType eventType,

      Integer page,

      Integer size

                                         ) {

    Pageable pageable = buildPageable(

        page,

        size

                                     );

    Page<AuditEvent> result = repository.findByEventType(

        eventType,

        pageable

                                                        );

    return mapper.toPageResponse(

        result

                                );

  }

  @Override
  public AuditPageResponse search(

      UUID aggregateId,

      AggregateType aggregateType,

      AuditEventType eventType,

      UUID performedBy,

      Instant from,

      Instant to,

      Integer page,

      Integer size

                                 ) {

    Pageable pageable = buildPageable(

        page,

        size

                                     );

    Specification<AuditEvent> specification = Specification.allOf(

        AuditSpecification.aggregateId(

            aggregateId

                                      ),

        AuditSpecification.aggregateType(

            aggregateType

                                        ),

        AuditSpecification.eventType(

            eventType

                                    ),

        AuditSpecification.performedBy(

            performedBy

                                      ),

        AuditSpecification.occurredAfter(

            from

                                        ),

        AuditSpecification.occurredBefore(

            to

                                         )

                                                                 );

    Page<AuditEvent> result = repository.findAll(

        specification,

        pageable

                                                );

    return mapper.toPageResponse(

        result

                                );

  }

  private Pageable buildPageable(

      Integer page,

      Integer size

                                ) {

    return PageRequest.of(

        page,

        size,

        Sort.by(

            Sort.Direction.DESC,

            "occurredAt"

               )

                         );

  }

}