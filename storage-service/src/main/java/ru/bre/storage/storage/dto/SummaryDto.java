package ru.bre.storage.storage.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;

public record SummaryDto(
        @JsonProperty("title") String title,
        @JsonProperty("text") String text,
        @JsonProperty("date") Date date
) {
}
