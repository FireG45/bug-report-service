package ru.bre.storage.storage.repo;

import ru.bre.storage.storage.dto.FeedbackDto;
import ru.bre.storage.storage.dto.ReportDto;
import ru.bre.storage.storage.dto.SummaryDto;

public interface StorageRepository {
    void save(ReportDto report);

    void save(SummaryDto summary);

    void save(FeedbackDto feedback);
}
