package ru.bre.storage.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.bre.storage.config.RateLimitFilter;
import ru.bre.storage.config.TelemetryAuthFilter;
import ru.bre.storage.exception.BatchRejectedException;
import ru.bre.storage.exception.InvalidEventsException;
import ru.bre.storage.service.SteamAuthService;
import ru.bre.storage.service.TelemetryService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TelemetryControllerTest {

    @Mock
    private TelemetryService telemetryService;

    private SteamAuthService steamAuthService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        steamAuthService = new SteamAuthService(true);
        mockMvc = MockMvcBuilders.standaloneSetup(new TelemetryController(telemetryService))
                .addFilters(new RateLimitFilter(), new TelemetryAuthFilter(steamAuthService))
                .build();
    }

    @Test
    void testAcceptValidBatch() throws Exception {
        mockMvc.perform(post("/telemetry/batches")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"schema_version":1,"events":[{"event_id":"bc24a059-cc9d-42a8-ba76-601d87279736"}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true));

        verify(telemetryService).acceptBatch(any());
    }

    @Test
    void testRejectBatchWithoutToken() throws Exception {
        mockMvc.perform(post("/telemetry/batches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"schema_version":1,"events":[{}]}
                                """))
                .andExpect(status().isUnauthorized());

        verify(telemetryService, never()).acceptBatch(any());
    }

    @Test
    void testRejectBatchWithUnknownToken() throws Exception {
        mockMvc.perform(post("/telemetry/batches")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer 0123456789abcdef")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"schema_version":1,"events":[{}]}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testRejectUnsupportedSchemaVersion() throws Exception {
        doThrow(new BatchRejectedException("Unsupported schema_version"))
                .when(telemetryService).acceptBatch(any());

        mockMvc.perform(post("/telemetry/batches")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"schema_version":2,"events":[{}]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Unsupported schema_version"));
    }

    @Test
    void testReturnInvalidEventIndices() throws Exception {
        doThrow(new InvalidEventsException(List.of(0, 2)))
                .when(telemetryService).acceptBatch(any());

        mockMvc.perform(post("/telemetry/batches")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"schema_version":1,"events":[{},{},{}]}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.invalid_event_indices[0]").value(0))
                .andExpect(jsonPath("$.invalid_event_indices[1]").value(2));
    }

    private String bearerToken() {
        return "Bearer " + steamAuthService.issueToken().getAccessToken();
    }
}
