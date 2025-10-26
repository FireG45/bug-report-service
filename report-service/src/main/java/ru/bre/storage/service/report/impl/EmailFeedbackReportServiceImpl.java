package ru.bre.storage.service.report.impl;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import ru.bre.storage.dto.FeedbackEntity;
import ru.bre.storage.dto.ReportEntity;
import ru.bre.storage.exception.ReportException;
import ru.bre.storage.service.report.FeedbackReportService;
import ru.bre.storage.util.ReportFileUtil;

import java.io.File;
import java.util.Date;


@Service
public class EmailFeedbackReportServiceImpl implements FeedbackReportService {

    private final String from;
    private final String to;
    private final JavaMailSender mailSender;

    @Autowired
    public EmailFeedbackReportServiceImpl(@Value("${mail.from}") String from, @Value("${mail.from}") String to, JavaMailSender mailSender) {
        this.from = from;
        this.to = to;
        this.mailSender = mailSender;
    }

    @Override
    public void report(ReportEntity reportEntity) {
       try {
           final MimeMessage mimeMessage = this.mailSender.createMimeMessage();
           final MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage, true);

           enrichTextPart(reportEntity, messageHelper);
           enrichWithFiles(reportEntity, messageHelper);

           this.mailSender.send(mimeMessage);
       } catch (MessagingException e) {
           throw new ReportException(e);
       }
    }

    @Override
    public void feedback(FeedbackEntity feedbackEntity) throws ReportException {
        try {
            final MimeMessage mimeMessage = this.mailSender.createMimeMessage();
            final MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage, true);

            enrichTextPart(feedbackEntity, messageHelper);

            this.mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            throw new ReportException(e);
        }
    }

    private void enrichWithFiles(ReportEntity reportEntity, MimeMessageHelper messageHelper) throws MessagingException {
        File imageFile = ReportFileUtil.convertMultipartFileToFile(reportEntity.getImageFile());
        if (imageFile != null) {
            messageHelper.addAttachment("screenshot.png", imageFile);
        }

        File logFile = ReportFileUtil.convertMultipartFileToFile(reportEntity.getLogFile());
        if (logFile != null) {
            messageHelper.addAttachment("logs.log", logFile);
        }
    }

    private void enrichTextPart(FeedbackEntity reportEntity, MimeMessageHelper messageHelper) throws MessagingException {
        messageHelper.setFrom(from);
        messageHelper.setTo(to);
        messageHelper.setSubject(reportEntity.getTitle() + " BUG REPORT " + new Date());
        messageHelper.setText(reportEntity.getText());
    }

}
