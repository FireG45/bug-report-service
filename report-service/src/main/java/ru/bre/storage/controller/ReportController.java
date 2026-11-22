package ru.bre.storage.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.bre.storage.dto.ReportEntity;
import ru.bre.storage.service.ReportService;


@RestController
@CrossOrigin(origins = {"http://localhost:3000", "https://www.mrak-bre.ru"})
public class ReportController {

    private final ReportService reportService;

    private boolean frontendReportEnabled;

    @Value("${server-secret}")
    private String serverSecret;

    @Autowired
    public ReportController(ReportService reportService,
                            @Value("${feature-toggle.frontend-report:true}")
                            boolean frontendReportEnabled) {
        this.reportService = reportService;
        this.frontendReportEnabled = frontendReportEnabled;
    }

    @PostMapping("/report-send")
    public ResponseEntity<String> report(
            @RequestParam("title") String title,
            @RequestParam("text") String text,
            @RequestParam("imageFile") MultipartFile imageFile,
            @RequestParam(value = "logFile", required = false) MultipartFile logFile
    ) {
        reportService.report(new ReportEntity(title, text, imageFile, logFile));
        return ResponseEntity.ok("OK!");
    }

    @PostMapping("/set-frontend-report")
    public ResponseEntity<String> setFrontendReport(
            @RequestParam("value") boolean value,
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

    @GetMapping("/healthcheck")
    public ResponseEntity<String> healthcheck() {
        return ResponseEntity.ok("OK");
    }
}
