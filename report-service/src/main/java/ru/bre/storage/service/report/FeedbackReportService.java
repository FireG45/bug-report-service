package ru.bre.storage.service.report;

import ru.bre.storage.dto.FeedbackEntity;
import ru.bre.storage.dto.ReportEntity;
import ru.bre.storage.exception.ReportException;

public interface FeedbackReportService {
    void report(ReportEntity reportEntity) throws ReportException;
    void feedback(FeedbackEntity feedbackEntity) throws ReportException;
}
