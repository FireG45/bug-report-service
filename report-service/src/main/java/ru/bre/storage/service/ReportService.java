package ru.bre.storage.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.bre.storage.dto.ReportEntity;
import ru.bre.storage.exception.ReportException;
import ru.bre.storage.util.ReportFileUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class ReportService {

    private final String from;
    private final JavaMailSender mailSender;

    public ReportService(@Value("${mail.from}") String from, JavaMailSender mailSender) {
        this.from = from;
        this.mailSender = mailSender;
    }

    public void report(ReportEntity reportEntity, UUID playerId) {
        List<File> tempFiles = new ArrayList<>();
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage, true);

            enrichTextPart(reportEntity, playerId, messageHelper);
            addAttachment(messageHelper, reportEntity.getImageFile(), "screenshot.png", tempFiles);
            addAttachment(messageHelper, reportEntity.getLogFile(), "logs.log", tempFiles);

            mailSender.send(mimeMessage);
        } catch (MessagingException | IOException e) {
            throw new ReportException(e);
        } finally {
            tempFiles.forEach(File::delete);
        }
    }

    private void addAttachment(MimeMessageHelper messageHelper,
                               MultipartFile file,
                               String attachmentName,
                               List<File> tempFiles) throws MessagingException, IOException {
        if (file == null || file.isEmpty()) {
            return;
        }
        File tempFile = ReportFileUtil.copyToTempFile(file);
        tempFiles.add(tempFile);
        messageHelper.addAttachment(attachmentName, tempFile);
    }

    private void enrichTextPart(ReportEntity reportEntity, UUID playerId, MimeMessageHelper messageHelper)
            throws MessagingException {
        messageHelper.setFrom(from);
        messageHelper.setTo(from);
        String subject = reportEntity.getTitle() + " BUG REPORT " + new Date();
        if (playerId != null) {
            subject = "[player:" + playerId + "] " + subject;
        }
        messageHelper.setSubject(sanitizeHeader(subject));
        messageHelper.setText(reportEntity.getText());
    }

    private String sanitizeHeader(String value) {
        return value.replaceAll("\\R", " ");
    }
}
