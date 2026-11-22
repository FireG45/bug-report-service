package ru.bre.storage.dto;

import java.time.Instant;
import java.util.UUID;

public interface LastClosedReport {

    UUID getAttemptId();

    UUID getSessionId();

    UUID getRunId();

    String getBuildId();

    String getLevelId();

    String getMissionId();

    String getTerminalEvent();

    String getOutcome();

    Long getPlayTimeMs();

    Instant getClosedAt();

    Instant getReceivedAt();
}
