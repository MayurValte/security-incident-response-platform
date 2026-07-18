package com.sirp.analytics.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sirp.analytics.entity.AnalyticsStatus;
import com.sirp.analytics.entity.IncidentAnalyticsRecord;
import com.sirp.analytics.repository.IncidentAnalyticsRepository;
import com.sirp.common.enums.IncidentPriority;
import com.sirp.common.enums.IncidentSeverity;
import com.sirp.common.events.IncidentAssignedEvent;
import com.sirp.common.events.IncidentClosedEvent;
import com.sirp.common.events.IncidentCreatedEvent;
import com.sirp.common.events.IncidentResolvedEvent;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnalyticsEventConsumerTest {

    @Mock
    private IncidentAnalyticsRepository repository;

    @InjectMocks
    private AnalyticsEventConsumer consumer;

    private UUID incidentId;

    @Nested
    class ConsumeCreated {

        @Test
        void savesNewRecordWithCreatedStatus() {
            incidentId = UUID.randomUUID();
            Instant occurredAt = Instant.now();
            IncidentCreatedEvent event = new IncidentCreatedEvent(UUID.randomUUID(), incidentId, "INC-2026-1",
                "Prod outage", "desc", IncidentPriority.P1, IncidentSeverity.HIGH, UUID.randomUUID(), occurredAt);
            when(repository.existsByIncidentId(incidentId)).thenReturn(false);

            consumer.consumeCreated(event);

            ArgumentCaptor<IncidentAnalyticsRecord> captor = ArgumentCaptor.forClass(IncidentAnalyticsRecord.class);
            verify(repository).save(captor.capture());
            IncidentAnalyticsRecord saved = captor.getValue();
            assertThat(saved.getIncidentId()).isEqualTo(incidentId);
            assertThat(saved.getIncidentNumber()).isEqualTo("INC-2026-1");
            assertThat(saved.getStatus()).isEqualTo(AnalyticsStatus.CREATED);
            assertThat(saved.getPriority()).isEqualTo(IncidentPriority.P1);
            assertThat(saved.getSeverity()).isEqualTo(IncidentSeverity.HIGH);
            assertThat(saved.getCreatedAt()).isEqualTo(occurredAt);
        }

        @Test
        void skipsSaveWhenIncidentAlreadyHasARecord() {
            incidentId = UUID.randomUUID();
            IncidentCreatedEvent event = new IncidentCreatedEvent(UUID.randomUUID(), incidentId, "INC-2026-1",
                "t", "d", IncidentPriority.P1, IncidentSeverity.HIGH, UUID.randomUUID(), Instant.now());
            when(repository.existsByIncidentId(incidentId)).thenReturn(true);

            consumer.consumeCreated(event);

            verify(repository, never()).save(any());
        }
    }

    @Nested
    class ConsumeAssigned {

        @Test
        void updatesExistingRecordToAssigned() {
            incidentId = UUID.randomUUID();
            Instant occurredAt = Instant.now();
            IncidentAnalyticsRecord record = IncidentAnalyticsRecord.builder().incidentId(incidentId)
                .status(AnalyticsStatus.CREATED).createdAt(Instant.now().minusSeconds(60)).build();
            IncidentAssignedEvent event = new IncidentAssignedEvent(UUID.randomUUID(), incidentId, "INC-2026-1",
                "t", IncidentPriority.P1, IncidentSeverity.HIGH, UUID.randomUUID(), UUID.randomUUID(), occurredAt);
            when(repository.findByIncidentId(incidentId)).thenReturn(Optional.of(record));

            consumer.consumeAssigned(event);

            assertThat(record.getStatus()).isEqualTo(AnalyticsStatus.ASSIGNED);
            assertThat(record.getAssignedAt()).isEqualTo(occurredAt);
            verify(repository).save(record);
        }

        @Test
        void doesNothingWhenNoRecordExistsYetDueToEventOrdering() {
            incidentId = UUID.randomUUID();
            IncidentAssignedEvent event = new IncidentAssignedEvent(UUID.randomUUID(), incidentId, "INC-2026-1",
                "t", IncidentPriority.P1, IncidentSeverity.HIGH, UUID.randomUUID(), UUID.randomUUID(),
                Instant.now());
            when(repository.findByIncidentId(incidentId)).thenReturn(Optional.empty());

            consumer.consumeAssigned(event);

            verify(repository, never()).save(any());
        }
    }

    @Nested
    class ConsumeResolved {

        @Test
        void updatesStatusAndComputesResolutionMinutes() {
            incidentId = UUID.randomUUID();
            Instant createdAt = Instant.parse("2026-07-18T10:00:00Z");
            Instant resolvedAt = Instant.parse("2026-07-18T10:45:00Z");
            IncidentAnalyticsRecord record = IncidentAnalyticsRecord.builder().incidentId(incidentId)
                .status(AnalyticsStatus.ASSIGNED).createdAt(createdAt).build();
            IncidentResolvedEvent event = new IncidentResolvedEvent(UUID.randomUUID(), incidentId, "INC-2026-1",
                "t", IncidentPriority.P1, IncidentSeverity.HIGH, UUID.randomUUID(), resolvedAt);
            when(repository.findByIncidentId(incidentId)).thenReturn(Optional.of(record));

            consumer.consumeResolved(event);

            assertThat(record.getStatus()).isEqualTo(AnalyticsStatus.RESOLVED);
            assertThat(record.getResolvedAt()).isEqualTo(resolvedAt);
            assertThat(record.getResolutionMinutes()).isEqualTo(45L);
            verify(repository).save(record);
        }

        @Test
        void doesNothingWhenNoRecordExists() {
            incidentId = UUID.randomUUID();
            IncidentResolvedEvent event = new IncidentResolvedEvent(UUID.randomUUID(), incidentId, "INC-2026-1",
                "t", IncidentPriority.P1, IncidentSeverity.HIGH, UUID.randomUUID(), Instant.now());
            when(repository.findByIncidentId(incidentId)).thenReturn(Optional.empty());

            consumer.consumeResolved(event);

            verify(repository, never()).save(any());
        }
    }

    @Nested
    class ConsumeClosed {

        @Test
        void updatesExistingRecordToClosed() {
            incidentId = UUID.randomUUID();
            Instant occurredAt = Instant.now();
            IncidentAnalyticsRecord record = IncidentAnalyticsRecord.builder().incidentId(incidentId)
                .status(AnalyticsStatus.RESOLVED).createdAt(Instant.now().minusSeconds(3600)).build();
            IncidentClosedEvent event = new IncidentClosedEvent(UUID.randomUUID(), incidentId, "INC-2026-1", "t",
                IncidentPriority.P1, IncidentSeverity.HIGH, UUID.randomUUID(), occurredAt);
            when(repository.findByIncidentId(incidentId)).thenReturn(Optional.of(record));

            consumer.consumeClosed(event);

            assertThat(record.getStatus()).isEqualTo(AnalyticsStatus.CLOSED);
            assertThat(record.getClosedAt()).isEqualTo(occurredAt);
            verify(repository).save(record);
        }

        @Test
        void doesNothingWhenNoRecordExists() {
            incidentId = UUID.randomUUID();
            IncidentClosedEvent event = new IncidentClosedEvent(UUID.randomUUID(), incidentId, "INC-2026-1", "t",
                IncidentPriority.P1, IncidentSeverity.HIGH, UUID.randomUUID(), Instant.now());
            when(repository.findByIncidentId(incidentId)).thenReturn(Optional.empty());

            consumer.consumeClosed(event);

            verify(repository, never()).save(any());
        }
    }
}
