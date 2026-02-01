package ru.bre.storage.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ru.bre.storage.dto.FeedbackEntity;
import ru.bre.storage.dto.ReportEntity;
import ru.bre.storage.service.ReportServiceFallbackHandler;


@RestController
@CrossOrigin(origins = {"http://localhost:3000", "https://www.mrak-bre.ru"})
public class ReportController {

    private final ReportServiceFallbackHandler reportServiceHandler;

    @Autowired
    public ReportController(ReportServiceFallbackHandler reportServiceHandler) {
        this.reportServiceHandler = reportServiceHandler;
    }

    @PostMapping("/report-send")
    public ResponseEntity<String> feedback(
            @RequestParam("title") String title,
            @RequestParam("text") String text,
            @RequestParam("imageFile") MultipartFile imageFile,
            @RequestParam(value = "logFile", required = false) MultipartFile logFile
    ) {
        reportServiceHandler.handle(new ReportEntity(title, text, imageFile, logFile));
        return ResponseEntity.ok("OK!");
    }

    @PostMapping("/feedback-send")
    public ResponseEntity<String> feedback(
            @RequestParam("title") String title,
            @RequestParam("text") String text
    ) {
        reportServiceHandler.handle(new FeedbackEntity(title, text));
        return ResponseEntity.ok("OK!");
    }
}
