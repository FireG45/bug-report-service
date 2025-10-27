package ru.bre.storage.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FeedbackDto(
        @JsonProperty("title") String title,
        @JsonProperty("text") String text
) {
}
