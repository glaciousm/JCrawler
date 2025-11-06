package com.jcrawler.ui;

import com.jcrawler.dao.CrawlSessionDao;
import com.jcrawler.model.CrawlSession;
import com.jcrawler.service.ExportService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class SessionsTab {

    private final CrawlSessionDao sessionDao;
    private final ExportService exportService;

    public SessionsTab(CrawlSessionDao sessionDao, ExportService exportService) {
        this.sessionDao = sessionDao;
        this.exportService = exportService;
    }

    private TableView<CrawlSession> sessionTable;
    private ObservableList<CrawlSession> sessionData;
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public VBox getContent() {
        VBox vbox = new VBox(15);
        vbox.setPadding(new Insets(20));

        // Title
        Label title = new Label("Crawl Sessions");
        title.setFont(Font.font("System", FontWeight.BOLD, 18));

        // Refresh button
        Button refreshButton = new Button("Refresh");
        refreshButton.setOnAction(e -> loadSessions());

        Button exportButton = new Button("Export Selected Session");
        exportButton.setOnAction(e -> exportSession());

        Button deleteButton = new Button("Delete Selected");
        deleteButton.setStyle("-fx-background-color: #F44336; -fx-text-fill: white;");
        deleteButton.setOnAction(e -> deleteSession());

        HBox buttonBox = new HBox(10, refreshButton, exportButton, deleteButton);
        buttonBox.setAlignment(Pos.CENTER_LEFT);

        // Create table
        sessionTable = new TableView<>();
        sessionData = FXCollections.observableArrayList();
        sessionTable.setItems(sessionData);

        // ID Column
        TableColumn<CrawlSession, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getId().toString()));
        idCol.setPrefWidth(50);

        // URL Column
        TableColumn<CrawlSession, String> urlCol = new TableColumn<>("Start URL");
        urlCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStartUrl()));
        urlCol.setPrefWidth(300);

        // Domain Column
        TableColumn<CrawlSession, String> domainCol = new TableColumn<>("Domain");
        domainCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getBaseDomain()));
        domainCol.setPrefWidth(150);

        // Status Column
        TableColumn<CrawlSession, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus().toString()));
        statusCol.setPrefWidth(100);

        // Pages Column
        TableColumn<CrawlSession, String> pagesCol = new TableColumn<>("Pages");
        pagesCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTotalPages().toString()));
        pagesCol.setPrefWidth(80);

        // Flows Column
        TableColumn<CrawlSession, String> flowsCol = new TableColumn<>("Flows");
        flowsCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTotalFlows().toString()));
        flowsCol.setPrefWidth(80);

        // Downloads Column
        TableColumn<CrawlSession, String> downloadsCol = new TableColumn<>("Files");
        downloadsCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTotalDownloaded().toString()));
        downloadsCol.setPrefWidth(80);

        // Start Time Column
        TableColumn<CrawlSession, String> startCol = new TableColumn<>("Start Time");
        startCol.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getStartTime().format(dateFormatter)));
        startCol.setPrefWidth(150);

        // End Time Column
        TableColumn<CrawlSession, String> endCol = new TableColumn<>("End Time");
        endCol.setCellValueFactory(data -> {
            if (data.getValue().getEndTime() != null) {
                return new SimpleStringProperty(data.getValue().getEndTime().format(dateFormatter));
            }
            return new SimpleStringProperty("N/A");
        });
        endCol.setPrefWidth(150);

        sessionTable.getColumns().addAll(idCol, urlCol, domainCol, statusCol, pagesCol,
                                         flowsCol, downloadsCol, startCol, endCol);

        VBox.setVgrow(sessionTable, Priority.ALWAYS);

        // Details area
        Label detailsLabel = new Label("Session Details:");
        detailsLabel.setFont(Font.font("System", FontWeight.BOLD, 12));

        TextArea detailsArea = new TextArea();
        detailsArea.setEditable(false);
        detailsArea.setPrefRowCount(5);
        detailsArea.setWrapText(true);

        // Update details when selection changes
        sessionTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                StringBuilder details = new StringBuilder();
                details.append("Session ID: ").append(newVal.getId()).append("\n");
                details.append("URL: ").append(newVal.getStartUrl()).append("\n");
                details.append("Status: ").append(newVal.getStatus()).append("\n");
                details.append("Max Depth: ").append(newVal.getMaxDepth()).append(" | Max Pages: ").append(newVal.getMaxPages()).append("\n");
                details.append("Concurrent Threads: ").append(newVal.getConcurrentThreads()).append(" | Request Delay: ").append(newVal.getRequestDelay()).append("s\n");
                details.append("JavaScript: ").append(newVal.getEnableJavaScript() ? "Enabled" : "Disabled").append("\n");
                details.append("Total Pages: ").append(newVal.getTotalPages()).append(" | Flows: ").append(newVal.getTotalFlows()).append("\n");
                details.append("Downloads: ").append(newVal.getTotalDownloaded()).append(" | External URLs: ").append(newVal.getTotalExternalUrls());
                detailsArea.setText(details.toString());
            } else {
                detailsArea.setText("");
            }
        });

        VBox detailsBox = new VBox(5, detailsLabel, detailsArea);

        vbox.getChildren().addAll(title, buttonBox, sessionTable, detailsBox);

        // Load sessions on initialization
        loadSessions();

        return vbox;
    }

    private void loadSessions() {
        new Thread(() -> {
            try {
                List<CrawlSession> sessions = sessionDao.findAll();
                Platform.runLater(() -> {
                    sessionData.clear();
                    sessionData.addAll(sessions);
                });
            } catch (Exception e) {
                Platform.runLater(() -> showAlert("Error", "Failed to load sessions: " + e.getMessage()));
            }
        }).start();
    }

    private void exportSession() {
        CrawlSession selected = sessionTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Error", "Please select a session to export");
            return;
        }

        // Show format selection dialog
        ChoiceDialog<String> dialog = new ChoiceDialog<>("JSON", "JSON", "CSV", "Excel", "PDF");
        dialog.setTitle("Export Format");
        dialog.setHeaderText("Select export format:");
        dialog.setContentText("Format:");

        dialog.showAndWait().ifPresent(format -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save Export File");
            fileChooser.setInitialFileName("session_" + selected.getId() + "_export");

            // Set extension filter based on format
            switch (format) {
                case "JSON":
                    fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
                    break;
                case "CSV":
                    fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
                    break;
                case "Excel":
                    fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
                    break;
                case "PDF":
                    fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
                    break;
            }

            File file = fileChooser.showSaveDialog(sessionTable.getScene().getWindow());
            if (file != null) {
                exportToFile(selected.getId(), format, file.getAbsolutePath());
            }
        });
    }

    private void exportToFile(Long sessionId, String format, String filePath) {
        new Thread(() -> {
            try {
                switch (format) {
                    case "JSON":
                        exportService.exportToJson(sessionId, filePath);
                        break;
                    case "CSV":
                        exportService.exportToCsv(sessionId, filePath);
                        break;
                    case "Excel":
                        exportService.exportToExcel(sessionId, filePath);
                        break;
                    case "PDF":
                        exportService.exportToPdf(sessionId, filePath);
                        break;
                }
                Platform.runLater(() -> showInfo("Success", "Session exported successfully to: " + filePath));
            } catch (Exception e) {
                Platform.runLater(() -> showAlert("Error", "Export failed: " + e.getMessage()));
            }
        }).start();
    }

    private void deleteSession() {
        CrawlSession selected = sessionTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Error", "Please select a session to delete");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Deletion");
        confirm.setHeaderText("Delete session " + selected.getId() + "?");
        confirm.setContentText("This will delete all associated data (pages, flows, downloads, etc.)");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                new Thread(() -> {
                    try {
                        sessionDao.deleteById(selected.getId());
                        Platform.runLater(() -> {
                            sessionData.remove(selected);
                            showInfo("Success", "Session deleted successfully");
                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> showAlert("Error", "Failed to delete session: " + e.getMessage()));
                    }
                }).start();
            }
        });
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
