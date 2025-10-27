package ru.bre.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import ru.bre.kafka.message.SummaryMessage;

public class KafkaProducerImpl implements KafkaProducer {

    @Value("${kafka.summary.topic:bre.summary.message}")
    private String summaryTopic;

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public KafkaProducerImpl(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void sendSummaryMessage(SummaryMessage summaryMessage) {
        kafkaTemplate.send(summaryTopic, convertToMessage(summaryMessage));
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
