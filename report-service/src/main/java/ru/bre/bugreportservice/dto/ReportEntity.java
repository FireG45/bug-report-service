package ru.bre.bugreportservice.dto;

import org.springframework.web.multipart.MultipartFile;

public record ReportEntity(
        String title,
        String text,
        MultipartFile imageFile,
        MultipartFile logFile,
        String imageFileUrl,
        String logFileUrl
) {
    public ReportEntity(String title, String text) {
        this(title, text, null, null, null, null);
    }

    public ReportEntity(String title, String text, MultipartFile imageFile, MultipartFile logFile) {
        this(title, text, imageFile, logFile, null, null);
    }
}
