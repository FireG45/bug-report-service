package ru.bre.storage.service.summary.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.bre.kafka.KafkaProducer;
import ru.bre.kafka.message.SummaryMessage;
import ru.bre.storage.entity.Feedback;
import ru.bre.storage.service.summary.SummaryService;
import ru.bre.storage.service.summary.handler.LLMResponseHandler;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class YaGptApiSummaryServiceImpl implements SummaryService {

    private static final Logger log = LoggerFactory.getLogger(YaGptApiSummaryServiceImpl.class);

    private final HttpClient http = HttpClient.newHttpClient();

    private final String apiUrl;
    private final String apiKey;
    private final String apiSecret;
    private final ObjectMapper mapper;
    private final KafkaProducer producer;
    private final LLMResponseHandler responseHandler;

    @Autowired
    public YaGptApiSummaryServiceImpl(
            @Value("${llm.api.url}") String apiUrl,
            @Value("${llm.api.key}") String apiKey,
            @Value("${llm.api.secret}") String apiSecret,
            ObjectMapper mapper,
            KafkaProducer producer, LLMResponseHandler responseHandler
    ) {
        this.producer = producer;
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.mapper = mapper;
        this.responseHandler = responseHandler;
    }

    @Override
    public void createSummary(List<Feedback> feedbackList) throws IOException, InterruptedException {
        String combinedFeedback = feedbackList.stream()
                .map(Feedback::toString)
                .collect(Collectors.joining(", "));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .header("Authorization", "Api-Key " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(
                        """
                                {
                                    "modelUri": "gpt://%s/yandexgpt-lite",
                                    "completionOptions": {
                                        "stream": false,
                                        "temperature": 0.6,
                                        "maxTokens": "5000"
                                    },
                                    "messages": [
                                        {
                                            "role": "user",
                                            "text": "Summarize the following user feedback in Russian in a concise but meaningful paragraph. Keep technical and emotional aspects if relevant.Feedback list: %s"
                                        }
                                    ]
                                }
                                """.formatted(apiSecret, combinedFeedback)
                ))
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

        String responseBody = responseHandler.handleResponse(response.statusCode(), response.body(), mapper);

        JsonNode root = mapper.readTree(responseBody);

        String summary = root.path("result")
                .path("alternatives")
                .get(0)
                .path("message")
                .path("text")
                .asText();

        producer.sendSummaryMessage(new SummaryMessage("SUMMARY", summary, new Date()));
    }
}
