package ru.bre.storage.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.bre.storage.dto.FeedbackDto;
import ru.bre.storage.dto.ReportDto;
import ru.bre.storage.dto.SummaryDto;
import ru.bre.storage.service.StorageService;

import java.util.List;

@RequestMapping("/v1/api")
@RestController
public class StorageController {

    private final StorageService storageService;

    @Value("${server-secret}")
    private String serverSecret;

    @Autowired
    public StorageController(StorageService storageService) {
        this.storageService = storageService;
    }

    @GetMapping("/report")
    public ResponseEntity<List<ReportDto>> getReports(@RequestParam Integer offset, @RequestParam Integer limit, @RequestParam String secret) {
        return serverSecret.equals(secret) ? ResponseEntity.ok(storageService.getReports(offset, limit)) : ResponseEntity.badRequest().build();
    }

    @GetMapping("/feedback")
    public ResponseEntity<List<FeedbackDto>> getFeedback(@RequestParam Integer offset, @RequestParam Integer limit, @RequestParam String secret) {
        return serverSecret.equals(secret) ? ResponseEntity.ok(storageService.getFeedback(offset, limit)) : ResponseEntity.badRequest().build();
    }

    @GetMapping("/summary")
    public ResponseEntity<List<SummaryDto>> getSummary(@RequestParam Integer offset, @RequestParam Integer limit, @RequestParam String secret) {
        return serverSecret.equals(secret) ? ResponseEntity.ok(storageService.getSummary(offset, limit)) : ResponseEntity.badRequest().build();
    }

    @GetMapping("/healthcheck")
    public ResponseEntity<String> healthcheck() {
        return ResponseEntity.ok("OK");
    }

    @DeleteMapping("/{entity}/{id}")
    public ResponseEntity<String> deleteById(@PathVariable String entity, @PathVariable Integer id, @RequestParam String secret) {
        if (!serverSecret.equals(secret)) {
            return ResponseEntity.badRequest().build();
        }
        try {
            int deleted = switch (entity) {
                case "report" -> storageService.deleteReportById(id);
                case "feedback" -> storageService.deleteFeedbackById(id);
                case "summary" -> storageService.deleteSummaryById(id);
                default -> 0;
            };
            if (deleted == 0) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(String.valueOf(deleted));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @DeleteMapping("/clean/{entity}")
    public ResponseEntity<String> clean(@PathVariable String entity, @RequestParam String secret) {
        if (!serverSecret.equals(secret)) {
            return ResponseEntity.badRequest().build();
        }
        try {
            int deleted = 0;
            switch (entity) {
                case "feedback" -> deleted = storageService.deleteFeedbacks();
                case "report" -> deleted = storageService.deleteReports();
                case "summary" -> deleted = storageService.deleteSummaries();
                default -> ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(String.valueOf(deleted));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }
}
