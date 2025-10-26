package ru.bre.storage.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class ReportEntity extends FeedbackEntity {
    protected MultipartFile imageFile;
    protected MultipartFile logFile;

    public ReportEntity(String title, String text, MultipartFile imageFile, MultipartFile logFile) {
        super(title, text);
        this.imageFile = imageFile;
        this.logFile = logFile;
    }
}
