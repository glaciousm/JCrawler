package com.jcrawler.ui;

import com.jcrawler.model.DownloadedFile;
import com.jcrawler.repository.DownloadedFileRepository;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.awt.Desktop;
import java.net.URI;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DownloadsTab {

    private final DownloadedFileRepository downloadRepository;

    private TableView<DownloadedFile> downloadTable;
    private ObservableList<DownloadedFile> downloadData;
    private TextField sessionFilterField;
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public VBox getContent() {
        VBox vbox = new VBox(15);
        vbox.setPadding(new Insets(20));

        // Title
        Label title = new Label("Downloaded Files");
        title.setFont(Font.font("System", FontWeight.BOLD, 18));

        // Filter by session ID
        Label filterLabel = new Label("Filter by Session ID:");
        sessionFilterField = new TextField();
        sessionFilterField.setPromptText("Enter session ID");
        sessionFilterField.setPrefWidth(150);

        Button filterButton = new Button("Filter");
        filterButton.setOnAction(e -> filterBySession());

        Button clearButton = new Button("Show All");
        clearButton.setOnAction(e -> loadAllDownloads());

        Button refreshButton = new Button("Refresh");
        refreshButton.setOnAction(e -> {
            if (sessionFilterField.getText().trim().isEmpty()) {
                loadAllDownloads();
            } else {
                filterBySession();
            }
        });

        Button openUrlButton = new Button("Open URL in Browser");
        openUrlButton.setOnAction(e -> openSelectedUrl());

        HBox filterBox = new HBox(10, filterLabel, sessionFilterField, filterButton, clearButton, refreshButton, openUrlButton);
        filterBox.setAlignment(Pos.CENTER_LEFT);

        // Create table
        downloadTable = new TableView<>();
        downloadData = FXCollections.observableArrayList();
        downloadTable.setItems(downloadData);

        // ID Column
        TableColumn<DownloadedFile, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getId().toString()));
        idCol.setPrefWidth(50);

        // Session ID Column
        TableColumn<DownloadedFile, String> sessionCol = new TableColumn<>("Session");
        sessionCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSessionId().toString()));
        sessionCol.setPrefWidth(70);

        // File Name Column
        TableColumn<DownloadedFile, String> nameCol = new TableColumn<>("File Name");
        nameCol.setCellValueFactory(data -> {
            String fileName = data.getValue().getFileName();
            return new SimpleStringProperty(fileName != null ? fileName : "N/A");
        });
        nameCol.setPrefWidth(250);

        // URL Column
        TableColumn<DownloadedFile, String> urlCol = new TableColumn<>("URL");
        urlCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getUrl()));
        urlCol.setPrefWidth(350);

        // Extension Column
        TableColumn<DownloadedFile, String> extCol = new TableColumn<>("Type");
        extCol.setCellValueFactory(data -> {
            String ext = data.getValue().getFileExtension();
            return new SimpleStringProperty(ext != null ? ext : "N/A");
        });
        extCol.setPrefWidth(80);

        // File Size Column
        TableColumn<DownloadedFile, String> sizeCol = new TableColumn<>("Size");
        sizeCol.setCellValueFactory(data -> {
            Long size = data.getValue().getFileSize();
            if (size != null) {
                return new SimpleStringProperty(formatFileSize(size));
            }
            return new SimpleStringProperty("N/A");
        });
        sizeCol.setPrefWidth(100);

        // Download Time Column
        TableColumn<DownloadedFile, String> timeCol = new TableColumn<>("Downloaded At");
        timeCol.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getDownloadedAt().format(dateFormatter)));
        timeCol.setPrefWidth(150);

        // Success Column
        TableColumn<DownloadedFile, String> successCol = new TableColumn<>("Status");
        successCol.setCellValueFactory(data -> {
            Boolean success = data.getValue().getDownloadSuccess();
            return new SimpleStringProperty(success ? "✓ Success" : "✗ Failed");
        });
        successCol.setPrefWidth(100);

        downloadTable.getColumns().addAll(idCol, sessionCol, nameCol, urlCol, extCol, sizeCol, timeCol, successCol);

        VBox.setVgrow(downloadTable, Priority.ALWAYS);

        // Statistics area
        Label statsLabel = new Label("Statistics:");
        statsLabel.setFont(Font.font("System", FontWeight.BOLD, 12));

        Label statsText = new Label("Total Files: 0 | Successful: 0 | Failed: 0");

        // Update stats when data changes
        downloadData.addListener((javafx.collections.ListChangeListener.Change<? extends DownloadedFile> c) -> {
            long total = downloadData.size();
            long successful = downloadData.stream().filter(DownloadedFile::getDownloadSuccess).count();
            long failed = total - successful;
            statsText.setText("Total Files: " + total + " | Successful: " + successful + " | Failed: " + failed);
        });

        HBox statsBox = new HBox(10, statsLabel, statsText);
        statsBox.setAlignment(Pos.CENTER_LEFT);

        vbox.getChildren().addAll(title, filterBox, downloadTable, statsBox);

        // Load all downloads on initialization
        loadAllDownloads();

        return vbox;
    }

    private void loadAllDownloads() {
        new Thread(() -> {
            try {
                List<DownloadedFile> downloads = downloadRepository.findAll();
                Platform.runLater(() -> {
                    downloadData.clear();
                    downloadData.addAll(downloads);
                    sessionFilterField.clear();
                });
            } catch (Exception e) {
                Platform.runLater(() -> showAlert("Error", "Failed to load downloads: " + e.getMessage()));
            }
        }).start();
    }

    private void filterBySession() {
        String sessionIdText = sessionFilterField.getText().trim();
        if (sessionIdText.isEmpty()) {
            showAlert("Error", "Please enter a session ID");
            return;
        }

        try {
            Long sessionId = Long.parseLong(sessionIdText);
            new Thread(() -> {
                try {
                    List<DownloadedFile> downloads = downloadRepository.findBySessionId(sessionId);
                    Platform.runLater(() -> {
                        downloadData.clear();
                        downloadData.addAll(downloads);
                        if (downloads.isEmpty()) {
                            showInfo("No Results", "No downloads found for session ID: " + sessionId);
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> showAlert("Error", "Failed to filter downloads: " + e.getMessage()));
                }
            }).start();
        } catch (NumberFormatException e) {
            showAlert("Error", "Invalid session ID format");
        }
    }

    private void openSelectedUrl() {
        DownloadedFile selected = downloadTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Error", "Please select a file to open its URL");
            return;
        }

        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(selected.getUrl()));
            } else {
                showAlert("Error", "Desktop browsing is not supported on this system");
            }
        } catch (Exception e) {
            showAlert("Error", "Failed to open URL: " + e.getMessage());
        }
    }

    private String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.2f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", size / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", size / (1024.0 * 1024.0 * 1024.0));
        }
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
