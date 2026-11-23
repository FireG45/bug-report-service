package ru.bre.storage.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.bre.storage.dto.LastClosedReport;
import ru.bre.storage.dto.TelemetryBatchRequest;
import ru.bre.storage.dto.TelemetryEventPayload;
import ru.bre.storage.exception.BatchRejectedException;
import ru.bre.storage.exception.InvalidEventsException;
import ru.bre.storage.repository.TelemetryEventRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TelemetryService {

    static final int SCHEMA_VERSION = 1;
    static final int MAX_EVENTS_PER_BATCH = 50;
    static final int MAX_EVENT_BYTES = 2048;
    static final int MAX_STRING_LENGTH = 64;

    private final TelemetryEventRepository repository;
    private final ObjectMapper objectMapper;

    public TelemetryService(TelemetryEventRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void acceptBatch(TelemetryBatchRequest batch, UUID playerId) {
        List<TelemetryEventPayload> events = validateEnvelope(batch);
        List<Integer> invalidIndices = findInvalidIndices(events);
        if (!invalidIndices.isEmpty()) {
            throw new InvalidEventsException(invalidIndices);
        }
        events.forEach(event -> {
            if (event.getProperties() == null || event.getProperties().isNull()) {
                event.setProperties(objectMapper.createObjectNode());
            }
        });
        repository.insertIgnore(batch.getSchemaVersion(), toJson(events), playerId.toString());
    }

    public Optional<LastClosedReport> lastClosedReport(UUID installationId) {
        return repository.findLastClosedReport(installationId);
    }

    private List<TelemetryEventPayload> validateEnvelope(TelemetryBatchRequest batch) {
        if (batch == null) {
            throw new BatchRejectedException("Request body is required");
        }
        if (batch.getSchemaVersion() == null || batch.getSchemaVersion() != SCHEMA_VERSION) {
            throw new BatchRejectedException("Unsupported schema_version");
        }
        if (batch.getEvents() == null || batch.getEvents().isEmpty()) {
            throw new BatchRejectedException("events must not be empty");
        }
        if (batch.getEvents().size() > MAX_EVENTS_PER_BATCH) {
            throw new BatchRejectedException("Too many events in one batch: " + batch.getEvents().size());
        }
        return batch.getEvents();
    }

    private List<Integer> findInvalidIndices(List<TelemetryEventPayload> events) {
        List<Integer> invalidIndices = new ArrayList<>();
        for (int index = 0; index < events.size(); index++) {
            if (!isValid(events.get(index))) {
                invalidIndices.add(index);
            }
        }
        return invalidIndices;
    }

    private boolean isValid(TelemetryEventPayload event) {
        if (event == null) {
            return false;
        }
        return event.getEventId() != null
                && event.getOccurredAt() != null
                && event.getInstallationId() != null
                && event.getSessionId() != null
                && event.getRunId() != null
                && event.getAttemptId() != null
                && isTechnicalString(event.getName())
                && isTechnicalString(event.getBuildId())
                && isTechnicalString(event.getLevelId())
                && isTechnicalString(event.getMissionId())
                && event.getEventSeq() != null && event.getEventSeq() > 0
                && event.getActiveElapsedMs() != null && event.getActiveElapsedMs() >= 0
                && hasValidCoordinates(event)
                && serializedSize(event) <= MAX_EVENT_BYTES;
    }

    private boolean isTechnicalString(String value) {
        return value != null && !value.isBlank() && value.length() <= MAX_STRING_LENGTH;
    }

    private boolean hasValidCoordinates(TelemetryEventPayload event) {
        Double x = event.getX();
        Double y = event.getY();
        Double z = event.getZ();
        if (x == null && y == null && z == null) {
            return true;
        }
        return x != null && y != null && z != null
                && Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z);
    }

    private int serializedSize(TelemetryEventPayload event) {
        try {
            return objectMapper.writeValueAsBytes(event).length;
        } catch (JsonProcessingException e) {
            return Integer.MAX_VALUE;
        }
    }

    private String toJson(List<TelemetryEventPayload> events) {
        try {
            return objectMapper.writeValueAsString(events);
        } catch (JsonProcessingException e) {
            throw new BatchRejectedException("Failed to serialize events");
        }
    }
}
