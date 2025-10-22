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
}
