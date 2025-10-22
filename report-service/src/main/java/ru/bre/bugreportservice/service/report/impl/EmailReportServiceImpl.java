package ru.bre.bugreportservice.service.report.impl;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import ru.bre.bugreportservice.dto.ReportEntity;
import ru.bre.bugreportservice.exception.ReportException;
import ru.bre.bugreportservice.service.report.ReportService;
import ru.bre.bugreportservice.util.ReportFileUtil;

import java.io.File;
import java.util.Date;


@Service
public class EmailReportServiceImpl implements ReportService {

    private final String from;
    private final String to;
    private final JavaMailSender mailSender;

    @Autowired
    public EmailReportServiceImpl(@Value("${mail.from}") String from, @Value("${mail.from}") String to, JavaMailSender mailSender) {
        this.from = from;
        this.to = to;
        this.mailSender = mailSender;
    }

    @Override
    public void report(ReportEntity reportEntity) {
       try {
           final MimeMessage mimeMessage = this.mailSender.createMimeMessage();
           final MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage, true);

           messageHelper.setFrom(from);
           messageHelper.setTo(to);
           messageHelper.setSubject(reportEntity.title() + " BUG REPORT " + new Date());
           messageHelper.setText(reportEntity.text());

           File imageFile = ReportFileUtil.convertMultipartFileToFile(reportEntity.imageFile());
           if (imageFile != null) {
               messageHelper.addAttachment("screenshot.png", imageFile);
           }

           File logFile = ReportFileUtil.convertMultipartFileToFile(reportEntity.logFile());
           if (logFile != null) {
               messageHelper.addAttachment("logs.log", logFile);
           }

           this.mailSender.send(mimeMessage);
       } catch (MessagingException e) {
           throw new ReportException(e);
       }
    }
}
