package com.jcrawler;

import com.jcrawler.ui.MainStage;
import javafx.application.Application;
import javafx.stage.Stage;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class JCrawlerApplication extends Application {

    private ConfigurableApplicationContext springContext;

    @Override
    public void init() {
        // Initialize Spring Boot context without web server
        springContext = new SpringApplicationBuilder(JCrawlerApplication.class)
                .headless(false)
                .run();
    }

    @Override
    public void start(Stage primaryStage) {
        // Get the MainStage bean and set up the UI
        MainStage mainStage = springContext.getBean(MainStage.class);
        mainStage.start(primaryStage);
    }

    @Override
    public void stop() {
        // Close Spring context when application closes
        springContext.close();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
