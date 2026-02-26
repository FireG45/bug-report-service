package ru.bre.storage.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.bre.storage.dto.message.FeedbackMessage;
import ru.bre.storage.dto.message.ReportMessage;
import ru.bre.storage.exception.ReportException;

import java.util.concurrent.ExecutionException;

@Component
public class KafkaProducerImpl implements KafkaProducer {

    @Value("${kafka.report.topic:bre.report.message}")
    private String reportTopic;

    @Value("${kafka.feedback.topic:bre.feedback.message}")
    private String feedbackTopic;

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public KafkaProducerImpl(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void sendReportMessage(ReportMessage reportMessage) {
        try {
            kafkaTemplate.send(reportTopic, convertToMessage(reportMessage)).get();
        } catch (ExecutionException | InterruptedException e) {
            throw new ReportException(e);
        }
    }

    @Override
    public void sendFeedbackMessage(FeedbackMessage feedbackMessage) {
        try {
            kafkaTemplate.send(feedbackTopic, convertToMessage(feedbackMessage)).get();
        } catch (ExecutionException | InterruptedException e) {
            throw new ReportException(e);
        }
    }

    private String convertToMessage(Object event) {
        try {
            return objectMapper.writeValueAsString(event);
        }
        catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
