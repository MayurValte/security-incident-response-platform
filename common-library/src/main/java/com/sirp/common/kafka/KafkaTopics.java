package com.sirp.common.kafka;

public final class KafkaTopics {

    public static final String INCIDENT_CREATED = "incident.created.v1";
    public static final String INCIDENT_ASSIGNED = "incident.assigned.v1";
    public static final String INCIDENT_RESOLVED = "incident.resolved.v1";
    public static final String INCIDENT_CLOSED = "incident.closed.v1";

    private KafkaTopics() {
    }

}