package ru.bre.storage.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import ru.bre.storage.config.SteamAuthProperties;
import ru.bre.storage.exception.SteamTicketRejectedException;
import ru.bre.storage.exception.SteamUnavailableException;

import java.util.Optional;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class SteamWebApiClientTest {

    private static final long STEAM_ID = 76561198000000000L;
    private static final String BASE_URL = "https://partner.steam-api.com";

    private MockRestServiceServer server;
    private SteamWebApiClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        SteamAuthProperties properties = new SteamAuthProperties();
        properties.setAppId(5536570L);
        properties.setIdentity("mrak-telemetry");
        properties.setPublisherKey("test-key");
        client = new SteamWebApiClient(builder.baseUrl(BASE_URL).build(), properties);
    }

    @Test
    void testAuthenticateTicket() {
        server.expect(requestTo(startsWith(BASE_URL + "/ISteamUserAuth/AuthenticateUserTicket/v1/")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("key", "test-key"))
                .andExpect(queryParam("appid", "5536570"))
                .andExpect(queryParam("ticket", "deadbeef"))
                .andExpect(queryParam("identity", "mrak-telemetry"))
                .andRespond(withSuccess("""
                        {"response":{"params":{"result":"OK","steamid":"76561198000000000",
                        "ownersteamid":"76561198000000000","vacbanned":false,"publisherbanned":false}}}
                        """, MediaType.APPLICATION_JSON));

        assertEquals(STEAM_ID, client.authenticateTicket("deadbeef"));
        server.verify();
    }

    @Test
    void testRejectTicketOnSteamError() {
        server.expect(requestTo(startsWith(BASE_URL + "/ISteamUserAuth/AuthenticateUserTicket/v1/")))
                .andRespond(withSuccess("""
                        {"response":{"error":{"errorcode":101,"errordesc":"Invalid ticket"}}}
                        """, MediaType.APPLICATION_JSON));

        assertThrows(SteamTicketRejectedException.class, () -> client.authenticateTicket("deadbeef"));
        server.verify();
    }

    @Test
    void testRejectTicketWithoutOkResult() {
        server.expect(requestTo(startsWith(BASE_URL + "/ISteamUserAuth/AuthenticateUserTicket/v1/")))
                .andRespond(withSuccess("""
                        {"response":{"params":{"result":"Denied"}}}
                        """, MediaType.APPLICATION_JSON));

        assertThrows(SteamTicketRejectedException.class, () -> client.authenticateTicket("deadbeef"));
    }

    @Test
    void testUnavailableOnInvalidSteamId() {
        server.expect(requestTo(startsWith(BASE_URL + "/ISteamUserAuth/AuthenticateUserTicket/v1/")))
                .andRespond(withSuccess("""
                        {"response":{"params":{"result":"OK","steamid":"not-a-number"}}}
                        """, MediaType.APPLICATION_JSON));

        assertThrows(SteamUnavailableException.class, () -> client.authenticateTicket("deadbeef"));
    }

    @Test
    void testUnavailableOnHttpError() {
        server.expect(requestTo(startsWith(BASE_URL + "/ISteamUserAuth/AuthenticateUserTicket/v1/")))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThrows(SteamUnavailableException.class, () -> client.authenticateTicket("deadbeef"));
    }

    @Test
    void testUnavailableWithoutPublisherKey() {
        SteamAuthProperties properties = new SteamAuthProperties();
        properties.setPublisherKey("");
        SteamWebApiClient keylessClient = new SteamWebApiClient(RestClient.builder().build(), properties);

        assertThrows(SteamUnavailableException.class, () -> keylessClient.authenticateTicket("deadbeef"));
    }

    @Test
    void testOwnsApp() {
        server.expect(requestTo(startsWith(BASE_URL + "/ISteamUser/CheckAppOwnership/v4/")))
                .andExpect(queryParam("key", "test-key"))
                .andExpect(queryParam("steamid", String.valueOf(STEAM_ID)))
                .andExpect(queryParam("appid", "5536570"))
                .andRespond(withSuccess("""
                        {"appownership":{"ownsapp":true,"result":"OK"}}
                        """, MediaType.APPLICATION_JSON));

        assertTrue(client.ownsApp(STEAM_ID));
        server.verify();
    }

    @Test
    void testDoesNotOwnApp() {
        server.expect(requestTo(startsWith(BASE_URL + "/ISteamUser/CheckAppOwnership/v4/")))
                .andRespond(withSuccess("""
                        {"appownership":{"ownsapp":false,"result":"OK"}}
                        """, MediaType.APPLICATION_JSON));

        assertFalse(client.ownsApp(STEAM_ID));
    }

    @Test
    void testOwnershipCheckUnavailableOnUnexpectedPayload() {
        server.expect(requestTo(startsWith(BASE_URL + "/ISteamUser/CheckAppOwnership/v4/")))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThrows(SteamUnavailableException.class, () -> client.ownsApp(STEAM_ID));
    }

    @Test
    void testFetchPlayerName() {
        server.expect(requestTo(startsWith(BASE_URL + "/ISteamUser/GetPlayerSummaries/v2/")))
                .andExpect(queryParam("steamids", String.valueOf(STEAM_ID)))
                .andRespond(withSuccess("""
                        {"response":{"players":[{"steamid":"76561198000000000","personaname":"  PlayerOne  "}]}}
                        """, MediaType.APPLICATION_JSON));

        assertEquals(Optional.of("PlayerOne"), client.fetchPlayerName(STEAM_ID));
    }

    @Test
    void testFetchPlayerNameReturnsEmptyWithoutPlayers() {
        server.expect(requestTo(startsWith(BASE_URL + "/ISteamUser/GetPlayerSummaries/v2/")))
                .andRespond(withSuccess("""
                        {"response":{"players":[]}}
                        """, MediaType.APPLICATION_JSON));

        assertEquals(Optional.empty(), client.fetchPlayerName(STEAM_ID));
    }

    @Test
    void testFetchPlayerNameIsBestEffort() {
        server.expect(requestTo(startsWith(BASE_URL + "/ISteamUser/GetPlayerSummaries/v2/")))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertEquals(Optional.empty(), client.fetchPlayerName(STEAM_ID));
    }
}
