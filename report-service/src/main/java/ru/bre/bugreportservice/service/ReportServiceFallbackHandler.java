package ru.bre.bugreportservice.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.bre.bugreportservice.dto.ReportEntity;
import ru.bre.bugreportservice.exception.ReportException;
import ru.bre.bugreportservice.service.report.ReportService;

import java.util.List;

@Service
public class ReportServiceFallbackHandler {

    private final List<ReportService> reportServices;

    @Autowired
    public ReportServiceFallbackHandler(List<ReportService> reportServices) {
        this.reportServices = reportServices;
    }

    public void handle(ReportEntity reportEntity) {
        for (ReportService service : reportServices) {
            try {
                service.report(reportEntity);
                return;
            } catch (ReportException e) {
                System.err.println("ReportService failed: " + service.getClass().getSimpleName() + " -> " + e.getMessage());
            }
        }
        throw new RuntimeException("All report services failed");
    }
}

