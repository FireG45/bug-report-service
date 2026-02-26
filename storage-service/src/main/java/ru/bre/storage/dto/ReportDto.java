package ru.bre.storage.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ReportDto(
        @JsonProperty("id") Integer id,
        @JsonProperty("title") String title,
        @JsonProperty("text") String text,
        @JsonProperty("imageFile") String imageFile,
        @JsonProperty("logFile") String logFile
) {
    public ReportDto(String title, String text, String imageFile, String logFile) {
        this(null, title, text, imageFile, logFile);
    }
}
