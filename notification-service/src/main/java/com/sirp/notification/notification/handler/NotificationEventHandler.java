package com.sirp.notification.notification.handler;

import com.sirp.common.events.IncidentAssignedEvent;
import com.sirp.common.events.IncidentClosedEvent;
import com.sirp.common.events.IncidentCreatedEvent;
import com.sirp.common.events.IncidentResolvedEvent;

public interface NotificationEventHandler {

    void handleIncidentCreated(IncidentCreatedEvent event);

    void handleIncidentAssigned(IncidentAssignedEvent event);

    void handleIncidentResolved(IncidentResolvedEvent event);

    void handleIncidentClosed(IncidentClosedEvent event);

}