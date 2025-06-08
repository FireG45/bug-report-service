package ru.bre.bugreportservice.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.bre.bugreportservice.dto.ReportDto;
import ru.bre.bugreportservice.util.ReportFileUtil;

import java.io.File;
import java.util.Date;


@Service
public class EmailReportServiceImpl implements ReportService {

    private final String from;
    private final String to;
    private final JavaMailSender mailSender;

    private final String SUBJECT = " !mrak BUG REPORT ";

    @Autowired
    public EmailReportServiceImpl(@Value("${mail.from}") String from, @Value("${mail.from}") String to, JavaMailSender mailSender) {
        this.from = from;
        this.to = to;
        this.mailSender = mailSender;
    }

    @Override
    public void report(ReportDto reportDto, MultipartFile imageMFile, MultipartFile logMFile) throws MessagingException {
        final MimeMessage mimeMessage = this.mailSender.createMimeMessage();
        final MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage, true);

        messageHelper.setFrom(from);
        messageHelper.setTo(to);
        messageHelper.setSubject(reportDto.title() + SUBJECT + new Date());
        messageHelper.setText(reportDto.text());

        File imageFile = ReportFileUtil.convertMultipartFileToFile(imageMFile);
        if (imageFile != null) {
            messageHelper.addAttachment("screenshot.png", imageFile);
        }

        File logFile = ReportFileUtil.convertMultipartFileToFile(logMFile);
        if (logFile != null) {
            messageHelper.addAttachment("logs.log", logFile);
        }

        this.mailSender.send(mimeMessage);
    }
}
