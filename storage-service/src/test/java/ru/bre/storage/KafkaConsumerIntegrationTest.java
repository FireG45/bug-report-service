package ru.bre.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import ru.bre.storage.dto.FeedbackDto;
import ru.bre.storage.dto.ReportDto;
import ru.bre.storage.dto.SummaryDto;
import ru.bre.storage.kafka.KafkaConsumer;
import ru.bre.storage.service.StorageService;

import java.util.Date;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class KafkaConsumerTest {

    private KafkaConsumer kafkaConsumer;

    @Mock
    private StorageService storageService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper();
        kafkaConsumer = new KafkaConsumer(objectMapper, storageService);
    }

    @Test
    void testListenReportMessages() throws Exception {
        ReportDto report = new ReportDto("Title1", "Text1", "img.png", "log.txt");
        String message = objectMapper.writeValueAsString(report);

        kafkaConsumer.listenReportMessages(message);

        ArgumentCaptor<ReportDto> captor = ArgumentCaptor.forClass(ReportDto.class);
        verify(storageService, times(1)).save(captor.capture());
        assertEquals("Title1", captor.getValue().title());
        assertEquals("Text1", captor.getValue().text());
    }

    @Test
    void testListenFeedbackMessages() throws Exception {
        FeedbackDto feedback = new FeedbackDto("Title2", "Text2");
        String message = objectMapper.writeValueAsString(feedback);

        kafkaConsumer.listenFeedbackMessages(message);

        ArgumentCaptor<FeedbackDto> captor = ArgumentCaptor.forClass(FeedbackDto.class);
        verify(storageService, times(1)).save(captor.capture());
        assertEquals("Title2", captor.getValue().title());
        assertEquals("Text2", captor.getValue().text());
    }

    @Test
    void testListenSummaryMessages() throws Exception {
        SummaryDto summary = new SummaryDto("Title3", "Text3", new Date());
        String message = objectMapper.writeValueAsString(summary);

        kafkaConsumer.listenSummaryMessages(message);

        ArgumentCaptor<SummaryDto> captor = ArgumentCaptor.forClass(SummaryDto.class);
        verify(storageService, times(1)).save(captor.capture());
        assertEquals("Title3", captor.getValue().title());
        assertEquals("Text3", captor.getValue().text());
    }
}
