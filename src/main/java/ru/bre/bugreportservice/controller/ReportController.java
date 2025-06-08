package ru.bre.bugreportservice.controller;

import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ru.bre.bugreportservice.dto.ReportDto;
import ru.bre.bugreportservice.service.ReportService;

@RestController
public class ReportController {

    private final ReportService reportService;

    @Autowired
    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping("/report-send")
    public ResponseEntity<String> report(
            @RequestParam("title") String title,
            @RequestParam("text") String text,
            @RequestParam("imageFile") MultipartFile imageFile,
            @RequestParam("logFile") MultipartFile logFile
    ) {
        try {
            reportService.report(new ReportDto(title, text), imageFile, logFile);
            return ResponseEntity.ok("OK!");
        } catch (MessagingException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
