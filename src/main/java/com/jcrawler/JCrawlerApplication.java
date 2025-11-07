package com.jcrawler;

import com.jcrawler.core.AppContext;
import com.jcrawler.ui.MainStage;
import javafx.application.Application;
import javafx.stage.Stage;

public class JCrawlerApplication extends Application {

    private AppContext appContext;

    @Override
    public void init() {
        System.out.println("=== JCrawlerApplication.init() starting ===");
        try {
            // Initialize application context
            appContext = AppContext.getInstance();
            System.out.println("=== AppContext initialized successfully ===");
        } catch (Exception e) {
            System.err.println("=== FATAL: Failed to initialize AppContext ===");
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public void start(Stage primaryStage) {
        System.out.println("=== JCrawlerApplication.start() starting ===");
        try {
            // Create and start the main stage
            MainStage mainStage = new MainStage(
                appContext.getCrawlerService(),
                appContext.getCrawlSessionDao(),
                appContext.getDownloadedFileDao(),
                appContext.getExportService()
            );
            System.out.println("=== MainStage created, calling start() ===");
            mainStage.start(primaryStage);
            System.out.println("=== GUI should now be visible ===");
        } catch (Exception e) {
            System.err.println("=== FATAL: Failed to start GUI ===");
            e.printStackTrace();
            throw e;
        }
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
