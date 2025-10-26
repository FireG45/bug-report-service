package ru.bre.storage.storage.repo.impl;

import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ru.bre.storage.jooq.tables.Report;
import ru.bre.storage.jooq.tables.records.FeedbackRecord;
import ru.bre.storage.jooq.tables.records.ReportRecord;
import ru.bre.storage.jooq.tables.records.SummaryRecord;
import ru.bre.storage.storage.dto.FeedbackDto;
import ru.bre.storage.storage.dto.ReportDto;
import ru.bre.storage.storage.dto.SummaryDto;
import ru.bre.storage.storage.repo.StorageRepository;

import java.time.LocalDateTime;
import java.time.ZoneId;

import static ru.bre.storage.jooq.Tables.FEEDBACK;
import static ru.bre.storage.jooq.Tables.REPORT;
import static ru.bre.storage.jooq.Tables.SUMMARY;

@Repository
public class StorageRepositoryImpl implements StorageRepository {

    private final DSLContext dsl;

    @Autowired
    public StorageRepositoryImpl(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    public void save(ReportDto report) {
        ReportRecord record = new ReportRecord();
        record.setTitle(report.title());
        record.setText(report.text());
        record.setImageFile(report.imageFile());
        record.setLogFile(report.logFile());

        dsl.insertInto(REPORT).set(record).execute();
    }

    @Override
    public void save(SummaryDto summary) {
        SummaryRecord record = new SummaryRecord();
        record.setTitle(summary.title());
        record.setText(summary.text());
        record.setDate(LocalDateTime.ofInstant(summary.date().toInstant(), ZoneId.systemDefault()));

        dsl.insertInto(SUMMARY).set(record).execute();
    }

    @Override
    public void save(FeedbackDto feedback) {
        FeedbackRecord record = new FeedbackRecord();
        record.setTitle(feedback.title());
        record.setText(feedback.text());

        dsl.insertInto(FEEDBACK).set(record).execute();
    }
}
