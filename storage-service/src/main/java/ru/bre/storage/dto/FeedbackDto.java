package ru.bre.storage.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FeedbackDto(
        @JsonProperty("id") Integer id,
        @JsonProperty("title") String title,
        @JsonProperty("text") String text
) {
    public FeedbackDto(String title, String text) {
        this(null, title, text);
    }
}
