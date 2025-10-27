package ru.bre.storage.controller;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import ru.bre.storage.entity.Feedback;
import ru.bre.storage.repository.FeedbackRepository;
import ru.bre.storage.service.summary.SummaryService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/summary")
public class SummaryController {

    private final FeedbackRepository feedbackRepository;
    private final SummaryService summaryService;

    public SummaryController(FeedbackRepository feedbackRepository, SummaryService summaryService) {
        this.feedbackRepository = feedbackRepository;
        this.summaryService = summaryService;
    }

    /**
     * Manually trigger summary generation for all feedbacks from the given date.
     *
     * Example:
     * GET /api/summary/generate?from=2025-10-20T00:00:00
     */
    @GetMapping("/generate")
    public String generateSummary(
            @RequestParam("from")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from
    ) {
        List<Feedback> feedbacks = feedbackRepository.getFeedbackFromDate(from);
        if (feedbacks.isEmpty()) {
            return "No feedback found for the specified period.";
        }

        summaryService.createSummary(feedbacks);
        return "Summary generation started successfully (check logs for output).";
    }
}
