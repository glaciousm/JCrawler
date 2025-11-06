package com.jcrawler.ui;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MainStage {

    private final CrawlerTab crawlerTab;
    private final SessionsTab sessionsTab;
    private final DownloadsTab downloadsTab;

    public void start(Stage primaryStage) {
        primaryStage.setTitle("JCrawler - Web Crawler & Data Extraction");

        // Create main layout
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        // Create tab pane
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

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
