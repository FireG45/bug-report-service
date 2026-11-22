package ru.bre.storage.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.bre.storage.dto.LastClosedReport;
import ru.bre.storage.model.TelemetryEvent;

import java.util.Optional;
import java.util.UUID;

public interface TelemetryEventRepository extends JpaRepository<TelemetryEvent, UUID> {

    String INSERT_EVENTS_IGNORE_QUERY = """
            INSERT INTO telemetry_events (event_id, schema_version, name, occurred_at, received_at, build_id,
                installation_id, session_id, run_id, attempt_id, event_seq, level_id, mission_id,
                active_elapsed_ms, x, y, z, properties)
            SELECT CAST(e ->> 'event_id' AS uuid),
                   :schemaVersion,
                   e ->> 'name',
                   CAST(e ->> 'occurred_at' AS timestamptz),
                   now(),
                   e ->> 'build_id',
                   CAST(e ->> 'installation_id' AS uuid),
                   CAST(e ->> 'session_id' AS uuid),
                   CAST(e ->> 'run_id' AS uuid),
                   CAST(e ->> 'attempt_id' AS uuid),
                   CAST(e ->> 'event_seq' AS bigint),
                   e ->> 'level_id',
                   e ->> 'mission_id',
                   CAST(e ->> 'active_elapsed_ms' AS bigint),
                   CAST(e ->> 'x' AS double precision),
                   CAST(e ->> 'y' AS double precision),
                   CAST(e ->> 'z' AS double precision),
                   COALESCE(e -> 'properties', CAST('{}' AS jsonb))
            FROM jsonb_array_elements(CAST(:events AS jsonb)) AS e
            ON CONFLICT (event_id) DO NOTHING
            """;

    String LAST_CLOSED_REPORT_QUERY = """
            WITH terminal AS (
                SELECT DISTINCT ON (e.attempt_id)
                       e.attempt_id,
                       e.session_id,
                       e.run_id,
                       e.build_id,
                       e.level_id,
                       e.mission_id,
                       e.name                     AS terminal_event,
                       e.properties ->> 'outcome' AS outcome,
                       e.occurred_at,
                       e.received_at
                FROM telemetry_events e
                WHERE e.installation_id = :installationId
                  AND e.name IN ('mission_resolved', 'player_died')
                ORDER BY e.attempt_id, e.received_at DESC
            ),
            attempt_time AS (
                SELECT attempt_id,
                       MAX(active_elapsed_ms) AS play_time_ms
                FROM telemetry_events
                WHERE installation_id = :installationId
                GROUP BY attempt_id
            )
            SELECT t.attempt_id     AS "attemptId",
                   t.session_id     AS "sessionId",
                   t.run_id         AS "runId",
                   t.build_id       AS "buildId",
                   t.level_id       AS "levelId",
                   t.mission_id     AS "missionId",
                   t.terminal_event AS "terminalEvent",
                   t.outcome        AS "outcome",
                   t.occurred_at    AS "closedAt",
                   t.received_at    AS "receivedAt",
                   a.play_time_ms   AS "playTimeMs"
            FROM terminal t
            JOIN attempt_time a USING (attempt_id)
            ORDER BY t.received_at DESC
            LIMIT 1
            """;

    @Modifying
    @Query(value = INSERT_EVENTS_IGNORE_QUERY, nativeQuery = true)
    int insertIgnore(@Param("schemaVersion") int schemaVersion, @Param("events") String events);

    @Query(value = LAST_CLOSED_REPORT_QUERY, nativeQuery = true)
    Optional<LastClosedReport> findLastClosedReport(@Param("installationId") UUID installationId);
}
