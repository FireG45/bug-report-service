package ru.bre.storage.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import ru.bre.storage.model.AuthToken;

import java.util.Optional;
import java.util.UUID;

public interface AuthTokenRepository extends JpaRepository<AuthToken, String> {

    String FIND_ACTIVE_PLAYER_QUERY = """
            SELECT player_id FROM auth_tokens
            WHERE token_hash = :tokenHash
              AND expires_at > now()
              AND revoked_at IS NULL
            """;

    String DELETE_EXPIRED_QUERY = """
            DELETE FROM auth_tokens WHERE expires_at < now() - interval '1 day'
            """;

    @Query(value = FIND_ACTIVE_PLAYER_QUERY, nativeQuery = true)
    Optional<UUID> findActivePlayerId(@Param("tokenHash") String tokenHash);

    @Modifying
    @Transactional
    @Query(value = DELETE_EXPIRED_QUERY, nativeQuery = true)
    int deleteExpired();
}
