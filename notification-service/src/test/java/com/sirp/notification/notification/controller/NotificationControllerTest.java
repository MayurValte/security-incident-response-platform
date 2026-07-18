package com.sirp.notification.notification.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sirp.notification.exception.GlobalExceptionHandler;
import com.sirp.notification.exception.NotificationNotFoundException;
import com.sirp.notification.notification.dto.response.NotificationPageResponse;
import com.sirp.notification.notification.dto.response.NotificationResponse;
import com.sirp.notification.notification.enums.NotificationChannel;
import com.sirp.notification.notification.enums.NotificationStatus;
import com.sirp.notification.notification.enums.NotificationType;
import com.sirp.notification.notification.service.NotificationService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        NotificationController controller = new NotificationController(notificationService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    private NotificationResponse sampleResponse(UUID id) {
        return new NotificationResponse(id, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
            "jdoe@sirp.local", NotificationChannel.EMAIL, NotificationType.INCIDENT_CREATED,
            NotificationStatus.SENT, "subj", "msg", null, Instant.now(), Instant.now());
    }

    @Test
    void getNotification_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(notificationService.getNotification(id)).thenReturn(sampleResponse(id));

        mockMvc.perform(get("/api/v1/notifications/{id}", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void getNotification_returns404WhenMissing() throws Exception {
        UUID id = UUID.randomUUID();
        when(notificationService.getNotification(id)).thenThrow(new NotificationNotFoundException(id));

        mockMvc.perform(get("/api/v1/notifications/{id}", id))
            .andExpect(status().isNotFound());
    }

    @Test
    void search_defaultsPageAndSizeWhenOmitted() throws Exception {
        when(notificationService.searchNotifications(eq(0), eq(10), any()))
            .thenReturn(new NotificationPageResponse(List.of(sampleResponse(UUID.randomUUID())), 0, 10, 1, 1,
                true, true));

        mockMvc.perform(get("/api/v1/notifications"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].channel").value("EMAIL"));
    }

    @Test
    void search_honorsExplicitPageAndSize() throws Exception {
        when(notificationService.searchNotifications(eq(1), eq(5), any()))
            .thenReturn(new NotificationPageResponse(List.of(), 1, 5, 0, 0, false, true));

        mockMvc.perform(get("/api/v1/notifications").param("page", "1").param("size", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(1))
            .andExpect(jsonPath("$.size").value(5));
    }

    @Test
    void getNotificationsByIncident_returns200WithPlainArray() throws Exception {
        UUID incidentId = UUID.randomUUID();
        when(notificationService.getNotificationsByIncident(incidentId))
            .thenReturn(List.of(sampleResponse(UUID.randomUUID())));

        mockMvc.perform(get("/api/v1/notifications/incident/{incidentId}", incidentId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].channel").value("EMAIL"));
    }

    @Test
    void getNotificationsByRecipient_returns200WithPlainArray() throws Exception {
        UUID recipientId = UUID.randomUUID();
        when(notificationService.getNotificationsByRecipient(recipientId))
            .thenReturn(List.of(sampleResponse(UUID.randomUUID())));

        mockMvc.perform(get("/api/v1/notifications/recipient/{recipientId}", recipientId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].channel").value("EMAIL"));
    }
}
