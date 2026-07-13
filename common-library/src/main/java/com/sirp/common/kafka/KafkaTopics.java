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

    public static final String AUTH_LOGIN_SUCCEEDED = "auth.login.succeeded.v1";
    public static final String AUTH_LOGIN_FAILED = "auth.login.failed.v1";

    public static final String USER_CREATED = "user.created.v1";
    public static final String USER_UPDATED = "user.updated.v1";
    public static final String USER_DELETED = "user.deleted.v1";

    private KafkaTopics() {
    }

}