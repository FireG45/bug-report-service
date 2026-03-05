package ru.bre.healthcheck.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.bre.healthcheck.model.ServiceStatus;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledHealthcheckService {

    private final HealthcheckService healthcheckService;
    private final TelegramNotifier telegramNotifier;

    private final Map<String, String> previousStatuses = new ConcurrentHashMap<>();
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @PostConstruct
    public void onStartup() {
        int serviceCount = healthcheckService.getServices().size();
        log.info("Health Check Monitor started. Services: {}", serviceCount);
        telegramNotifier.send("<b>\uD83D\uDFE2 Health Monitor started</b>\nChecking " + serviceCount + " services");
    }

    @Scheduled(fixedDelayString = "${healthcheck.interval-ms:120000}")
    public void scheduledCheck() {
        List<ServiceStatus> statuses = healthcheckService.checkAll();
        String now = LocalDateTime.now().format(FORMATTER);

        for (ServiceStatus svc : statuses) {
            String name = svc.getName();
            String status = svc.getStatus();
            String prev = previousStatuses.getOrDefault(name, "up");

            if ("down".equals(status) && !"down".equals(prev)) {
                log.warn("ALERT: {} is DOWN (target: {})", name, svc.getTarget());
                telegramNotifier.send(
                        "<b>\uD83D\uDD34 " + name + " is DOWN</b>\n"
                                + "Target: <code>" + svc.getTarget() + "</code>\n"
                                + "Time: " + now
                );
            } else if ("up".equals(status) && "down".equals(prev)) {
                log.info("RECOVERED: {} is UP (target: {})", name, svc.getTarget());
                telegramNotifier.send(
                        "<b>\uD83D\uDFE2 " + name + " RECOVERED</b>\n"
                                + "Target: <code>" + svc.getTarget() + "</code>\n"
                                + "Time: " + now
                );
            }

            previousStatuses.put(name, status);
        }
    }
}
