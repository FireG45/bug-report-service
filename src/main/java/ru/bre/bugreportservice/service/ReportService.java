package ru.bre.bugreportservice.service;

import jakarta.mail.MessagingException;
import org.springframework.web.multipart.MultipartFile;
import ru.bre.bugreportservice.dto.ReportDto;

public interface ReportService {
    void report(ReportDto reportDto, MultipartFile imageMFile, MultipartFile logMFile) throws MessagingException;
}
