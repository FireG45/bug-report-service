package ru.bre.storage.controller;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.bre.storage.entity.Feedback;
import ru.bre.storage.repository.FeedbackRepository;
import ru.bre.storage.service.summary.SummaryService;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/summary")
public class SummaryController {

    private final SummaryService summaryService;

    public SummaryController(SummaryService summaryService) {
        this.summaryService = summaryService;
    }

    @GetMapping("/healthcheck")
    public ResponseEntity<String> healthcheck() {
        return ResponseEntity.ok("OK");
    }

    @GetMapping("/generate")
    public String generateSummary(
            @RequestParam("from")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from
    ) throws IOException, InterruptedException {
        summaryService.createSummaryAsync(from);
        return "Summary generation background task started successfully";
    }
}
