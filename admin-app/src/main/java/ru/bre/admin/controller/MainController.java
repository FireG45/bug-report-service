package ru.bre.admin.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import ru.bre.admin.model.FeedbackDto;
import ru.bre.admin.model.ReportDto;
import ru.bre.admin.model.SummaryDto;
import ru.bre.admin.service.StorageServiceClient;
import ru.bre.admin.util.ConfigManager;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class MainController {
    @FXML
    private TextField hostField;

    @FXML
    private Button connectButton;

    @FXML
    private Button reportsButton;

    @FXML
    private Button feedbackButton;

    @FXML
    private Button summaryButton;

    @FXML
    private Label statusLabel;

    @FXML
    private Label pageInfoLabel;

    @FXML
    private Button prevPageButton;

    @FXML
    private Button nextPageButton;

    @FXML
    private TableView<Object> dataTable;

    private StorageServiceClient client;
    private String currentHost;
    private String currentEntityType;
    private int currentPage = 0;
    private static final int PAGE_SIZE = 50;

    @FXML
    public void initialize() {
        client = new StorageServiceClient();
        
        // Загружаем сохраненный хост
        String savedHost = ConfigManager.loadHost();
        if (!savedHost.isEmpty()) {
            hostField.setText(savedHost);
        }
        
        statusLabel.setText("Введите хост и нажмите 'Подключиться'");
        pageInfoLabel.setText("");
        
        reportsButton.setDisable(true);
        feedbackButton.setDisable(true);
        summaryButton.setDisable(true);
        prevPageButton.setDisable(true);
        nextPageButton.setDisable(true);

        dataTable.setPlaceholder(new Label("Данные не загружены"));
    }

    @FXML
    private void handleConnect() {
        String host = hostField.getText().trim();
        if (host.isEmpty()) {
            showError("Ошибка", "Пожалуйста, введите хост");
            return;
        }

        // Добавляем http:// если не указан протокол
        if (!host.startsWith("http://") && !host.startsWith("https://")) {
            host = "http://" + host;
        }

        try {
            client.setBaseUrl(host);
            currentHost = host;
            currentPage = 0;
            
            // Сохраняем хост
            ConfigManager.saveHost(host);
            
            statusLabel.setText("Подключено к: " + host);
            pageInfoLabel.setText("");
            reportsButton.setDisable(false);
            feedbackButton.setDisable(false);
            summaryButton.setDisable(false);
            prevPageButton.setDisable(true);
            nextPageButton.setDisable(true);
        } catch (Exception e) {
            showError("Ошибка подключения", e.getMessage());
            statusLabel.setText("Ошибка подключения");
        }
    }

    @FXML
    private void handleGetReports() {
        currentEntityType = "report";
        currentPage = 0;
        loadData("report", 0);
    }

    @FXML
    private void handleGetFeedback() {
        currentEntityType = "feedback";
        currentPage = 0;
        loadData("feedback", 0);
    }

    @FXML
    private void handleGetSummary() {
        currentEntityType = "summary";
        currentPage = 0;
        loadData("summary", 0);
    }

    @FXML
    private void handlePrevPage() {
        if (currentPage > 0) {
            currentPage--;
            loadData(currentEntityType, currentPage);
        }
    }

    @FXML
    private void handleNextPage() {
        currentPage++;
        loadData(currentEntityType, currentPage);
    }

    private void loadData(String entityType, int page) {
        if (currentHost == null) {
            showError("Ошибка", "Сначала подключитесь к серверу");
            return;
        }

        int offset = page * PAGE_SIZE;
        int limit = PAGE_SIZE;

        statusLabel.setText("Загрузка данных...");
        prevPageButton.setDisable(true);
        nextPageButton.setDisable(true);

        // Запускаем в отдельном потоке, чтобы не блокировать UI
        new Thread(() -> {
            try {
                switch (entityType) {
                    case "report":
                        List<ReportDto> reports = client.getReports(offset, limit);
                        Platform.runLater(() -> {
                            showReports(reports);
                            updatePagination(reports.size());
                        });
                        break;
                    case "feedback":
                        List<FeedbackDto> feedbacks = client.getFeedback(offset, limit);
                        Platform.runLater(() -> {
                            showFeedback(feedbacks);
                            updatePagination(feedbacks.size());
                        });
                        break;
                    case "summary":
                        List<SummaryDto> summaries = client.getSummary(offset, limit);
                        Platform.runLater(() -> {
                            showSummary(summaries);
                            updatePagination(summaries.size());
                        });
                        break;
                }
                Platform.runLater(() -> statusLabel.setText("Данные загружены успешно"));
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Ошибка загрузки данных");
                    showError("Ошибка", "Не удалось загрузить данные: " + e.getMessage());
                    prevPageButton.setDisable(true);
                    nextPageButton.setDisable(true);
                });
            }
        }).start();
    }

    private void updatePagination(int itemsCount) {
        pageInfoLabel.setText(String.format("Страница %d (показано %d записей)", currentPage + 1, itemsCount));
        
        prevPageButton.setDisable(currentPage == 0);
        // Если получили меньше записей, чем PAGE_SIZE, значит это последняя страница
        nextPageButton.setDisable(itemsCount < PAGE_SIZE);
    }

    private void showReports(List<ReportDto> reports) {
        dataTable.getColumns().clear();

        TableColumn<Object, String> titleCol = new TableColumn<>("Title");
        titleCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(((ReportDto) cell.getValue()).getTitle()));
        titleCol.setPrefWidth(220);

        TableColumn<Object, String> textCol = new TableColumn<>("Text");
        textCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(((ReportDto) cell.getValue()).getText()));
        textCol.setPrefWidth(420);

        TableColumn<Object, String> imageFileCol = new TableColumn<>("Image File");
        imageFileCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(((ReportDto) cell.getValue()).getImageFile()));
        imageFileCol.setPrefWidth(220);

        TableColumn<Object, String> logFileCol = new TableColumn<>("Log File");
        logFileCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(((ReportDto) cell.getValue()).getLogFile()));
        logFileCol.setPrefWidth(220);

        dataTable.getColumns().addAll(titleCol, textCol, imageFileCol, logFileCol);
        dataTable.setItems(FXCollections.observableArrayList(reports));
    }

    private void showFeedback(List<FeedbackDto> feedbacks) {
        dataTable.getColumns().clear();

        TableColumn<Object, String> titleCol = new TableColumn<>("Title");
        titleCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(((FeedbackDto) cell.getValue()).getTitle()));
        titleCol.setPrefWidth(260);

        TableColumn<Object, String> textCol = new TableColumn<>("Text");
        textCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(((FeedbackDto) cell.getValue()).getText()));
        textCol.setPrefWidth(760);

        dataTable.getColumns().addAll(titleCol, textCol);
        dataTable.setItems(FXCollections.observableArrayList(feedbacks));
    }

    private void showSummary(List<SummaryDto> summaries) {
        dataTable.getColumns().clear();

        TableColumn<Object, String> titleCol = new TableColumn<>("Title");
        titleCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(((SummaryDto) cell.getValue()).getTitle()));
        titleCol.setPrefWidth(260);

        TableColumn<Object, String> textCol = new TableColumn<>("Text");
        textCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(((SummaryDto) cell.getValue()).getText()));
        textCol.setPrefWidth(640);

        TableColumn<Object, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(cell -> {
            Date d = ((SummaryDto) cell.getValue()).getDate();
            if (d == null) {
                return new javafx.beans.property.SimpleStringProperty("");
            }
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return new javafx.beans.property.SimpleStringProperty(sdf.format(d));
        });
        dateCol.setPrefWidth(180);

        dataTable.getColumns().addAll(titleCol, textCol, dateCol);
        dataTable.setItems(FXCollections.observableArrayList(summaries));
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
