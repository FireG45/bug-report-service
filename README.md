# Система баг-репортов для [!mrak](https://store.steampowered.com/app/3794260/mrak_Demo)

Сервис приёма баг-репортов. Отчёты отправляются на почту, скриншоты и логи прикладываются к письму.

## Возможности

* `POST /report-send` — баг-репорт: `title`, `text`, `imageFile` и опциональный `logFile`.
  Требует `Authorization: Bearer` с токеном из `/telemetry/auth/steam`; без него — `401`,
  при превышении лимита на аккаунт (по умолчанию 40 отчётов в час) — `429`. Анонимный приём
  можно временно вернуть через `REPORT_REQUIRE_AUTH=false` (по умолчанию `true`).
* `POST /telemetry/auth/steam` — обмен Steam-билета на Bearer-токен: проверка билета
  (`AuthenticateUserTicket`) и лицензии Playtest (`CheckAppOwnership`) через Steam Web API.
  Токен живёт 1 час, в БД хранится только его SHA-256 хеш.
* `POST /telemetry/batches` — приём пачки событий с Bearer-токеном, дедупликация по `event_id`,
  к событиям добавляется `player_id` аккаунта.
* Ограничение частоты запросов (Bucket4j): 3 запроса в минуту с одного IP, при превышении — `429`.
* `GET /get-frontend-report` — всегда возвращает `false` (feature-toggle удалён).
* `GET /healthcheck` — проверка живости сервиса.

## Локальный запуск

Для полного запуска нужны Docker с Compose и соседний каталог `mrak-main-page`: `docker-compose.yml`
собирает веб-интерфейс из `../mrak-main-page`.

1. Склонируйте этот репозиторий и [`mrak-main-page`](https://github.com/FireG45/mrak-main-page) в одну папку.
2. В корне этого репозитория скопируйте `.env.example` в `.env` и заполните `EMAIL_USERNAME`,
   `EMAIL_PASSWORD` (SMTP Яндекса) и `STEAM_PUBLISHER_KEY` (Publisher Web API Key
   с доступом к AppID плейтеста; хранится только на сервере). Без ключа обмен билетов отвечает `503`.
   Для отчётов без Steam-токена (локальная отладка формы) выставьте `REPORT_REQUIRE_AUTH=false`,
   лимит на аккаунт задаётся `REPORT_ACCOUNT_RATE_LIMIT_PER_HOUR`.
3. Запустите `docker compose up --build` из корня репозитория.

Report-service доступен на `http://localhost:8080`, фронтенд — на `http://localhost:3000`.

Для локальной разработки профиль `local` разрешает CORS с `http://localhost:3000`
(`SPRING_PROFILES_ACTIVE=local`); в проде origin задаётся через `CORS_ALLOWED_ORIGINS`.

## Мониторинг

`monitoring/healthcheck.sh` — долгоживущий процесс на хосте: проверяет readiness,
ключевые URL, состояние контейнеров, диск, свежесть бэкапа БД и всплески 5xx/429/401
в access-логе nginx, шлёт алерты в Telegram (`TELEGRAM_BOT_TOKEN`, `TELEGRAM_CHAT_ID`
из `.env`; `TELEGRAM_API_IP` — опциональный резолв api.telegram.org мимо DNS).
Запуск через `monitoring/bug-report-monitor.service`.

`monitoring/fail2ban/mrak-report-limit.*` — jail, который банит IP за повторные
срабатывания `limit_req` nginx только в зоне `report` (`/api/report-send`), не задевая
телеметрию игроков за общим NAT.

## Архитектура

Godot-клиент → `report-service` → SMTP-письмо с вложениями. Браузерная форма багрепорта
оставлена для совместимости, но отключена (`/get-frontend-report` всегда `false`).

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
