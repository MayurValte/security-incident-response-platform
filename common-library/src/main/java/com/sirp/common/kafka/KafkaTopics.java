package com.sirp.common.kafka;

public final class KafkaTopics {

    public static final String INCIDENT_CREATED = "incident.created.v1";
    public static final String INCIDENT_ASSIGNED = "incident.assigned.v1";
    public static final String INCIDENT_RESOLVED = "incident.resolved.v1";
    public static final String INCIDENT_CLOSED = "incident.closed.v1";

    public static final String WORKFLOW_CREATED = "workflow.created.v1";
    public static final String WORKFLOW_ASSIGNED = "workflow.assigned.v1";
    public static final String WORKFLOW_ESCALATED = "workflow.escalated.v1";
    public static final String WORKFLOW_RESOLVED = "workflow.resolved.v1";
    public static final String WORKFLOW_CLOSED = "workflow.closed.v1";

    private KafkaTopics() {
    }

}