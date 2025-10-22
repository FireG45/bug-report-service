package ru.bre.bugreportservice.service.report.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import ru.bre.bugreportservice.dto.ReportEntity;
import ru.bre.bugreportservice.dto.message.BugReportMessageDto;
import ru.bre.bugreportservice.exception.ReportException;
import ru.bre.bugreportservice.kafka.ReportProducer;
import ru.bre.bugreportservice.service.report.ReportService;

@Order(Ordered.HIGHEST_PRECEDENCE)
@Service
public class KafkaReportServiceImpl implements ReportService {

    private final ReportProducer reportProducer;

    @Autowired
    public KafkaReportServiceImpl(ReportProducer reportProducer) {
        this.reportProducer = reportProducer;
    }

    @Override
    public void report(ReportEntity reportEntity) throws ReportException {
        BugReportMessageDto reportMessageDto = new BugReportMessageDto(
                reportEntity.title(),
                reportEntity.text(),
                null,
                null
        );
        reportProducer.sendReportMessage(reportMessageDto);
    }
}
