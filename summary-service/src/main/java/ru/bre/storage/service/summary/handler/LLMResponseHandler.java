package ru.bre.storage.service.summary.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import ru.bre.storage.service.summary.exception.LLMException;

@Service
public class LLMResponseHandler {

    public String handleResponse(int status, String body, ObjectMapper mapper) {
        if (status >= 200 && status < 300) {
            return body;
        }

        String errorMsg = parseError(body, mapper);
        String message = switch (status) {
            case 400 -> "Bad request: " + errorMsg;
            case 401 -> "Unauthorized: invalid API key or credentials. " + errorMsg;
            case 402 -> "Payment required or usage limit exceeded. " + errorMsg;
            case 403 -> "Forbidden: insufficient permissions. " + errorMsg;
            case 404 -> "Resource not found: " + errorMsg;
            case 408 -> "Request timeout: " + errorMsg;
            case 429 -> "Too many requests: rate limit exceeded. " + errorMsg;
            case 500, 502, 503, 504 -> "Server error (" + status + "): " + errorMsg;
            default -> "Unexpected response (" + status + "): " + errorMsg;
        };

        throw new LLMException(message, status);
    }

    private String parseError(String body, ObjectMapper mapper) {
        try {
            JsonNode node = mapper.readTree(body);
            if (node.has("error")) {
                JsonNode err = node.path("error");
                String code = err.path("code").asText("");
                String message = err.path("message").asText("");
                return (code + " " + message).trim();
            } else if (node.has("message")) {
                return node.path("message").asText();
            }
        } catch (Exception ignored) {}
        return (body != null && !body.isBlank()) ? body : "Unknown error";
    }
}
