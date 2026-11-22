package ru.bre.storage.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.bre.storage.config.RateLimitFilter;
import ru.bre.storage.dto.ReportEntity;
import ru.bre.storage.service.ReportService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ReportControllerTest {

    @Mock
    private ReportService reportService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ReportController reportController = new ReportController(reportService, true);
        RateLimitFilter rateLimitFilter = new RateLimitFilter();
        mockMvc = MockMvcBuilders.standaloneSetup(reportController)
                .addFilters(rateLimitFilter)
                .build();
    }

    @Test
    void testReportSendSuccess() throws Exception {
        MockMultipartFile imageFile = new MockMultipartFile("imageFile", "image.png",
                MediaType.IMAGE_PNG_VALUE, "dummy image content".getBytes());

        mockMvc.perform(multipart("/report-send")
                        .file(imageFile)
                        .param("title", "Test Report")
                        .param("text", "Some text"))
                .andExpect(status().isOk())
                .andExpect(content().string("OK!"));

        ArgumentCaptor<ReportEntity> captor = ArgumentCaptor.forClass(ReportEntity.class);
        verify(reportService, times(1)).report(captor.capture());

        ReportEntity entity = captor.getValue();
        assertEquals("Test Report", entity.getTitle());
        assertEquals("Some text", entity.getText());
        assertNotNull(entity.getImageFile());
    }

    @Test
    void testReportSendTooManyRequests() throws Exception {
        MockMultipartFile imageFile = new MockMultipartFile("imageFile", "image.png",
                MediaType.IMAGE_PNG_VALUE, "dummy image".getBytes());

        for (int i = 0; i < 3 ; i++) {
            mockMvc.perform(multipart("/report-send")
                            .file(imageFile)
                            .param("title", "R" + i)
                            .param("text", "Text" + i))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(multipart("/report-send")
                        .file(imageFile)
                        .param("title", "R3")
                        .param("text", "Text3"))
                .andExpect(status().isTooManyRequests());
    }

}
