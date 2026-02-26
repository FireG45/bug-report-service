package ru.bre.admin.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import ru.bre.admin.model.FeedbackDto;
import ru.bre.admin.model.ReportDto;
import ru.bre.admin.model.SummaryDto;
import ru.bre.admin.service.StorageServiceClient;
import ru.bre.admin.util.ConfigManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class MainController {
    @FXML
    private TextField hostField;

    @FXML
    private TextField secretField;

    @FXML
    private TextField reportHostField;

    @FXML
    private ToggleButton frontendReportToggle;

    @FXML
    private Button connectButton;

    @FXML
    private Button reportsButton;

    @FXML
    private Button feedbackButton;

    @FXML
    private Button summaryButton;

    @FXML
    private Button reportsDeleteButton;

    @FXML
    private Button feedbackDeleteButton;

    @FXML
    private Button summaryDeleteButton;

    @FXML
    private Label statusLabel;

    @FXML
    private Label pageInfoLabel;

    @FXML
    private Button prevPageButton;

    @FXML
    private Button nextPageButton;

    @FXML
    private Button deleteSelectedButton;

    @FXML
    private TableView<Object> dataTable;

    private StorageServiceClient client;
    private String currentHost;
    private String currentEntityType;
    private int currentPage = 0;
    private static final int PAGE_SIZE = 100;

    @FXML
    public void initialize() {
        client = new StorageServiceClient();
        
        // Загружаем сохраненный хост
        String savedHost = ConfigManager.loadHost();
        if (!savedHost.isEmpty()) {
            hostField.setText(savedHost);
        }

        String savedSecret = ConfigManager.loadSecret();
        if (!savedSecret.isEmpty()) {
            secretField.setText(savedSecret);
        }

        String savedReportHost = ConfigManager.loadReportHost();
        if (!savedReportHost.isEmpty()) {
            reportHostField.setText(savedReportHost);
        }
        
        statusLabel.setText("Введите хост и нажмите 'Подключиться'");
        pageInfoLabel.setText("");
        
        reportsButton.setDisable(true);
        feedbackButton.setDisable(true);
        summaryButton.setDisable(true);
        prevPageButton.setDisable(true);
        nextPageButton.setDisable(true);
        reportsDeleteButton.setDisable(true);
        summaryDeleteButton.setDisable(true);
        feedbackDeleteButton.setDisable(true);
        deleteSelectedButton.setDisable(true);
        frontendReportToggle.setDisable(true);

        dataTable.getSelectionModel().setCellSelectionEnabled(true);
        dataTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        dataTable.setPlaceholder(new Label("Данные не загружены"));

        dataTable.setOnKeyPressed(e -> {
            if (e.isControlDown() && e.getCode() == KeyCode.C) {
                copySelectionToClipboard();
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void copySelectionToClipboard() {
        StringBuilder sb = new StringBuilder();

        for (TablePosition<?, ?> pos : dataTable.getSelectionModel().getSelectedCells()) {
            int row = pos.getRow();
            Object rowItem = dataTable.getItems().get(row);

            TableColumn<Object, Object> column =
                    (TableColumn<Object, Object>) pos.getTableColumn();

            Object cellValue = column.getCellObservableValue(rowItem).getValue();

            if (cellValue != null) {
                sb.append(cellValue).append('\t');
            }
        }

        ClipboardContent content = new ClipboardContent();
        content.putString(sb.toString());
        Clipboard.getSystemClipboard().setContent(content);
    }

    @FXML
    private void handleConnect() {
        String host = hostField.getText().trim();
        if (host.isEmpty()) {
            showError("Ошибка", "Пожалуйста, введите хост");
            return;
        }

        String secret = secretField.getText().trim();

        // Добавляем http:// если не указан протокол
        if (!host.startsWith("http://") && !host.startsWith("https://")) {
            host = "http://" + host;
        }

        String reportHost = reportHostField.getText().trim();
        if (!reportHost.isEmpty() && !reportHost.startsWith("http://") && !reportHost.startsWith("https://")) {
            reportHost = "http://" + reportHost;
        }

        try {
            client.setBaseUrl(host);
            client.setSecret(secret);
            if (!reportHost.isEmpty()) {
                client.setReportBaseUrl(reportHost);
            }
            currentHost = host;
            currentPage = 0;

            // Сохраняем хост
            ConfigManager.saveConfig(host, secret, reportHost);

            statusLabel.setText("Подключено к: " + host);
            pageInfoLabel.setText("");
            reportsButton.setDisable(false);
            feedbackButton.setDisable(false);
            summaryButton.setDisable(false);
            prevPageButton.setDisable(true);
            nextPageButton.setDisable(true);
            reportsDeleteButton.setDisable(false);
            summaryDeleteButton.setDisable(false);
            feedbackDeleteButton.setDisable(false);
            deleteSelectedButton.setDisable(false);

            if (!reportHost.isEmpty()) {
                frontendReportToggle.setDisable(false);
                loadFrontendReportState();
            }
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
    private void handleDeleteReports() {
        currentEntityType = "report";
        currentPage = 0;
        deleteData("report");
    }

    @FXML
    private void handleDeleteFeedback() {
        currentEntityType = "feedback";
        currentPage = 0;
        deleteData("feedback");
    }

    @FXML
    private void handleDeleteSummary() {
        currentEntityType = "summary";
        currentPage = 0;
        deleteData("summary");
    }

    @FXML
    private void handleDeleteSelected() {
        if (currentHost == null) {
            showError("Ошибка", "Сначала подключитесь к серверу");
            return;
        }
        if (currentEntityType == null) {
            showError("Ошибка", "Сначала загрузите данные");
            return;
        }

        Object selected = dataTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Ошибка", "Выберите строку для удаления");
            return;
        }

        Integer id = getEntityId(selected);
        if (id == null) {
            showError("Ошибка", "Не удалось определить ID записи");
            return;
        }

        statusLabel.setText("Удаление записи #" + id + "...");

        new Thread(() -> {
            try {
                client.deleteById(currentEntityType, id);
                Platform.runLater(() -> {
                    dataTable.getItems().remove(selected);
                    statusLabel.setText("Запись #" + id + " удалена");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Ошибка удаления");
                    showError("Ошибка", "Не удалось удалить запись: " + e.getMessage());
                });
            }
        }).start();
    }

    private Integer getEntityId(Object entity) {
        if (entity instanceof ReportDto dto) return dto.getId();
        if (entity instanceof FeedbackDto dto) return dto.getId();
        if (entity instanceof SummaryDto dto) return dto.getId();
        return null;
    }

    @FXML
    private void handleToggleFrontendReport() {
        boolean newValue = frontendReportToggle.isSelected();
        frontendReportToggle.setDisable(true);
        statusLabel.setText("Переключение frontend report...");

        new Thread(() -> {
            try {
                String result = client.setFrontendReport(newValue);
                Platform.runLater(() -> {
                    updateToggleText(newValue);
                    statusLabel.setText(result);
                    frontendReportToggle.setDisable(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    frontendReportToggle.setSelected(!newValue);
                    statusLabel.setText("Ошибка переключения frontend report");
                    showError("Ошибка", "Не удалось переключить frontend report: " + e.getMessage());
                    frontendReportToggle.setDisable(false);
                });
            }
        }).start();
    }

    private void loadFrontendReportState() {
        new Thread(() -> {
            try {
                boolean state = client.getFrontendReport();
                Platform.runLater(() -> {
                    frontendReportToggle.setSelected(state);
                    updateToggleText(state);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    frontendReportToggle.setText("Frontend Report: ???");
                    statusLabel.setText("Не удалось получить состояние frontend report");
                });
            }
        }).start();
    }

    private void updateToggleText(boolean enabled) {
        frontendReportToggle.setText("Frontend Report: " + (enabled ? "ON" : "OFF"));
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

    private void deleteData(String entityType) {
        if (currentHost == null) {
            showError("Ошибка", "Сначала подключитесь к серверу");
            return;
        }

        statusLabel.setText("Удаление данных...");
        prevPageButton.setDisable(true);
        nextPageButton.setDisable(true);

        new Thread(() -> {
            try {
                switch (entityType) {
                    case "report":
                        client.delete(entityType);
                        List<ReportDto> reports = client.getReports(0, 100);
                        Platform.runLater(() -> {
                            showReports(reports);
                            updatePagination(reports.size());
                        });
                        break;
                    case "feedback":
                        client.delete(entityType);
                        List<FeedbackDto> feedbacks = client.getFeedback(0, 100);
                        Platform.runLater(() -> {
                            showFeedback(feedbacks);
                            updatePagination(feedbacks.size());
                        });
                        break;
                    case "summary":
                        client.delete(entityType);
                        List<SummaryDto> summaries = client.getSummary(0, 100);
                        Platform.runLater(() -> {
                            showSummary(summaries);
                            updatePagination(summaries.size());
                        });
                        break;
                }
                Platform.runLater(() -> statusLabel.setText("Данные удалены успешно"));
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Ошибка удаления данных");
                    showError("Ошибка", "Не удалось удалить данные: " + e.getMessage());
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
        titleCol.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleStringProperty(
                        ((ReportDto) cell.getValue()).getTitle()
                )
        );
        titleCol.setPrefWidth(220);

        TableColumn<Object, String> textCol = new TableColumn<>("Text");
        textCol.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleStringProperty(
                        ((ReportDto) cell.getValue()).getText()
                )
        );
        textCol.setPrefWidth(420);

        TableColumn<Object, String> imageFileCol = new TableColumn<>("Image File");
        imageFileCol.setCellValueFactory(cell -> {
            ReportDto dto = (ReportDto) cell.getValue();
            return new javafx.beans.property.SimpleStringProperty(
                    buildMinioUrl(dto.getImageFile(), "screenshots")
            );
        });
        imageFileCol.setCellFactory(col -> createLinkCell());
        imageFileCol.setPrefWidth(220);

        TableColumn<Object, String> logFileCol = new TableColumn<>("Log File");
        logFileCol.setCellValueFactory(cell -> {
            ReportDto dto = (ReportDto) cell.getValue();
            return new javafx.beans.property.SimpleStringProperty(
                    buildMinioUrl(dto.getLogFile(), "logs")
            );
        });
        logFileCol.setCellFactory(col -> createLinkCell());
        logFileCol.setPrefWidth(220);

        dataTable.getColumns().addAll(
                titleCol,
                textCol,
                imageFileCol,
                logFileCol
        );

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

    private TableCell<Object, String> createLinkCell() {
        return new TableCell<>() {
            private final Hyperlink link = new Hyperlink();

            {
                link.setOnAction(e -> {
                    String url = getItem();
                    if (url != null && !url.isBlank()) {
                        try {
                            java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
                        } catch (Exception ex) {
                            showError("Ошибка", "Не удалось открыть ссылку");
                        }
                    }
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null || item.isBlank()) {
                    setGraphic(null);
                    setText(null);
                } else {
                    link.setText(item);
                    setGraphic(link);
                    setText(null);
                }
            }
        };
    }

    private String buildMinioUrl(String fileName, String bucket) {
        if (fileName == null || fileName.isBlank()) {
            return "";
        }

        String host = hostField.getText().trim();
        if (host.isBlank()) {
            return "";
        }

        host = host.replaceAll(":\\d+$", "");

        return host + ":9000/" + bucket + "/" + fileName;
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }}
