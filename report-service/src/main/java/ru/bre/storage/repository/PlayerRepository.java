package ru.bre.storage.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import ru.bre.storage.model.Player;

import java.util.UUID;

public interface PlayerRepository extends JpaRepository<Player, UUID> {

    String UPSERT_PLAYER_QUERY = """
            INSERT INTO players (player_id, steam_id64, player_name)
            VALUES (:playerId, :steamId64, :playerName)
            ON CONFLICT (steam_id64) DO UPDATE
                SET player_name = COALESCE(EXCLUDED.player_name, players.player_name)
            """;

    String FIND_PLAYER_ID_QUERY = """
            SELECT player_id FROM players WHERE steam_id64 = :steamId64
            """;

    @Modifying
    @Transactional
    @Query(value = UPSERT_PLAYER_QUERY, nativeQuery = true)
    int upsert(@Param("playerId") UUID playerId,
               @Param("steamId64") long steamId64,
               @Param("playerName") String playerName);

    @Query(value = FIND_PLAYER_ID_QUERY, nativeQuery = true)
    UUID findPlayerIdBySteamId64(@Param("steamId64") long steamId64);
}
