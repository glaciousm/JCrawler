package com.jcrawler;

import com.jcrawler.core.AppContext;
import com.jcrawler.ui.MainStage;
import javafx.application.Application;
import javafx.stage.Stage;

public class JCrawlerApplication extends Application {

    private AppContext appContext;

    @Override
    public void init() {
        // Initialize application context
        appContext = AppContext.getInstance();
    }

    @Override
    public void start(Stage primaryStage) {
        // Create and start the main stage
        MainStage mainStage = new MainStage(
            appContext.getCrawlerService(),
            appContext.getCrawlSessionDao(),
            appContext.getDownloadedFileDao(),
            appContext.getExportService()
        );
        mainStage.start(primaryStage);
    }

    @Override
    public void stop() {
        // Shutdown application context
        if (appContext != null) {
            appContext.shutdown();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
