package ru.bre.storage.storage.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ReportDto(
        @JsonProperty("title") String title,
        @JsonProperty("text") String text,
        @JsonProperty("imageFile") String imageFile,
        @JsonProperty("logFile") String logFile
) {
}
