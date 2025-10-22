package ru.bre.bugreportservice.dto.message;

public record BugReportMessageDto(String title, String text, String imageFile, String logFile) {
}
