package ru.bre.storage.service;

import ru.bre.storage.dto.FeedbackDto;
import ru.bre.storage.dto.ReportDto;
import ru.bre.storage.dto.SummaryDto;

import java.util.List;

public interface StorageService {
    void save(ReportDto report);

    void save(SummaryDto summary);

    void save(FeedbackDto feedback);

    List<ReportDto> getReports(int offset, int limit);

    List<FeedbackDto> getFeedback(int offset, int limit);

    List<SummaryDto> getSummary(int offset, int limit);
}
