package ru.bre.storage.service.summary;

import java.io.IOException;
import java.time.LocalDateTime;

public interface SummaryService {
    void createSummary(LocalDateTime from) throws IOException, InterruptedException;

    void createSummaryAsync(LocalDateTime from) throws IOException, InterruptedException;
}
