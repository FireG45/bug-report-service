package ru.bre.storage.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.bre.storage.dto.FeedbackDto;
import ru.bre.storage.dto.ReportDto;
import ru.bre.storage.dto.SummaryDto;
import ru.bre.storage.service.StorageService;

import java.util.logging.Logger;

@Component
public class KafkaConsumer {

    private final Logger log = Logger.getLogger(this.getClass().getName());

    private final ObjectMapper objectMapper;

    private final StorageService storageService;

    @Autowired
    public KafkaConsumer(ObjectMapper objectMapper, StorageService storageService) {
        this.objectMapper = objectMapper;
        this.storageService = storageService;
    }

    @KafkaListener(id = "${kafka.config.consumer.report.group-id}", topics = "${kafka.report.topic}")
    public void listenReportMessages(String message) throws JsonProcessingException {
        storageService.save(objectMapper.readValue(message, ReportDto.class));
        log.info("Report message received: " + message);
    }

    @KafkaListener(id = "${kafka.config.consumer.feedback.group-id}", topics = "${kafka.feedback.topic}")
    public void listenFeedbackMessages(String message) throws JsonProcessingException {
        storageService.save(objectMapper.readValue(message, FeedbackDto.class));
        log.info("Feedback message received: " + message);
    }

    @KafkaListener(id = "${kafka.config.consumer.summary.group-id}", topics = "${kafka.summary.topic}")
    public void listenSummaryMessages(String message) throws JsonProcessingException {
        storageService.save(objectMapper.readValue(message, SummaryDto.class));
        log.info("Summary message received: " + message);
    }
}
