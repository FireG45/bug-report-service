package ru.bre.storage.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriBuilder;
import ru.bre.storage.config.SteamAuthProperties;
import ru.bre.storage.exception.SteamTicketRejectedException;
import ru.bre.storage.exception.SteamUnavailableException;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * Вызовы Steam Web API. Publisher Key, билеты и токены не попадают в журнал:
 * сообщения исключений RestClient содержат URI с key, поэтому логируется только класс ошибки.
 */
@Service
public class SteamWebApiClient {

    private static final Logger log = LoggerFactory.getLogger(SteamWebApiClient.class);
    private static final int MAX_PLAYER_NAME_LENGTH = 64;
    private static final String OK = "OK";

    private final RestClient restClient;
    private final SteamAuthProperties properties;

    public SteamWebApiClient(RestClient steamRestClient, SteamAuthProperties properties) {
        this.restClient = steamRestClient;
        this.properties = properties;
        if (properties.getPublisherKey().isBlank()) {
            log.error("STEAM_PUBLISHER_KEY is not configured: Steam auth will answer 503");
        }
    }

    public long authenticateTicket(String ticket) {
        JsonNode response = get("/ISteamUserAuth/AuthenticateUserTicket/v1/", query -> query
                .queryParam("appid", properties.getAppId())
                .queryParam("ticket", ticket)
                .queryParam("identity", properties.getIdentity()))
                .path("response");

        JsonNode error = response.path("error");
        if (!error.isMissingNode()) {
            log.warn("Steam rejected the ticket: {}", steamErrorCode(error));
            throw new SteamTicketRejectedException();
        }
        JsonNode params = response.path("params");
        if (!OK.equals(params.path("result").asText())) {
            log.warn("Steam ticket authentication returned result={}", params.path("result").asText(""));
            throw new SteamTicketRejectedException();
        }
        try {
            return Long.parseLong(params.path("steamid").asText(""));
        } catch (NumberFormatException e) {
            log.warn("Steam ticket authentication returned an invalid SteamID64");
            throw new SteamUnavailableException();
        }
    }

    public boolean ownsApp(long steamId) {
        JsonNode ownership = get("/ISteamUser/CheckAppOwnership/v4/", query -> query
                .queryParam("steamid", steamId)
                .queryParam("appid", properties.getAppId()))
                .path("appownership");
        if (!OK.equals(ownership.path("result").asText())) {
            log.warn("Steam app ownership check returned an unexpected payload");
            throw new SteamUnavailableException();
        }
        return ownership.path("ownsapp").asBoolean(false);
    }

    public Optional<String> fetchPlayerName(long steamId) {
        try {
            String name = get("/ISteamUser/GetPlayerSummaries/v2/", query -> query
                    .queryParam("steamids", steamId))
                    .path("response").path("players").path(0).path("personaname").asText("").trim();
            if (name.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(name.length() <= MAX_PLAYER_NAME_LENGTH
                    ? name
                    : name.substring(0, MAX_PLAYER_NAME_LENGTH));
        } catch (SteamUnavailableException e) {
            log.warn("Failed to fetch Steam persona name, continuing without it");
            return Optional.empty();
        }
    }

    private JsonNode get(String path, Consumer<UriBuilder> queryParams) {
        if (properties.getPublisherKey().isBlank()) {
            throw new SteamUnavailableException();
        }
        try {
            JsonNode body = restClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.path(path)
                                .queryParam("key", properties.getPublisherKey());
                        queryParams.accept(uriBuilder);
                        return uriBuilder.build();
                    })
                    .retrieve()
                    .body(JsonNode.class);
            if (body == null || body.isMissingNode()) {
                log.warn("Steam Web API returned an empty body for {}", path);
                throw new SteamUnavailableException();
            }
            return body;
        } catch (RestClientException e) {
            log.warn("Steam Web API request to {} failed: {}", path, e.getClass().getSimpleName());
            throw new SteamUnavailableException();
        }
    }

    private String steamErrorCode(JsonNode error) {
        JsonNode code = error.path("errorcode");
        if (code.isMissingNode()) {
            code = error.path("errcode");
        }
        String description = error.path("errordesc").asText("");
        return description.isEmpty() ? code.asText("?") : code.asText("?") + " " + description;
    }
}
