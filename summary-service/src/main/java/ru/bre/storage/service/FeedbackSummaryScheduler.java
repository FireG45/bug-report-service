package ru.bre.storage.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.bre.storage.entity.Feedback;
import ru.bre.storage.repository.FeedbackRepository;
import ru.bre.storage.service.summary.SummaryService;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class FeedbackSummaryScheduler {

    private static final Logger log = LoggerFactory.getLogger(FeedbackSummaryScheduler.class);

    private final FeedbackRepository repository;
    private final SummaryService summaryService;

    @Value("${summary.days-interval:7}")
    private int daysInterval;

    @Autowired
    public FeedbackSummaryScheduler(FeedbackRepository repository, SummaryService summaryService) {
        this.repository = repository;
        this.summaryService = summaryService;
    }

    @PostConstruct
    public void init() {
        log.info("FeedbackSummaryScheduler initialized with daysInterval = {}", daysInterval);
    }

    @Async("jobPool")
    @Scheduled(cron = "${summary.cron:0 0 2 * * *}")
    public void collectFeedbackSummary() throws IOException, InterruptedException {
        LocalDateTime fromDate = LocalDateTime.now().minusDays(daysInterval);
        log.info("Collecting feedback from last {} days (since {})", daysInterval, fromDate);

        List<Feedback> recentFeedbacks = repository.getFeedbackFromDate(fromDate);

        if (recentFeedbacks.isEmpty()) {
            log.info("No feedback found for this period");
            return;
        }

        log.info("Found {} feedback entries, passing to SummaryService", recentFeedbacks.size());
        summaryService.createSummary(null);
    }
}
