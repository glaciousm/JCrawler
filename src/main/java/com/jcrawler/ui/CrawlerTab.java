package com.jcrawler.ui;

import com.jcrawler.dao.InternalLinkDao;
import com.jcrawler.dao.PageDao;
import com.jcrawler.dto.CrawlRequest;
import com.jcrawler.dto.CrawlResponse;
import com.jcrawler.model.InternalLink;
import com.jcrawler.model.Page;
import com.jcrawler.service.CrawlerService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.List;

public class CrawlerTab {

    private final CrawlerService crawlerService;
    private final PageDao pageDao;
    private final InternalLinkDao internalLinkDao;

    public CrawlerTab(CrawlerService crawlerService, PageDao pageDao, InternalLinkDao internalLinkDao) {
        this.crawlerService = crawlerService;
        this.pageDao = pageDao;
        this.internalLinkDao = internalLinkDao;
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
    private Button refreshButton;
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

        refreshButton = new Button("Refresh Status");
        refreshButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");
        refreshButton.setPrefWidth(120);
        refreshButton.setOnAction(e -> refreshStatus());

        HBox buttonBox = new HBox(10, startButton, pauseButton, stopButton, refreshButton);
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

        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefRowCount(15);
        logArea.setWrapText(true);
        logArea.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 11px;");

        VBox logBox = new VBox(5, logLabel, logArea);
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

        // Clear log area
        logArea.clear();
        logArea.appendText("=== STARTING CRAWL ===\n");
        logArea.appendText("URL: " + url + "\n");
        logArea.appendText("JavaScript: " + enableJsCheckBox.isSelected() + "\n");
        logArea.appendText("Max Depth: " + maxDepthSpinner.getValue() + "\n\n");

        // Set up UI callback for real-time logging
        System.out.println("[CrawlerTab] Setting UI callback...");
        crawlerService.setUICallback(message -> {
            System.out.println("[CrawlerTab] Callback received message: " + message);
            Platform.runLater(() -> {
                System.out.println("[CrawlerTab] Appending to logArea: " + message);
                logArea.appendText("[CALLBACK] " + message + "\n");
            });
        });
        System.out.println("[CrawlerTab] UI callback set!");

        // IMMEDIATE TEST - This should show up RIGHT AWAY
        logArea.appendText("TEST: About to start background thread\n");

        // Start crawl in background thread
        new Thread(() -> {
            try {
                logArea.appendText("TEST: Inside background thread, calling startCrawl\n");
                CrawlResponse response = crawlerService.startCrawl(request);
                currentSessionId = response.getSessionId();

                Platform.runLater(() -> {
                    logArea.appendText("TEST: startCrawl returned, session ID: " + currentSessionId + "\n");
                    statusLabel.setText("Status: Running (Session ID: " + currentSessionId + ")");
                    // Messages now come from the callback, not here
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

    private void refreshStatus() {
        if (currentSessionId == null) {
            logArea.appendText("\n[REFRESH] No active session to refresh\n");
            return;
        }

        new Thread(() -> {
            try {
                Platform.runLater(() -> logArea.appendText("\n[REFRESH] Fetching session data from database...\n"));

                CrawlResponse response = crawlerService.getStatus(currentSessionId);
                List<Page> pages = pageDao.findBySessionId(currentSessionId);
                List<InternalLink> internalLinks = internalLinkDao.findBySessionId(currentSessionId);

                Platform.runLater(() -> {
                    logArea.appendText("[REFRESH] === SESSION STATUS ===\n");
                    logArea.appendText("[REFRESH] Session ID: " + response.getSessionId() + "\n");
                    logArea.appendText("[REFRESH] Status: " + response.getStatus() + "\n");
                    logArea.appendText("[REFRESH] Total Pages: " + response.getTotalPages() + "\n");
                    logArea.appendText("[REFRESH] Total Flows: " + response.getTotalFlows() + "\n");
                    logArea.appendText("[REFRESH] Total Downloaded: " + response.getTotalDownloaded() + "\n");
                    logArea.appendText("[REFRESH] Total External URLs: " + response.getTotalExternalUrls() + "\n");
                    logArea.appendText("[REFRESH] Start Time: " + response.getStartTime() + "\n");
                    if (response.getEndTime() != null) {
                        logArea.appendText("[REFRESH] End Time: " + response.getEndTime() + "\n");
                    }
                    logArea.appendText("[REFRESH] ========================\n\n");

                    // Show actual pages found
                    logArea.appendText("[REFRESH] === PAGES IN DATABASE ===\n");
                    if (pages.isEmpty()) {
                        logArea.appendText("[REFRESH] No pages found in database!\n");
                    } else {
                        for (Page page : pages) {
                            logArea.appendText("[REFRESH] Page: " + page.getUrl() +
                                " (depth: " + page.getDepthLevel() +
                                ", status: " + page.getStatusCode() + ")\n");
                        }
                    }
                    logArea.appendText("[REFRESH] ========================\n\n");

                    // Show internal links found
                    logArea.appendText("[REFRESH] === INTERNAL LINKS IN DATABASE ===\n");
                    if (internalLinks.isEmpty()) {
                        logArea.appendText("[REFRESH] No internal links found! This means:\n");
                        logArea.appendText("[REFRESH]   - WebView might not be rendering the page\n");
                        logArea.appendText("[REFRESH]   - Page has no links\n");
                        logArea.appendText("[REFRESH]   - Link extraction failed\n");
                    } else {
                        int toShow = Math.min(10, internalLinks.size());
                        logArea.appendText("[REFRESH] Showing first " + toShow + " of " + internalLinks.size() + " links:\n");
                        for (int i = 0; i < toShow; i++) {
                            InternalLink link = internalLinks.get(i);
                            logArea.appendText("[REFRESH]   " + link.getUrl() + "\n");
                        }
                    }
                    logArea.appendText("[REFRESH] ========================\n\n");

                    statusLabel.setText("Status: " + response.getStatus());
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    logArea.appendText("[REFRESH] ERROR: " + e.getMessage() + "\n");
                    e.printStackTrace();
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
