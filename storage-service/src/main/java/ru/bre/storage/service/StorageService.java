package ru.bre.storage.service;

import ru.bre.storage.dto.FeedbackDto;
import ru.bre.storage.dto.ReportDto;
import ru.bre.storage.dto.SummaryDto;

import java.util.List;

public interface StorageService {
    int deleteReports();

    int deleteFeedbacks();

    int deleteSummaries();

    void save(ReportDto report);

    void save(SummaryDto summary);

    void save(FeedbackDto feedback);

    List<ReportDto> getReports(int offset, int limit);

    List<FeedbackDto> getFeedback(int offset, int limit);

    List<SummaryDto> getSummary(int offset, int limit);

    int deleteReportById(int id);

    int deleteFeedbackById(int id);

    int deleteSummaryById(int id);
}
