package ru.bre.storage.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.bre.storage.dto.FeedbackEntity;
import ru.bre.storage.dto.ReportEntity;
import ru.bre.storage.exception.ReportException;
import ru.bre.storage.service.report.FeedbackReportService;

import java.util.List;

@Service
public class ReportServiceFallbackHandler {

    private final List<FeedbackReportService> reportServices;

    @Autowired
    public ReportServiceFallbackHandler(List<FeedbackReportService> reportServices) {
        this.reportServices = reportServices;
    }

    public void handle(ReportEntity reportEntity) {
        for (FeedbackReportService service : reportServices) {
            try {
                service.report(reportEntity);
                return;
            } catch (ReportException e) {
                System.err.println("ReportService failed: " + service.getClass().getSimpleName() + " -> " + e.getMessage());
            }
        }
        throw new RuntimeException("All report services failed");
    }

    public void handle(FeedbackEntity feedbackEntity) {
        for (FeedbackReportService service : reportServices) {
            try {
                service.feedback(feedbackEntity);
                return;
            } catch (ReportException e) {
                System.err.println("FeedbackService failed: " + service.getClass().getSimpleName() + " -> " + e.getMessage());
            }
        }
        throw new RuntimeException("All report services failed");
    }
}

