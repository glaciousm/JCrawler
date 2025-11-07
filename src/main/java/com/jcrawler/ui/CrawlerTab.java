package com.jcrawler.ui;

import com.jcrawler.dto.CrawlRequest;
import com.jcrawler.dto.CrawlResponse;
import com.jcrawler.service.CrawlerService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class CrawlerTab {

    private final CrawlerService crawlerService;

    public CrawlerTab(CrawlerService crawlerService) {
        this.crawlerService = crawlerService;
    }

    private TextField urlField;
    private Spinner<Integer> maxDepthSpinner;
    private Spinner<Integer> maxPagesSpinner;
    private Spinner<Double> requestDelaySpinner;
    private Spinner<Integer> concurrentThreadsSpinner;
    private CheckBox enableJsCheckBox;
    private CheckBox downloadFilesCheckBox;
    private Button startButton;
    private Button pauseButton;
    private Button stopButton;
    private TextArea logArea;
    private Label statusLabel;
    private ProgressBar progressBar;

    private Long currentSessionId;

    public VBox getContent() {
        VBox vbox = new VBox(15);
        vbox.setPadding(new Insets(20));

        // Title
        Label title = new Label("Web Crawler Configuration");
        title.setFont(Font.font("System", FontWeight.BOLD, 18));

        // URL Input
        Label urlLabel = new Label("Start URL:");
        urlField = new TextField();
        urlField.setPromptText("https://example.com");
        urlField.setPrefWidth(600);

        HBox urlBox = new HBox(10, urlLabel, urlField);
        urlBox.setAlignment(Pos.CENTER_LEFT);

        // Configuration Grid
        GridPane configGrid = new GridPane();
        configGrid.setHgap(15);
        configGrid.setVgap(10);
        configGrid.setPadding(new Insets(10));

        // Max Depth
        Label depthLabel = new Label("Max Depth:");
        maxDepthSpinner = new Spinner<>(0, 50, 0);
        maxDepthSpinner.setEditable(true);
        maxDepthSpinner.setPrefWidth(100);
        configGrid.add(depthLabel, 0, 0);
        configGrid.add(maxDepthSpinner, 1, 0);

        // Max Pages
        Label pagesLabel = new Label("Max Pages:");
        maxPagesSpinner = new Spinner<>(0, 10000, 0);
        maxPagesSpinner.setEditable(true);
        maxPagesSpinner.setPrefWidth(100);
        configGrid.add(pagesLabel, 2, 0);
        configGrid.add(maxPagesSpinner, 3, 0);

        // Request Delay
        Label delayLabel = new Label("Request Delay (s):");
        requestDelaySpinner = new Spinner<>(0.0, 10.0, 1.0, 0.1);
        requestDelaySpinner.setEditable(true);
        requestDelaySpinner.setPrefWidth(100);
        configGrid.add(delayLabel, 0, 1);
        configGrid.add(requestDelaySpinner, 1, 1);

        // Concurrent Threads
        Label threadsLabel = new Label("Concurrent Threads:");
        concurrentThreadsSpinner = new Spinner<>(1, 20, 5);
        concurrentThreadsSpinner.setEditable(true);
        concurrentThreadsSpinner.setPrefWidth(100);
        configGrid.add(threadsLabel, 2, 1);
        configGrid.add(concurrentThreadsSpinner, 3, 1);

        // Checkboxes
        enableJsCheckBox = new CheckBox("Enable JavaScript Rendering");
        downloadFilesCheckBox = new CheckBox("Download Files");
        downloadFilesCheckBox.setSelected(true);

        HBox checkBoxBox = new HBox(20, enableJsCheckBox, downloadFilesCheckBox);
        checkBoxBox.setPadding(new Insets(10, 0, 0, 0));

        // Buttons
        startButton = new Button("Start Crawl");
        startButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
        startButton.setPrefWidth(120);
        startButton.setOnAction(e -> startCrawl());

        pauseButton = new Button("Pause");
        pauseButton.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white;");
        pauseButton.setPrefWidth(100);
        pauseButton.setDisable(true);
        pauseButton.setOnAction(e -> pauseCrawl());

        stopButton = new Button("Stop");
        stopButton.setStyle("-fx-background-color: #F44336; -fx-text-fill: white;");
        stopButton.setPrefWidth(100);
        stopButton.setDisable(true);
        stopButton.setOnAction(e -> stopCrawl());

        HBox buttonBox = new HBox(10, startButton, pauseButton, stopButton);
        buttonBox.setAlignment(Pos.CENTER_LEFT);

        // Status Section
        statusLabel = new Label("Status: Idle");
        statusLabel.setFont(Font.font("System", FontWeight.BOLD, 14));

        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(600);

        VBox statusBox = new VBox(5, statusLabel, progressBar);

        // Log Area
        Label logLabel = new Label("Crawl Log:");
        logLabel.setFont(Font.font("System", FontWeight.BOLD, 12));

        Button refreshLogButton = new Button("Refresh");
        refreshLogButton.setOnAction(e -> refreshLog());

        HBox logHeaderBox = new HBox(10, logLabel, refreshLogButton);
        logHeaderBox.setAlignment(Pos.CENTER_LEFT);

        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefRowCount(15);
        logArea.setWrapText(true);
        logArea.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 11px;");

        VBox logBox = new VBox(5, logHeaderBox, logArea);
        VBox.setVgrow(logArea, Priority.ALWAYS);

        // Add all to main vbox
        vbox.getChildren().addAll(
                title,
                new Separator(),
                urlBox,
                configGrid,
                checkBoxBox,
                buttonBox,
                new Separator(),
                statusBox,
                logBox
        );

        return vbox;
    }

    private void startCrawl() {
        String url = urlField.getText().trim();
        if (url.isEmpty()) {
            showAlert("Error", "Please enter a valid URL");
            return;
        }

        CrawlRequest request = CrawlRequest.builder()
                .startUrl(url)
                .maxDepth(maxDepthSpinner.getValue())
                .maxPages(maxPagesSpinner.getValue())
                .requestDelay(requestDelaySpinner.getValue())
                .concurrentThreads(concurrentThreadsSpinner.getValue())
                .enableJavaScript(enableJsCheckBox.isSelected())
                .downloadFiles(downloadFilesCheckBox.isSelected())
                .build();

        // Start crawl in background thread
        new Thread(() -> {
            try {
                CrawlResponse response = crawlerService.startCrawl(request, message -> {
                    // Send log messages to GUI
                    Platform.runLater(() -> logArea.appendText(message + "\n"));
                });
                currentSessionId = response.getSessionId();

                Platform.runLater(() -> {
                    statusLabel.setText("Status: Running (Session ID: " + currentSessionId + ")");
                    logArea.appendText("Crawl started for: " + url + "\n");
                    logArea.appendText("Session ID: " + currentSessionId + "\n");
                    logArea.appendText("Max Depth: " + request.getMaxDepth() + " | Max Pages: " + request.getMaxPages() + "\n");
                    logArea.appendText("JavaScript: " + (request.getEnableJavaScript() ? "Enabled" : "Disabled") + "\n");
                    logArea.appendText("----------------------------------------\n");

                    startButton.setDisable(true);
                    pauseButton.setDisable(false);
                    stopButton.setDisable(false);
                    progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    showAlert("Error", "Failed to start crawl: " + e.getMessage());
                    logArea.appendText("ERROR: " + e.getMessage() + "\n");
                });
            }
        }).start();
    }

    private void pauseCrawl() {
        if (currentSessionId == null) return;

        new Thread(() -> {
            try {
                crawlerService.pauseCrawl(currentSessionId);
                Platform.runLater(() -> {
                    statusLabel.setText("Status: Paused");
                    logArea.appendText("Crawl paused\n");
                    pauseButton.setText("Resume");
                    pauseButton.setOnAction(e -> resumeCrawl());
                });
            } catch (Exception e) {
                Platform.runLater(() -> showAlert("Error", "Failed to pause crawl: " + e.getMessage()));
            }
        }).start();
    }

    private void resumeCrawl() {
        if (currentSessionId == null) return;

        new Thread(() -> {
            try {
                crawlerService.resumeCrawl(currentSessionId);
                Platform.runLater(() -> {
                    statusLabel.setText("Status: Running");
                    logArea.appendText("Crawl resumed\n");
                    pauseButton.setText("Pause");
                    pauseButton.setOnAction(e -> pauseCrawl());
                });
            } catch (Exception e) {
                Platform.runLater(() -> showAlert("Error", "Failed to resume crawl: " + e.getMessage()));
            }
        }).start();
    }

    private void stopCrawl() {
        if (currentSessionId == null) return;

        new Thread(() -> {
            try {
                crawlerService.stopCrawl(currentSessionId);
                Platform.runLater(() -> {
                    statusLabel.setText("Status: Stopped");
                    logArea.appendText("Crawl stopped\n");
                    logArea.appendText("========================================\n\n");
                    startButton.setDisable(false);
                    pauseButton.setDisable(true);
                    stopButton.setDisable(true);
                    progressBar.setProgress(0);
                    currentSessionId = null;
                });
            } catch (Exception e) {
                Platform.runLater(() -> showAlert("Error", "Failed to stop crawl: " + e.getMessage()));
            }
        }).start();
    }

    private void refreshLog() {
        if (currentSessionId == null) {
            logArea.appendText("No active session to refresh\n");
            return;
        }

        new Thread(() -> {
            try {
                CrawlResponse response = crawlerService.getStatus(currentSessionId);
                Platform.runLater(() -> {
                    logArea.appendText("----------------------------------------\n");
                    logArea.appendText("Refreshed at: " + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")) + "\n");
                    logArea.appendText("Session ID: " + response.getSessionId() + "\n");
                    logArea.appendText("Status: " + response.getStatus() + "\n");
                    logArea.appendText("Pages Crawled: " + response.getTotalPages() + "\n");
                    logArea.appendText("Flows Found: " + response.getTotalFlows() + "\n");
                    logArea.appendText("Downloads: " + response.getTotalDownloaded() + "\n");
                    logArea.appendText("External URLs: " + response.getTotalExternalUrls() + "\n");
                    logArea.appendText("----------------------------------------\n");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    logArea.appendText("ERROR refreshing: " + e.getMessage() + "\n");
                });
            }
        }).start();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
