# Безопасность

## Как сообщить об уязвимости

Откройте GitHub Security Advisory или issue без деталей эксплуатации. Публичный репозиторий —
не место для работающих эксплойтов.

## Известные принятые риски

### CVE-2026-47884 и CVE-2026-47890 (spring-webmvc 6.2.19)

Статус: принято владельцем 2026-11-25.

Оба CVE касаются механизмов, которых в сервисе нет:

- CVE-2026-47884 — RCE через `XsltView`. Все контроллеры — `@RestController`, MVC-view
  и XSLT не используются.
- CVE-2026-47890 — повреждение SSE-потока с view. SSE/WebFlux-эндпоинтов нет.

Исправление есть только в Spring Framework 7.0.9 (Spring Boot 4.x); переход на Boot 4 —
мажорная миграция. Пересмотреть риск, если появятся MVC-вью, SSE/WebFlux или начнётся
миграция на Boot 4.

### Прочее (решение владельца, 2026-11-23)

- события телеметрии и PII хранятся без автоочистки;
- отзыв токенов — вручную через `auth_tokens.revoked_at` (токены живут не дольше часа);
- репозиторий публичный, секретов в коде нет.

## Сканирование

```bash
trivy image --scanners vuln --severity CRITICAL,HIGH bug-report-service-report-service:latest
trivy config --severity CRITICAL,HIGH .
```

Состояние на 2026-11-25: OS-пакеты образа — 0 Critical/High, misconfig Dockerfile — 0,
Java-зависимости — 0 High, 2 Critical из блока выше (Tomcat, Jackson и pgjdbc пропатчены
переопределением версий в `pom.xml`).
