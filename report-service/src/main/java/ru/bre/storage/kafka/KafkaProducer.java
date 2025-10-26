package ru.bre.storage.kafka;

import ru.bre.storage.dto.message.FeedbackMessage;
import ru.bre.storage.dto.message.ReportMessage;

public interface KafkaProducer {
    void sendReportMessage(ReportMessage reportMessage);

    void sendFeedbackMessage(FeedbackMessage reportMessageDto);
}
