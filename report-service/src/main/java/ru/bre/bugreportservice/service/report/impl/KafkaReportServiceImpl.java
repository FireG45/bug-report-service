package ru.bre.bugreportservice.service.report.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import ru.bre.bugreportservice.dto.ReportEntity;
import ru.bre.bugreportservice.dto.message.BugReportMessageDto;
import ru.bre.bugreportservice.exception.ReportException;
import ru.bre.bugreportservice.kafka.ReportProducer;
import ru.bre.bugreportservice.service.minio.MinioService;
import ru.bre.bugreportservice.service.report.ReportService;

import java.util.Date;

@Order(Ordered.HIGHEST_PRECEDENCE)
@Service
public class KafkaReportServiceImpl implements ReportService {

    static private final String LOG_FILE_NAME = "logFile";
    static private final String SCREENSHOT_FILE_NAME = "imageFile";

    private final ReportProducer reportProducer;
    private final MinioService minioService;

    @Autowired
    public KafkaReportServiceImpl(ReportProducer reportProducer, MinioService minioService) {
        this.reportProducer = reportProducer;
        this.minioService = minioService;
    }

    @Override
    public void report(ReportEntity reportEntity) throws ReportException {
        String logFileName = generateFilename(LOG_FILE_NAME);
        String imageFileName = generateFilename(SCREENSHOT_FILE_NAME);

        BugReportMessageDto reportMessageDto = new BugReportMessageDto(
                reportEntity.title(),
                reportEntity.text(),
                logFileName,
                imageFileName
        );
        reportProducer.sendReportMessage(reportMessageDto);
        minioService.uploadLogFile(logFileName, reportEntity.logFile());
        minioService.uploadScreenshotFile(imageFileName, reportEntity.imageFile());
    }

    private String generateFilename(String name) {
        return "%s-(%s).jpg".formatted(name, new Date().toString());
    }
}
