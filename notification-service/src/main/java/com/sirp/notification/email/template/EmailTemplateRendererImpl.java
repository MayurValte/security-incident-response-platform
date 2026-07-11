package com.sirp.notification.email.template;

import com.sirp.notification.email.model.IncidentEmailModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
public class EmailTemplateRendererImpl implements EmailTemplateRenderer {

    private final TemplateEngine templateEngine;

    @Override
    public String renderIncidentCreated(IncidentEmailModel model) {
        Context context = buildContext(model);
        return templateEngine.process("incident-created", context);
    }

    @Override
    public String renderIncidentAssigned(IncidentEmailModel model) {
        Context context = buildContext(model);
        return templateEngine.process("incident-assigned", context);
    }

    @Override
    public String renderIncidentResolved(IncidentEmailModel model) {
        Context context = buildContext(model);
        return templateEngine.process("incident-resolved", context);
    }

    @Override
    public String renderIncidentClosed(IncidentEmailModel model) {
        Context context = buildContext(model);
        return templateEngine.process("incident-closed", context);
    }

    private Context buildContext(IncidentEmailModel model) {
        Context context = new Context();
        context.setVariable("incidentNumber", model.incidentNumber());
        context.setVariable("title", model.title());
        context.setVariable("description", model.description());
        context.setVariable("priority", model.priority());
        context.setVariable("severity", model.severity());
        context.setVariable("status", model.status());
        context.setVariable("createdBy", model.createdBy());
        context.setVariable("createdAt", model.createdAt());
        context.setVariable("incidentUrl", model.incidentUrl());
        return context;
    }
}
