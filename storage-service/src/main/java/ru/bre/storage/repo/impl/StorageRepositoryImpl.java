package ru.bre.storage.repo.impl;

import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ru.bre.storage.jooq.tables.records.FeedbackRecord;
import ru.bre.storage.jooq.tables.records.ReportRecord;
import ru.bre.storage.jooq.tables.records.SummaryRecord;
import ru.bre.storage.dto.FeedbackDto;
import ru.bre.storage.dto.ReportDto;
import ru.bre.storage.dto.SummaryDto;
import ru.bre.storage.repo.StorageRepository;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

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
    public int deleteReports() {
        return dsl.deleteFrom(REPORT).execute();
    }

    @Override
    public int deleteFeedbacks() {
        return dsl.deleteFrom(FEEDBACK).execute();
    }

    @Override
    public int deleteSummaries() {
        return dsl.deleteFrom(SUMMARY).execute();
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

    @Override
    public List<ReportDto> getReports(int offset, int limit) {
        return dsl.selectFrom(REPORT).offset(offset).limit(limit).fetch()
                .map(record -> new ReportDto(
                        record.getTitle(),
                        record.getText(),
                        record.getImageFile(),
                        record.getLogFile()
                ));
    }

    @Override
    public List<FeedbackDto> getFeedback(int offset, int limit) {
        return dsl.selectFrom(FEEDBACK).offset(offset).limit(limit).fetch()
                .map(record -> new FeedbackDto(
                        record.getTitle(),
                        record.getText()
                ));
    }

    @Override
    public List<SummaryDto> getSummary(int offset, int limit) {
        return dsl.selectFrom(SUMMARY).offset(offset).limit(limit).fetch()
                .map(record -> new SummaryDto(
                        record.getTitle(),
                        record.getText(),
                        Date.from(record.getDate().atZone(ZoneId.systemDefault()).toInstant())
                ));
    }
}
