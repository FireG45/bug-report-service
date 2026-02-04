package ru.bre.storage.controller;

import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    public StorageController(StorageService storageService) {
        this.storageService = storageService;
    }

    @GetMapping("/report")
    public List<ReportDto> getReports(@RequestParam Integer offset, @RequestParam Integer limit) {
        return storageService.getReports(offset, limit);
    }

    @GetMapping("/feedback")
    public List<FeedbackDto> getFeedback(@RequestParam Integer offset, @RequestParam Integer limit) {
        return storageService.getFeedback(offset, limit);
    }

    @GetMapping("/summary")
    public List<SummaryDto> getSummary(@RequestParam Integer offset, @RequestParam Integer limit) {
        return storageService.getSummary(offset, limit);
    }

    @DeleteMapping("/clean/{entity}")
    public ResponseEntity<String> clean(@PathVariable String entity) {
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
