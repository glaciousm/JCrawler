package com.jcrawler.ui;

import com.jcrawler.dao.CrawlSessionDao;
import com.jcrawler.dao.DownloadedFileDao;
import com.jcrawler.service.CrawlerService;
import com.jcrawler.service.ExportService;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class MainStage {

    private final CrawlerService crawlerService;
    private final CrawlSessionDao sessionDao;
    private final DownloadedFileDao downloadedFileDao;
    private final ExportService exportService;

    public MainStage(CrawlerService crawlerService, CrawlSessionDao sessionDao,
                    DownloadedFileDao downloadedFileDao, ExportService exportService) {
        this.crawlerService = crawlerService;
        this.sessionDao = sessionDao;
        this.downloadedFileDao = downloadedFileDao;
        this.exportService = exportService;
    }

    public void start(Stage primaryStage) {
        primaryStage.setTitle("JCrawler - Lightweight Web Crawler");

        // Create main layout
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        // Create tab pane
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // Create tabs with dependencies
        CrawlerTab crawlerTab = new CrawlerTab(crawlerService);
        SessionsTab sessionsTab = new SessionsTab(sessionDao, exportService);
        DownloadsTab downloadsTab = new DownloadsTab(downloadedFileDao);

        // Create tabs
        Tab crawlTab = new Tab("Crawler");
        crawlTab.setContent(crawlerTab.getContent());

        Tab sessTab = new Tab("Sessions");
        sessTab.setContent(sessionsTab.getContent());

        Tab downTab = new Tab("Downloads");
        downTab.setContent(downloadsTab.getContent());

        tabPane.getTabs().addAll(crawlTab, sessTab, downTab);

        root.setCenter(tabPane);

        // Create and set scene
        Scene scene = new Scene(root, 1200, 800);
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
