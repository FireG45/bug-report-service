package ru.bre.bugreportservice.controller;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ru.bre.bugreportservice.dto.FeedbackDto;
import ru.bre.bugreportservice.dto.BugReportDto;
import ru.bre.bugreportservice.service.ReportServiceFallbackHandler;

import java.time.Duration;

@RestController
public class ReportController {

    private final ReportServiceFallbackHandler reportServiceHandler;
    private final Bucket bucket;

    @Autowired
    public ReportController(ReportServiceFallbackHandler reportServiceHandler) {
        this.reportServiceHandler = reportServiceHandler;
        Bandwidth limit = Bandwidth.classic(2, Refill.greedy(2, Duration.ofMinutes(1)));
        this.bucket = Bucket.builder()
                .addLimit(limit)
                .build();
    }

    @PostMapping("/report-send")
    public ResponseEntity<String> feedback(
            @RequestParam("title") String title,
            @RequestParam("text") String text,
            @RequestParam("imageFile") MultipartFile imageFile,
            @RequestParam(value = "logFile", required = false) MultipartFile logFile
    ) {
        try {
            reportServiceHandler.handle(new BugReportDto(title, text, imageFile, logFile));
            if (bucket.tryConsume(1)) {
                return ResponseEntity.ok("OK!");
            }

            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/feedback-send")
    public ResponseEntity<String> feedback(
            @RequestParam("title") String title,
            @RequestParam("text") String text
    ) {
        try {
            reportServiceHandler.handle(new FeedbackDto(title, text));
            if (bucket.tryConsume(1)) {
                return ResponseEntity.ok("OK!");
            }

            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
