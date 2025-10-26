package ru.bre.storage.storage.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.bre.storage.storage.dto.FeedbackDto;
import ru.bre.storage.storage.dto.ReportDto;
import ru.bre.storage.storage.dto.SummaryDto;
import ru.bre.storage.storage.repo.StorageRepository;
import ru.bre.storage.storage.service.StorageService;

@Service
public class StorageServiceImpl implements StorageService {

    private final StorageRepository storageRepository;

    @Autowired
    public StorageServiceImpl(StorageRepository storageRepository) {
        this.storageRepository = storageRepository;
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
}
