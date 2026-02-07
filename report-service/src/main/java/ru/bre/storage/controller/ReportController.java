package ru.bre.storage.controller;

import jakarta.websocket.server.PathParam;
import org.apache.kafka.common.protocol.types.Field;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.bre.storage.dto.FeedbackEntity;
import ru.bre.storage.dto.ReportEntity;
import ru.bre.storage.service.ReportServiceFallbackHandler;


@RestController
@CrossOrigin(origins = {"http://localhost:3000", "https://www.mrak-bre.ru"})
public class ReportController {

    private final ReportServiceFallbackHandler reportServiceHandler;

    private boolean frontendReportEnabled;

    @Value("${server-secret}")
    private String serverSecret;

    @Autowired
    public ReportController(ReportServiceFallbackHandler reportServiceHandler,
                            @Value("${feature-toggle.frontend-report:true}")
                            boolean frontendReportEnabled) {
        this.reportServiceHandler = reportServiceHandler;
        this.frontendReportEnabled = frontendReportEnabled;
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

    @PostMapping("/set-frontend-report/{value}")
    public ResponseEntity<String> setFrontendReport(
            @PathParam("value") boolean value,
            @RequestParam("secret") String secret
    ) {
        if (secret.equals(serverSecret)) {
            frontendReportEnabled = value;
            return ResponseEntity.ok("frontendReportEnabled = " + frontendReportEnabled);
        } else {
            return ResponseEntity.badRequest().body("Wrong secret!");
        }
    }

    @GetMapping("/get-frontend-report")
    public ResponseEntity<Boolean> getFrontendReport() {
        return ResponseEntity.ok(frontendReportEnabled);
    }
}
