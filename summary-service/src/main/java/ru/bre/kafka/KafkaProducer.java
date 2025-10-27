package ru.bre.kafka;


import ru.bre.kafka.message.SummaryMessage;

public interface KafkaProducer {
    void sendSummaryMessage(SummaryMessage summaryMessage);
}
