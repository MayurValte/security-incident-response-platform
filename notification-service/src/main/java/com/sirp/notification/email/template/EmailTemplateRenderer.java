package com.sirp.notification.email.template;

import com.sirp.notification.email.model.IncidentEmailModel;

public interface EmailTemplateRenderer {

    String renderIncidentCreated(IncidentEmailModel model);

    String renderIncidentAssigned(IncidentEmailModel model);

    String renderIncidentResolved(IncidentEmailModel model);

    String renderIncidentClosed(IncidentEmailModel model);
}