# Система баг-репортов для [!mrak](https://store.steampowered.com/app/3794260/mrak_Demo)

Сервис приёма баг-репортов. Отчёты отправляются на почту, скриншоты и логи прикладываются к письму.

## Возможности

* `POST /report-send` — баг-репорт: `title`, `text`, `imageFile` и опциональный `logFile`.
* `POST /telemetry/auth/steam` — обмен Steam-билета на Bearer-токен: проверка билета
  (`AuthenticateUserTicket`) и лицензии Playtest (`CheckAppOwnership`) через Steam Web API.
  Токен живёт 1 час, в БД хранится только его SHA-256 хеш.
* `POST /telemetry/batches` — приём пачки событий с Bearer-токеном, дедупликация по `event_id`,
  к событиям добавляется `player_id` аккаунта.
* Ограничение частоты запросов (Bucket4j): 3 запроса в минуту с одного IP, при превышении — `429`.
* Feature-toggle `frontend-report`: `GET /get-frontend-report`, `POST /set-frontend-report` (по секрету).
* `GET /healthcheck` — проверка живости сервиса.

## Локальный запуск

Для полного запуска нужны Docker с Compose и соседний каталог `mrak-main-page`: `docker-compose.yml`
собирает веб-интерфейс из `../mrak-main-page`.

1. Склонируйте этот репозиторий и [`mrak-main-page`](https://github.com/FireG45/mrak-main-page) в одну папку.
2. В корне этого репозитория скопируйте `.env.example` в `.env` и заполните `EMAIL_USERNAME`,
   `EMAIL_PASSWORD` (SMTP Яндекса), `SERVER_SECRET` и `STEAM_PUBLISHER_KEY` (Publisher Web API Key
   с доступом к AppID плейтеста; хранится только на сервере). Без ключа обмен билетов отвечает `503`.
3. Запустите `docker compose up --build` из корня репозитория.

Report-service доступен на `http://localhost:8080`, фронтенд — на `http://localhost:3000`.

## Архитектура

Фронтенд → `report-service` → SMTP-письмо с вложениями.

### Report Service

Spring Boot приложение:

* принимает баг-репорты;
* отправляет письмо через `smtp.yandex.ru` (JavaMail), прикладывая скриншот и лог;
* принимает телеметрию Playtest в PostgreSQL: обменивает Steam-билет на токен,
  проверяет лицензию аккаунта и пишет события с внутренним `player_id`;
* ограничивает частоту запросов и отдаёт healthcheck.

## Технологии

* **Backend**: Spring Boot (Java 17), Spring Mail
* **Rate limiting**: Bucket4j + Caffeine
* **Контейнеризация**: Docker Compose
