package ru.bre.storage.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import ru.bre.storage.dto.TelemetryBatchRequest;
import ru.bre.storage.dto.TelemetryEventPayload;
import ru.bre.storage.exception.BatchRejectedException;
import ru.bre.storage.exception.InvalidEventsException;
import ru.bre.storage.repository.TelemetryEventRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

class TelemetryServiceTest {

    private static final UUID PLAYER_ID = UUID.fromString("9f1f6c1e-4d4e-4d43-9d20-6a2f1f0a4b7c");

    @Mock
    private TelemetryEventRepository repository;

    private TelemetryService telemetryService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        telemetryService = new TelemetryService(repository, new ObjectMapper().findAndRegisterModules());
    }

    @Test
    void testRejectUnknownSchemaVersion() {
        TelemetryBatchRequest batch = new TelemetryBatchRequest(2, List.of(validEvent()));

        assertThrows(BatchRejectedException.class, () -> telemetryService.acceptBatch(batch, PLAYER_ID));
    }

    @Test
    void testRejectEmptyEvents() {
        TelemetryBatchRequest batch = new TelemetryBatchRequest(1, List.of());

        assertThrows(BatchRejectedException.class, () -> telemetryService.acceptBatch(batch, PLAYER_ID));
    }

    @Test
    void testRejectTooManyEvents() {
        List<TelemetryEventPayload> events = java.util.stream.Stream.generate(this::validEvent)
                .limit(TelemetryService.MAX_EVENTS_PER_BATCH + 1)
                .toList();
        TelemetryBatchRequest batch = new TelemetryBatchRequest(1, events);

        assertThrows(BatchRejectedException.class, () -> telemetryService.acceptBatch(batch, PLAYER_ID));
    }

    @Test
    void testReportInvalidEventIndices() {
        TelemetryEventPayload withoutId = validEvent();
        withoutId.setEventId(null);
        TelemetryEventPayload partialCoordinates = validEvent();
        partialCoordinates.setX(1.0);
        partialCoordinates.setY(null);
        TelemetryBatchRequest batch = new TelemetryBatchRequest(1, List.of(validEvent(), withoutId, partialCoordinates));

        InvalidEventsException exception = assertThrows(InvalidEventsException.class,
                () -> telemetryService.acceptBatch(batch, PLAYER_ID));
        assertEquals(List.of(1, 2), exception.getInvalidEventIndices());
    }

    @Test
    void testPersistValidBatch() {
        TelemetryBatchRequest batch = new TelemetryBatchRequest(1, List.of(validEvent()));

        telemetryService.acceptBatch(batch, PLAYER_ID);

        verify(repository).insertIgnore(eq(1), anyString(), eq(PLAYER_ID.toString()));
    }

    private TelemetryEventPayload validEvent() {
        return new TelemetryEventPayload(
                UUID.randomUUID(),
                "level_entered",
                Instant.parse("2026-11-22T12:00:00Z"),
                "playtest-001",
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                1L,
                "roofs",
                "roofs",
                1000L,
                -173.4,
                -112.1,
                -24.7,
                null);
    }
}
