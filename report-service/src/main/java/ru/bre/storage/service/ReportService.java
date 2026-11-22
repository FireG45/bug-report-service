package ru.bre.storage.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import ru.bre.storage.dto.ReportEntity;
import ru.bre.storage.exception.ReportException;
import ru.bre.storage.util.ReportFileUtil;

import java.io.File;
import java.util.Date;

@Service
public class ReportService {

    private final String from;
    private final JavaMailSender mailSender;

    public ReportService(@Value("${mail.from}") String from, JavaMailSender mailSender) {
        this.from = from;
        this.mailSender = mailSender;
    }

    public void report(ReportEntity reportEntity) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage, true);

            enrichTextPart(reportEntity, messageHelper);
            enrichWithFiles(reportEntity, messageHelper);

            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            throw new ReportException(e);
        }
    }

    private void enrichWithFiles(ReportEntity reportEntity, MimeMessageHelper messageHelper) throws MessagingException {
        if (reportEntity.getImageFile() != null) {
            File imageFile = ReportFileUtil.convertMultipartFileToFile(reportEntity.getImageFile());
            if (imageFile != null) {
                messageHelper.addAttachment("screenshot.png", imageFile);
            }
        }

        if (reportEntity.getLogFile() != null) {
            File logFile = ReportFileUtil.convertMultipartFileToFile(reportEntity.getLogFile());
            if (logFile != null) {
                messageHelper.addAttachment("logs.log", logFile);
            }
        }
    }

    private void enrichTextPart(ReportEntity reportEntity, MimeMessageHelper messageHelper) throws MessagingException {
        messageHelper.setFrom(from);
        messageHelper.setTo(from);
        messageHelper.setSubject(reportEntity.getTitle() + " BUG REPORT " + new Date());
        messageHelper.setText(reportEntity.getText());
    }
}
