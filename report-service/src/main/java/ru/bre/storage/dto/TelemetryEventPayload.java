package ru.bre.storage.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TelemetryEventPayload {

    @JsonProperty("event_id")
    private UUID eventId;

    private String name;

    @JsonProperty("occurred_at")
    private Instant occurredAt;

    @JsonProperty("build_id")
    private String buildId;

    @JsonProperty("installation_id")
    private UUID installationId;

    @JsonProperty("session_id")
    private UUID sessionId;

    @JsonProperty("run_id")
    private UUID runId;

    @JsonProperty("attempt_id")
    private UUID attemptId;

    @JsonProperty("event_seq")
    private Long eventSeq;

    @JsonProperty("level_id")
    private String levelId;

    @JsonProperty("mission_id")
    private String missionId;

    @JsonProperty("active_elapsed_ms")
    private Long activeElapsedMs;

    private Double x;
    private Double y;
    private Double z;

    private JsonNode properties;
}
