package ru.bre.storage.model;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "telemetry_events")
public class TelemetryEvent {

    @Id
    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "schema_version", nullable = false)
    private Integer schemaVersion;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "build_id", nullable = false, length = 64)
    private String buildId;

    @Column(name = "installation_id", nullable = false)
    private UUID installationId;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "run_id", nullable = false)
    private UUID runId;

    @Column(name = "attempt_id", nullable = false)
    private UUID attemptId;

    @Column(name = "event_seq", nullable = false)
    private Long eventSeq;

    @Column(name = "level_id", nullable = false, length = 64)
    private String levelId;

    @Column(name = "mission_id", nullable = false, length = 64)
    private String missionId;

    @Column(name = "active_elapsed_ms", nullable = false)
    private Long activeElapsedMs;

    private Double x;
    private Double y;
    private Double z;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "properties", nullable = false)
    private JsonNode properties;
}
