package com.jcrawler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class JCrawlerApplication {

    public static void main(String[] args) {
        SpringApplication.run(JCrawlerApplication.class, args);
    }
}
