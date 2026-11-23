package ru.bre.storage.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "players")
public class Player {

    @Id
    @Column(name = "player_id")
    private UUID playerId;

    @Column(name = "steam_id64", nullable = false, unique = true)
    private Long steamId64;

    @Column(name = "player_name", length = 64)
    private String playerName;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
