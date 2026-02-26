package ru.bre.storage.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;

public record SummaryDto(
        @JsonProperty("id") Integer id,
        @JsonProperty("title") String title,
        @JsonProperty("text") String text,
        @JsonProperty("date") Date date
) {
    public SummaryDto(String title, String text, Date date) {
        this(null, title, text, date);
    }
}
