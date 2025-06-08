package ru.bre.bugreportservice.controller;

import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ru.bre.bugreportservice.dto.ReportDto;
import ru.bre.bugreportservice.service.ReportService;

import java.util.Objects;

@RestController
public class ReportController {

    private final ReportService reportService;

    private final String secretRef;

    @Autowired
    public ReportController(ReportService reportService, @Value("${secret}") String secretRef) {
        this.reportService = reportService;
        this.secretRef = secretRef;
    }

    @PostMapping("/report-send")
    public ResponseEntity<String> report(
            @RequestParam("title") String title,
            @RequestParam("text") String text,
            @RequestParam("imageFile") MultipartFile imageFile,
            @RequestParam(value = "logFile", required = false) MultipartFile logFile,
            @RequestParam(value = "secret", required = false) String secret
    ) {
        if (!Objects.equals(secret, secretRef)) {
            return ResponseEntity.badRequest().body("ERR!");
        }
        try {
            reportService.report(new ReportDto(title, text), imageFile, logFile);
            return ResponseEntity.ok("OK!");
        } catch (MessagingException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
