package com.sirp.workflow.kafka.producer;

import com.sirp.common.events.workflow.WorkflowAssignedEvent;
import com.sirp.common.events.workflow.WorkflowClosedEvent;
import com.sirp.common.events.workflow.WorkflowCreatedEvent;
import com.sirp.common.events.workflow.WorkflowEscalatedEvent;
import com.sirp.common.events.workflow.WorkflowResolvedEvent;
import com.sirp.common.kafka.KafkaTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class WorkflowEventProducerImpl implements WorkflowEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void publishWorkflowCreated(WorkflowCreatedEvent event) {
        kafkaTemplate.send(KafkaTopics.WORKFLOW_CREATED, event.workflowId().toString(), event);
        log.info("Published WorkflowCreatedEvent [{}]", event.eventId());
    }

    @Override
    public void publishWorkflowAssigned(WorkflowAssignedEvent event) {
        kafkaTemplate.send(KafkaTopics.WORKFLOW_ASSIGNED, event.workflowId().toString(), event);
        log.info("Published WorkflowAssignedEvent [{}]", event.eventId());
    }

    @Override
    public void publishWorkflowEscalated(WorkflowEscalatedEvent event) {
        kafkaTemplate.send(KafkaTopics.WORKFLOW_ESCALATED, event.workflowId().toString(), event);
        log.info("Published WorkflowEscalatedEvent [{}]", event.eventId());
    }

    @Override
    public void publishWorkflowResolved(WorkflowResolvedEvent event) {
        kafkaTemplate.send(KafkaTopics.WORKFLOW_RESOLVED, event.workflowId().toString(), event);
        log.info("Published WorkflowResolvedEvent [{}]", event.eventId());
    }

    @Override
    public void publishWorkflowClosed(WorkflowClosedEvent event) {
        kafkaTemplate.send(KafkaTopics.WORKFLOW_CLOSED, event.workflowId().toString(), event);
        log.info("Published WorkflowClosedEvent [{}]", event.eventId());
    }
}
