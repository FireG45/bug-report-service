package ru.bre.healthcheck.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.bre.healthcheck.model.ServiceStatus;
import ru.bre.healthcheck.service.HealthcheckService;

import java.util.List;

@RestController
@RequestMapping("/api/healthcheck")
@RequiredArgsConstructor
public class HealthcheckController {

    private final HealthcheckService healthcheckService;

    @GetMapping("/statuses")
    public ResponseEntity<List<ServiceStatus>> getStatuses() {
        return ResponseEntity.ok(healthcheckService.checkAll());
    }
}
