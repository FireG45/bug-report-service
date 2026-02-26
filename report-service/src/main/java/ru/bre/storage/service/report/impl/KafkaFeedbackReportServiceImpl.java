package ru.bre.storage.service.report.impl;

import io.minio.errors.MinioException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.bre.storage.dto.FeedbackEntity;
import ru.bre.storage.dto.ReportEntity;
import ru.bre.storage.dto.message.FeedbackMessage;
import ru.bre.storage.dto.message.ReportMessage;
import ru.bre.storage.exception.ReportException;
import ru.bre.storage.kafka.KafkaProducer;
import ru.bre.storage.service.minio.MinioService;
import ru.bre.storage.service.report.FeedbackReportService;

import java.util.Date;

@Order(Ordered.HIGHEST_PRECEDENCE)
@Service
public class KafkaFeedbackReportServiceImpl implements FeedbackReportService {

    static private final String LOG_FILE_NAME = "logFile";
    static private final String SCREENSHOT_FILE_NAME = "imageFile";

    private final KafkaProducer kafkaProducer;
    private final MinioService minioService;

    @Autowired
    public KafkaFeedbackReportServiceImpl(KafkaProducer kafkaProducer, MinioService minioService) {
        this.kafkaProducer = kafkaProducer;
        this.minioService = minioService;
    }

    @Override
    public void report(ReportEntity reportEntity) throws ReportException {
        try {
            MultipartFile logFile = reportEntity.getLogFile();
            MultipartFile imageFile = reportEntity.getImageFile();

            String logFileName = generateFilename(LOG_FILE_NAME, logFile.getOriginalFilename());
            String imageFileName = generateFilename(SCREENSHOT_FILE_NAME, imageFile.getOriginalFilename());

            ReportMessage reportMessage = new ReportMessage(
                    reportEntity.getTitle(),
                    reportEntity.getText(),
                    imageFileName,
                    logFileName
            );
            kafkaProducer.sendReportMessage(reportMessage);
            minioService.uploadLogFile(logFileName, reportEntity.getLogFile());
            minioService.uploadScreenshotFile(imageFileName, reportEntity.getImageFile());
        } catch (Exception e) {
            throw new ReportException(e);
        }
    }

    @Override
    public void feedback(FeedbackEntity feedbackEntity) throws ReportException {
        try {
            FeedbackMessage reportMessageDto = new FeedbackMessage(
                    feedbackEntity.getTitle(),
                    feedbackEntity.getText()
            );
            kafkaProducer.sendFeedbackMessage(reportMessageDto);
        } catch (Exception e) {
            throw new ReportException(e);
        }
    }

    private String generateFilename(String name, String originalFilename) {
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        return "%s-(%s)%s".formatted(name, new Date().toString(), extension);
    }
}
