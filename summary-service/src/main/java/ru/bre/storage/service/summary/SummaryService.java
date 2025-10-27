package ru.bre.storage.service.summary;

import ru.bre.storage.entity.Feedback;

import java.util.List;

public interface SummaryService {
    void createSummary(List<Feedback> feedbackList);
}
