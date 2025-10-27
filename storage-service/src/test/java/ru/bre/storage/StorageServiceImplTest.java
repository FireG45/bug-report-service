package ru.bre.storage;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.boot.test.context.SpringBootTest;
import ru.bre.storage.dto.FeedbackDto;
import ru.bre.storage.dto.ReportDto;
import ru.bre.storage.dto.SummaryDto;
import ru.bre.storage.repo.StorageRepository;
import ru.bre.storage.service.impl.StorageServiceImpl;

import java.util.Date;
import java.util.List;
import java.util.Arrays;

class StorageServiceImplTest {

    @Mock
    private StorageRepository storageRepository;

    private StorageServiceImpl storageService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        storageService = new StorageServiceImpl(storageRepository);
    }

    @Test
    void testSaveReport() {
        ReportDto report = new ReportDto("Title", "Text", "image.png", "log.txt");

        storageService.save(report);

        verify(storageRepository, times(1)).save(report);
    }

    @Test
    void testSaveSummary() {
        SummaryDto summary = new SummaryDto("Title", "Text", new Date());

        storageService.save(summary);

        verify(storageRepository, times(1)).save(summary);
    }

    @Test
    void testSaveFeedback() {
        FeedbackDto feedback = new FeedbackDto("Title", "Text");

        storageService.save(feedback);

        verify(storageRepository, times(1)).save(feedback);
    }

    @Test
    void testGetReports() {
        List<ReportDto> mockReports = Arrays.asList(
                new ReportDto("R1", "Text1", "img1.png", "log1.txt"),
                new ReportDto("R2", "Text2", "img2.png", "log2.txt")
        );

        when(storageRepository.getReports(0, 2)).thenReturn(mockReports);

        List<ReportDto> result = storageService.getReports(0, 2);

        assertEquals(2, result.size());
        assertEquals("R1", result.get(0).title());
        verify(storageRepository, times(1)).getReports(0, 2);
    }

    @Test
    void testGetFeedback() {
        List<FeedbackDto> mockFeedback = List.of(
                new FeedbackDto("F1", "Text1")
        );

        when(storageRepository.getFeedback(0, 1)).thenReturn(mockFeedback);

        List<FeedbackDto> result = storageService.getFeedback(0, 1);

        assertEquals(1, result.size());
        assertEquals("F1", result.get(0).title());
        verify(storageRepository, times(1)).getFeedback(0, 1);
    }

    @Test
    void testGetSummary() {
        List<SummaryDto> mockSummary = List.of(
                new SummaryDto("S1", "Text1", new Date())
        );

        when(storageRepository.getSummary(0, 1)).thenReturn(mockSummary);

        List<SummaryDto> result = storageService.getSummary(0, 1);

        assertEquals(1, result.size());
        assertEquals("S1", result.get(0).title());
        verify(storageRepository, times(1)).getSummary(0, 1);
    }
}
