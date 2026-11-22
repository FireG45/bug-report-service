package ru.bre.storage.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import ru.bre.storage.dto.TelemetryBatchRequest;
import ru.bre.storage.exception.BatchRejectedException;
import ru.bre.storage.exception.InvalidEventsException;
import ru.bre.storage.service.TelemetryService;

import java.util.Map;

@RestController
public class TelemetryController {

    static final long MAX_BATCH_BYTES = 128 * 1024;

    private final TelemetryService telemetryService;

    public TelemetryController(TelemetryService telemetryService) {
        this.telemetryService = telemetryService;
    }

    @PostMapping("/telemetry/batches")
    public ResponseEntity<Map<String, Object>> acceptBatch(
            @RequestBody(required = false) TelemetryBatchRequest batch,
            HttpServletRequest request
    ) {
        if (request.getContentLengthLong() > MAX_BATCH_BYTES) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                    .body(Map.of("error", "Batch body exceeds 128 KiB"));
        }
        telemetryService.acceptBatch(batch);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @ExceptionHandler(BatchRejectedException.class)
    public ResponseEntity<Map<String, Object>> handleBatchRejected(BatchRejectedException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(InvalidEventsException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidEvents(InvalidEventsException e) {
        return ResponseEntity.unprocessableEntity()
                .body(Map.of("invalid_event_indices", e.getInvalidEventIndices()));
    }
}
