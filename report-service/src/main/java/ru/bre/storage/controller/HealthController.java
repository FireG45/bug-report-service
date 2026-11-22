package ru.bre.storage.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@RestController
public class HealthController {

    private final DataSource dataSource;

    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping("/healthcheck/liveness")
    public ResponseEntity<String> liveness() {
        return ResponseEntity.ok("OK");
    }

    @GetMapping("/healthcheck/readiness")
    public ResponseEntity<String> readiness() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(2)
                    ? ResponseEntity.ok("OK")
                    : ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Database unavailable");
        } catch (SQLException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Database unavailable");
        }
    }

}
