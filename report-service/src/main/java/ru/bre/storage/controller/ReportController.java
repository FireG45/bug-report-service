package ru.bre.storage.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ru.bre.storage.dto.ReportEntity;
import ru.bre.storage.service.ReportService;

@RestController
public class ReportController {

    private static final int MAX_TITLE_LENGTH = 200;
    private static final int MAX_TEXT_LENGTH = 20_000;
    private static final long MAX_IMAGE_BYTES = 10L * 1024 * 1024;
    private static final long MAX_LOG_BYTES = 20L * 1024 * 1024;

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping("/report-send")
    public ResponseEntity<String> report(
            @RequestParam("title") String title,
            @RequestParam("text") String text,
            @RequestParam("imageFile") MultipartFile imageFile,
            @RequestParam(value = "logFile", required = false) MultipartFile logFile
    ) {
        if (title.isBlank() || title.length() > MAX_TITLE_LENGTH) {
            return ResponseEntity.badRequest().body("Invalid title");
        }
        if (text.length() > MAX_TEXT_LENGTH) {
            return ResponseEntity.badRequest().body("Text is too long");
        }
        if (imageFile.getSize() > MAX_IMAGE_BYTES) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body("Screenshot is too large");
        }
        if (logFile != null && logFile.getSize() > MAX_LOG_BYTES) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body("Log file is too large");
        }
        reportService.report(new ReportEntity(title, text, imageFile, logFile));
        return ResponseEntity.ok("OK!");
    }

    @GetMapping("/get-frontend-report")
    public ResponseEntity<Boolean> getFrontendReport() {
        return ResponseEntity.ok(false);
    }

    @GetMapping("/healthcheck")
    public ResponseEntity<String> healthcheck() {
        return ResponseEntity.ok("OK");
    }
}
