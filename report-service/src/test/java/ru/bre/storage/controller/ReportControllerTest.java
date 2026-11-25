package ru.bre.storage.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.bre.storage.config.RateLimitFilter;
import ru.bre.storage.config.ReportAuthFilter;
import ru.bre.storage.dto.ReportEntity;
import ru.bre.storage.service.ReportService;
import ru.bre.storage.service.SteamAuthService;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReportControllerTest {

    private static final String VALID_TOKEN = "valid-token";
    private static final UUID PLAYER_ID = UUID.fromString("9f1f6c1e-4d4e-4d43-9d20-6a2f1f0a4b7c");

    @Mock
    private ReportService reportService;

    private SteamAuthService steamAuthService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        steamAuthService = mock(SteamAuthService.class);
        when(steamAuthService.resolvePlayerId(VALID_TOKEN)).thenReturn(Optional.of(PLAYER_ID));
        mockMvc = MockMvcBuilders.standaloneSetup(new ReportController(reportService))
                .addFilters(new RateLimitFilter(), new ReportAuthFilter(steamAuthService, true, 3))
                .build();
    }

    @Test
    void testReportSendSuccess() throws Exception {
        mockMvc.perform(multipart("/report-send")
                        .file(imageFile())
                        .param("title", "Test Report")
                        .param("text", "Some text")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + VALID_TOKEN))
                .andExpect(status().isOk())
                .andExpect(content().string("OK!"));

        ArgumentCaptor<ReportEntity> captor = ArgumentCaptor.forClass(ReportEntity.class);
        verify(reportService, times(1)).report(captor.capture(), eq(PLAYER_ID));

        ReportEntity entity = captor.getValue();
        assertEquals("Test Report", entity.getTitle());
        assertEquals("Some text", entity.getText());
        assertNotNull(entity.getImageFile());
    }

    @Test
    void testRejectReportWithoutToken() throws Exception {
        mockMvc.perform(multipart("/report-send")
                        .file(imageFile())
                        .param("title", "Test Report")
                        .param("text", "Some text"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"));

        verifyNoInteractions(reportService);
    }

    @Test
    void testRejectReportWithUnknownToken() throws Exception {
        mockMvc.perform(multipart("/report-send")
                        .file(imageFile())
                        .param("title", "Test Report")
                        .param("text", "Some text")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer unknown"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(reportService);
    }

    @Test
    void testRejectNonBearerAuthorization() throws Exception {
        mockMvc.perform(multipart("/report-send")
                        .file(imageFile())
                        .param("title", "Test Report")
                        .param("text", "Some text")
                        .header(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNz"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(reportService);
    }

    @Test
    void testRejectOverAccountRateLimit() throws Exception {
        MockMvc limitedMvc = MockMvcBuilders.standaloneSetup(new ReportController(reportService))
                .addFilters(new ReportAuthFilter(steamAuthService, true, 1))
                .build();

        limitedMvc.perform(multipart("/report-send")
                        .file(imageFile())
                        .param("title", "First")
                        .param("text", "Some text")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + VALID_TOKEN))
                .andExpect(status().isOk());

        MvcResult second = limitedMvc.perform(multipart("/report-send")
                        .file(imageFile())
                        .param("title", "Second")
                        .param("text", "Some text")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + VALID_TOKEN))
                .andExpect(status().isTooManyRequests())
                .andReturn();

        assertNotNull(second.getResponse().getHeader(HttpHeaders.RETRY_AFTER));
    }

    @Test
    void testAnonymousReportAllowedWhenAuthOptional() throws Exception {
        MockMvc optionalMvc = MockMvcBuilders.standaloneSetup(new ReportController(reportService))
                .addFilters(new ReportAuthFilter(steamAuthService, false, 3))
                .build();

        optionalMvc.perform(multipart("/report-send")
                        .file(imageFile())
                        .param("title", "Anonymous")
                        .param("text", "Some text"))
                .andExpect(status().isOk())
                .andExpect(content().string("OK!"));

        verify(reportService).report(any(ReportEntity.class), isNull());
    }

    @Test
    void testInvalidTokenRejectedEvenWhenAuthOptional() throws Exception {
        MockMvc optionalMvc = MockMvcBuilders.standaloneSetup(new ReportController(reportService))
                .addFilters(new ReportAuthFilter(steamAuthService, false, 3))
                .build();

        optionalMvc.perform(multipart("/report-send")
                        .file(imageFile())
                        .param("title", "Anonymous")
                        .param("text", "Some text")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer unknown"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(reportService);
    }

    @Test
    void testReportSendTooManyRequests() throws Exception {
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(multipart("/report-send")
                            .file(imageFile())
                            .param("title", "R" + i)
                            .param("text", "Text" + i)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + VALID_TOKEN))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(multipart("/report-send")
                        .file(imageFile())
                        .param("title", "R3")
                        .param("text", "Text3")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + VALID_TOKEN))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void testRejectTooLongTitle() throws Exception {
        mockMvc.perform(multipart("/report-send")
                        .file(imageFile())
                        .param("title", "x".repeat(201))
                        .param("text", "Some text")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + VALID_TOKEN))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(reportService);
    }

    @Test
    void testFrontendReportToggleAlwaysFalse() throws Exception {
        mockMvc.perform(get("/get-frontend-report"))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    private MockMultipartFile imageFile() {
        return new MockMultipartFile("imageFile", "image.png",
                MediaType.IMAGE_PNG_VALUE, "dummy image content".getBytes());
    }
}
