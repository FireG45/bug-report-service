package ru.bre.storage.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
@AllArgsConstructor
public class ReportEntity {
    private String title;
    private String text;
    private MultipartFile imageFile;
    private MultipartFile logFile;
}
