# Система баг-репортов и отзывов для [!mrak]([https://github.com](https://store.steampowered.com/app/3794260/mrak_Demo))

Микросервисная платформа для сбора баг-репортов, пользовательских отзывов и генерации сводок с помощью AI. Используются Kafka для сообщений, MinIO для файлов и PostgreSQL для хранения данных.

## Архитектура

[![image.png](https://s.iimg.su/s/27/gvD7iDnxIZNuiBXNDRfZpczPkUEzD66RfG1pKNF9.png)](https://iimg.su/i/vD7iDn)

Компоненты:

### 1. **Report Service**

* **Эндпоинты**:

  * `POST /report-send` – принимает баг-репорты с изображениями и логами.
  * `POST /feedback-send` – принимает текстовые отзывы.
* **Функции**:

  * Загружает файлы в **MinIO**.
  * Отправляет сообщения с метаданными или текстом в Kafka:

    * `bre.report.message`
    * `bre.feedback.message`
* **Особенности**:

  * **Фоллбек-сервис**: если один из `FeedbackReportService` падает, автоматически пробует следующий в списке. Если все падают — возвращается ошибка.
  * **Лимит запросов**: реализован через Bucket/RateLimiter (2 запроса в минуту). При превышении возвращается `429 Too Many Requests`.

### 2. **Kafka**

* Основной брокер сообщений.
* Используемые топики:

  * `bre.report.message`
  * `bre.feedback.message`
  * `bre.summary.message`

### 3. **Storage Service**

* Читает все топики Kafka.
* Сохраняет данные и метаданные в **PostgreSQL**.
* Предоставляет REST API для чтения данных.
* Отдаёт ссылки на файлы в **MinIO**.

### 4. **Summary Service**

* Читает отзывы из базы.
* Генерирует сводки с помощью **JLLM**.
* Отправляет результаты в Kafka для хранения storage-service.

### 5. **MinIO**

* Хранит файлы логов и скриншоты.
* Предоставляет ссылки для фронтенда и storage-service.

### 6. **Frontend / Dashboard**

* Читает данные через API storage-service.
* Отображает баг-репорты, отзывы и сводки.
* Предоставляет ссылки на файлы в MinIO.

---

## Workflow

1. Фронтенд или игра отправляет баг-репорт или отзыв.
2. **Report Service**:

   * Загружает файлы в MinIO (если есть).
   * Отправляет сообщение в Kafka.
3. **Storage Service**:

   * Сохраняет метаданные и тексты в базу.
   * Предоставляет REST API.
4. **Summary Service**:

   * Генерирует AI-сводки.
   * Отправляет их в Kafka.
5. **Frontend / Dashboard**:

   * Получает данные через API.
   * Отображает отчёты, отзывы и сводки.

---

## Технологии

* **Backend**: Spring Boot (Java)
* **Messaging**: Apache Kafka
* **Файлы**: MinIO (S3-совместимый)
* **База данных**: PostgreSQL
* **AI-сводки**: JLLM
* **Контейнеризация**: Docker Compose

