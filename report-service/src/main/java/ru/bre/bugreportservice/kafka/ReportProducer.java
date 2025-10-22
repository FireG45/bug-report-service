package ru.bre.bugreportservice.kafka;

import ru.bre.bugreportservice.dto.message.BugReportMessageDto;

public interface ReportProducer {
    void sendReportMessage(BugReportMessageDto reportMessage);
}
