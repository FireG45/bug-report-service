package ru.bre.storage.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import ru.bre.storage.dto.SteamAuthRequest;
import ru.bre.storage.exception.PlaytestAccessDeniedException;
import ru.bre.storage.exception.SteamTicketRejectedException;
import ru.bre.storage.exception.SteamUnavailableException;
import ru.bre.storage.service.SteamAuthService;

import java.util.Map;
import java.util.regex.Pattern;

@RestController
public class SteamAuthController {

    private static final int MAX_TICKET_LENGTH = 8192;
    private static final Pattern HEX_TICKET = Pattern.compile("[0-9a-fA-F]+");

    private final SteamAuthService steamAuthService;

    public SteamAuthController(SteamAuthService steamAuthService) {
        this.steamAuthService = steamAuthService;
    }

    @PostMapping("/telemetry/auth/steam")
    public ResponseEntity<?> authenticate(
            @RequestBody(required = false) SteamAuthRequest request
    ) {
        String ticket = request == null ? null : request.getTicket();
        if (!isValidTicket(ticket)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid ticket"));
        }
        return ResponseEntity.ok(steamAuthService.issueToken(ticket));
    }

    @ExceptionHandler(SteamTicketRejectedException.class)
    public ResponseEntity<Map<String, Object>> handleTicketRejected(SteamTicketRejectedException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(PlaytestAccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(PlaytestAccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(SteamUnavailableException.class)
    public ResponseEntity<Map<String, Object>> handleUnavailable(SteamUnavailableException e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", e.getMessage()));
    }

    private boolean isValidTicket(String ticket) {
        return ticket != null
                && ticket.length() >= 2
                && ticket.length() <= MAX_TICKET_LENGTH
                && ticket.length() % 2 == 0
                && HEX_TICKET.matcher(ticket).matches();
    }
}
