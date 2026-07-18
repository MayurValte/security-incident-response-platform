package com.sirp.analytics.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sirp.analytics.dto.response.AnalyticsSummaryResponse;
import com.sirp.analytics.dto.response.DailyTrendResponse;
import com.sirp.analytics.dto.response.PriorityBreakdownResponse;
import com.sirp.analytics.dto.response.SeverityBreakdownResponse;
import com.sirp.analytics.service.AnalyticsService;
import com.sirp.common.enums.IncidentPriority;
import com.sirp.common.enums.IncidentSeverity;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AnalyticsControllerTest {

    @Mock
    private AnalyticsService analyticsService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AnalyticsController controller = new AnalyticsController(analyticsService);
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule())
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
            .build();
    }

    @Test
    void getSummary_returns200() throws Exception {
        when(analyticsService.getSummary(any(), any())).thenReturn(
            new AnalyticsSummaryResponse(10, 8, 6, 4, 33.5));

        mockMvc.perform(get("/api/v1/analytics/summary"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalCreated").value(10))
            .andExpect(jsonPath("$.avgResolutionMinutes").value(33.5));
    }

    @Test
    void getSummary_passesFromAndToQueryParamsThrough() throws Exception {
        when(analyticsService.getSummary(any(), any())).thenReturn(
            new AnalyticsSummaryResponse(1, 1, 1, 1, 1.0));

        mockMvc.perform(get("/api/v1/analytics/summary")
                .param("from", "2026-06-01T00:00:00Z")
                .param("to", "2026-07-01T00:00:00Z"))
            .andExpect(status().isOk());
    }

    @Test
    void getBySeverity_returns200WithArray() throws Exception {
        when(analyticsService.getBySeverity(any(), any())).thenReturn(
            List.of(new SeverityBreakdownResponse(IncidentSeverity.HIGH, 5, 20.0)));

        mockMvc.perform(get("/api/v1/analytics/by-severity"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].severity").value("HIGH"))
            .andExpect(jsonPath("$[0].incidentCount").value(5));
    }

    @Test
    void getByPriority_returns200WithArray() throws Exception {
        when(analyticsService.getByPriority(any(), any())).thenReturn(
            List.of(new PriorityBreakdownResponse(IncidentPriority.P1, 3, 12.0)));

        mockMvc.perform(get("/api/v1/analytics/by-priority"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].priority").value("P1"));
    }

    @Test
    void getDailyTrend_returns200WithDateOnlyField() throws Exception {
        when(analyticsService.getDailyTrend(any(), any())).thenReturn(
            List.of(new DailyTrendResponse(LocalDate.of(2026, 6, 15), 3, 2, 1)));

        mockMvc.perform(get("/api/v1/analytics/trend"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].date").value("2026-06-15"))
            .andExpect(jsonPath("$[0].createdCount").value(3));
    }
}
