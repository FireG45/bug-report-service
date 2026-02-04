# Storage Service Admin App

Десктопное приложение-админка для Windows для работы с Storage Service.

## Возможности

- Подключение к storage-service по указанному хосту
- Просмотр Reports (отчетов)
- Просмотр Feedback (обратной связи)
- Просмотр Summary (сводок)
- Настройка пагинации (offset и limit)

## Требования

- Java 17 или выше
- Maven 3.6+

## Сборка

```bash
cd admin-app
mvn clean package
```

## Запуск

### Через Maven:
```bash
mvn javafx:run
```

### Через скомпилированный JAR:
```bash
java --module-path <путь-к-javafx-sdk>/lib --add-modules javafx.controls,javafx.fxml -jar target/admin-app-1.0.0.jar
```

### Создание исполняемого JAR с зависимостями:
После выполнения `mvn package` будет создан JAR файл в `target/admin-app-1.0.0.jar`

## Использование

1. Запустите приложение
2. Введите хост storage-service (например: `http://localhost:8080` или просто `localhost:8080`)
3. Нажмите "Подключиться"
4. Установите offset и limit для пагинации (по умолчанию: offset=0, limit=100)
5. Нажмите на одну из кнопок для получения данных:
   - "Получить Reports" - получить отчеты
   - "Получить Feedback" - получить обратную связь
   - "Получить Summary" - получить сводки

Данные отображаются в отдельных окнах с таблицами.

## Структура проекта

```
admin-app/
├── src/
│   └── main/
│       ├── java/
│       │   └── ru/
│       │       └── bre/
│       │           └── admin/
│       │               ├── AdminApp.java          # Главный класс приложения
│       │               ├── controller/
│       │               │   └── MainController.java # Контроллер главного окна
│       │               ├── model/
│       │               │   ├── ReportDto.java      # Модель Report
│       │               │   ├── FeedbackDto.java    # Модель Feedback
│       │               │   └── SummaryDto.java     # Модель Summary
│       │               └── service/
│       │                   └── StorageServiceClient.java # HTTP клиент
│       └── resources/
│           ├── main.fxml      # FXML разметка главного окна
│           └── application.css # Стили приложения
├── pom.xml
└── README.md
```
