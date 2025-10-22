package ru.bre.bugreportservice.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.bre.bugreportservice.dto.message.BugReportMessageDto;

@Component
public class ReportProducerImpl implements ReportProducer {

    @Value("${kafka.report.topic:bre.report.message}")
    private String reportTopic;

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public ReportProducerImpl(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void sendReportMessage(BugReportMessageDto reportMessage) {
        kafkaTemplate.send(reportTopic, convertToMessage(reportMessage));
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
