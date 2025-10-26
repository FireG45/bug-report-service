package ru.bre.storage.controller;

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
import ru.bre.storage.dto.FeedbackEntity;
import ru.bre.storage.dto.ReportEntity;
import ru.bre.storage.service.ReportServiceFallbackHandler;

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
            if (bucket.tryConsume(1)) {
                reportServiceHandler.handle(new ReportEntity(title, text, imageFile, logFile));
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
            if (bucket.tryConsume(1)) {
                reportServiceHandler.handle(new FeedbackEntity(title, text));
                return ResponseEntity.ok("OK!");
            }

            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
