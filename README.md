# Система баг-репортов для [!mrak](https://store.steampowered.com/app/3794260/mrak_Demo)

Сервис приёма баг-репортов. Отчёты отправляются на почту, скриншоты и логи прикладываются к письму.

## Возможности

* `POST /report-send` — баг-репорт: `title`, `text`, `imageFile` и опциональный `logFile`.
* Ограничение частоты запросов (Bucket4j): 3 запроса в минуту с одного IP, при превышении — `429`.
* Feature-toggle `frontend-report`: `GET /get-frontend-report`, `POST /set-frontend-report` (по секрету).
* `GET /healthcheck` — проверка живости сервиса.

## Локальный запуск

Для полного запуска нужны Docker с Compose и соседний каталог `mrak-main-page`: `docker-compose.yml`
собирает веб-интерфейс из `../mrak-main-page`.

1. Склонируйте этот репозиторий и [`mrak-main-page`](https://github.com/FireG45/mrak-main-page) в одну папку.
2. В корне этого репозитория скопируйте `.env.example` в `.env` и заполните `EMAIL_USERNAME`,
   `EMAIL_PASSWORD` (SMTP Яндекса) и `SERVER_SECRET`.
3. Запустите `docker compose up --build` из корня репозитория.

Report-service доступен на `http://localhost:8080`, фронтенд — на `http://localhost:3000`.

## Архитектура

Фронтенд → `report-service` → SMTP-письмо с вложениями.

### Report Service

Spring Boot приложение:

* принимает баг-репорты;
* отправляет письмо через `smtp.yandex.ru` (JavaMail), прикладывая скриншот и лог;
* ограничивает частоту запросов и отдаёт healthcheck.

## Технологии

* **Backend**: Spring Boot (Java 17), Spring Mail
* **Rate limiting**: Bucket4j + Caffeine
* **Контейнеризация**: Docker Compose
