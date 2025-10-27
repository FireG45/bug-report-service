package ru.bre.storage.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.bre.storage.controller.ReportController;
import ru.bre.storage.dto.FeedbackEntity;
import ru.bre.storage.dto.ReportEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ReportControllerFallbackTest {

    @Mock
    private ReportServiceFallbackHandler reportServiceHandler;

    @InjectMocks
    private ReportController reportController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(reportController).build();
    }

    @Test
    void testFallbackHandlerCalledForReport() throws Exception {
        MockMultipartFile imageFile = new MockMultipartFile("imageFile", "image.png",
                MediaType.IMAGE_PNG_VALUE, "dummy image".getBytes());

        mockMvc.perform(multipart("/report-send")
                        .file(imageFile)
                        .param("title", "Report Title")
                        .param("text", "Report Text"))
                .andExpect(status().isOk());

        ArgumentCaptor<ReportEntity> captor = ArgumentCaptor.forClass(ReportEntity.class);
        verify(reportServiceHandler, times(1)).handle(captor.capture());

        ReportEntity entity = captor.getValue();
        assertEquals("Report Title", entity.getTitle());
        assertEquals("Report Text", entity.getText());
        assertNotNull(entity.getImageFile());
    }

    @Test
    void testFallbackHandlerCalledForFeedback() throws Exception {
        mockMvc.perform(multipart("/feedback-send")
                        .param("title", "Feedback Title")
                        .param("text", "Feedback Text"))
                .andExpect(status().isOk());

        ArgumentCaptor<FeedbackEntity> captor = ArgumentCaptor.forClass(FeedbackEntity.class);
        verify(reportServiceHandler, times(1)).handle(captor.capture());

        FeedbackEntity entity = captor.getValue();
        assertEquals("Feedback Title", entity.getTitle());
        assertEquals("Feedback Text", entity.getText());
    }
}
