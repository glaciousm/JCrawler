package com.jcrawler;

import com.jcrawler.core.AppContext;
import com.jcrawler.ui.MainStage;
import javafx.application.Application;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

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
            appContext.getPageDao(),
            appContext.getInternalLinkDao(),
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
        try {
            // Redirect System.out to a file for debugging
            File logFile = new File("jcrawler-console.log");
            FileOutputStream fos = new FileOutputStream(logFile);
            PrintStream ps = new PrintStream(fos, true);
            System.setOut(ps);
            System.setErr(ps);

            System.out.println("===========================================");
            System.out.println("JCrawler Debug Console");
            System.out.println("Log file: " + logFile.getAbsolutePath());
            System.out.println("===========================================\n");
        } catch (Exception e) {
            e.printStackTrace();
        }

        launch(args);
    }
}
