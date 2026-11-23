package ru.bre.storage.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "telemetry.steam-auth")
public class SteamAuthProperties {

    private long appId;
    private String identity;
    private String publisherKey = "";
    private String baseUrl = "https://partner.steam-api.com";
    private Duration connectTimeout = Duration.ofSeconds(2);
    private Duration readTimeout = Duration.ofSeconds(5);
    private Duration tokenTtl = Duration.ofHours(1);
}
