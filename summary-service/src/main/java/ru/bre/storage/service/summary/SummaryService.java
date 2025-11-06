package ru.bre.storage.service.summary;

import ru.bre.storage.entity.Feedback;

import java.io.IOException;
import java.util.List;

public interface SummaryService {
    void createSummary(List<Feedback> feedbackList) throws IOException, InterruptedException;
}
