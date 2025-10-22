package ru.bre.bugreportservice.service.report;

import ru.bre.bugreportservice.dto.ReportEntity;
import ru.bre.bugreportservice.exception.ReportException;

public interface ReportService {
    void report(ReportEntity reportEntity) throws ReportException;
}
