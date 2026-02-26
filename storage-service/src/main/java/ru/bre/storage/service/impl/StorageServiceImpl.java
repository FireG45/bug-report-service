package ru.bre.storage.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.bre.storage.dto.FeedbackDto;
import ru.bre.storage.dto.ReportDto;
import ru.bre.storage.dto.SummaryDto;
import ru.bre.storage.repo.StorageRepository;
import ru.bre.storage.service.StorageService;

import java.util.List;

@Service
public class StorageServiceImpl implements StorageService {

    private final StorageRepository storageRepository;

    @Autowired
    public StorageServiceImpl(StorageRepository storageRepository) {
        this.storageRepository = storageRepository;
    }

    @Override
    public int deleteReports() {
        return storageRepository.deleteReports();
    }

    @Override
    public int deleteFeedbacks() {
        return storageRepository.deleteFeedbacks();
    }

    @Override
    public int deleteSummaries() {
        return storageRepository.deleteSummaries();
    }

    @Override
    public void save(ReportDto report) {
        storageRepository.save(report);
    }

    @Override
    public void save(SummaryDto summary) {
        storageRepository.save(summary);
    }

    @Override
    public void save(FeedbackDto feedback) {
        storageRepository.save(feedback);
    }

    @Override
    public List<ReportDto> getReports(int offset, int limit) {
        return storageRepository.getReports(offset, limit);
    }

    @Override
    public List<FeedbackDto> getFeedback(int offset, int limit) {
        return storageRepository.getFeedback(offset, limit);
    }

    @Override
    public List<SummaryDto> getSummary(int offset, int limit) {
        return storageRepository.getSummary(offset, limit);
    }

    @Override
    public int deleteReportById(int id) {
        return storageRepository.deleteReportById(id);
    }

    @Override
    public int deleteFeedbackById(int id) {
        return storageRepository.deleteFeedbackById(id);
    }

    @Override
    public int deleteSummaryById(int id) {
        return storageRepository.deleteSummaryById(id);
    }
}
