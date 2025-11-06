package com.jcrawler.core;

import com.jcrawler.dao.*;
import com.jcrawler.engine.CrawlerEngine;
import com.jcrawler.engine.LinkExtractor;
import com.jcrawler.engine.PageProcessor;
import com.jcrawler.service.*;
import lombok.Getter;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Application context - manages all dependencies
 * Replaces Spring's dependency injection
 */
@Getter
public class AppContext {

    private static AppContext instance;

    // DAOs
    private final CrawlSessionDao crawlSessionDao;
    private final PageDao pageDao;
    private final NavigationFlowDao navigationFlowDao;
    private final ExtractionRuleDao extractionRuleDao;
    private final ExtractedDataDao extractedDataDao;
    private final DownloadedFileDao downloadedFileDao;
    private final ExternalUrlDao externalUrlDao;
    private final InternalLinkDao internalLinkDao;

    // Engine components
    private final LinkExtractor linkExtractor;
    private final PageProcessor pageProcessor;
    private final CrawlerEngine crawlerEngine;

    // Services
    private final ExtractionService extractionService;
    private final DownloadService downloadService;
    private final ExportService exportService;
    private final CrawlerService crawlerService;

    // Thread pool for async execution
    private final ExecutorService executorService;

    private AppContext() {
        // Initialize Hibernate
        HibernateConfig.getSessionFactory();

        // Initialize DAOs
        this.crawlSessionDao = new CrawlSessionDao();
        this.pageDao = new PageDao();
        this.navigationFlowDao = new NavigationFlowDao();
        this.extractionRuleDao = new ExtractionRuleDao();
        this.extractedDataDao = new ExtractedDataDao();
        this.downloadedFileDao = new DownloadedFileDao();
        this.externalUrlDao = new ExternalUrlDao();
        this.internalLinkDao = new InternalLinkDao();

        // Initialize executor service
        this.executorService = Executors.newFixedThreadPool(20);

        // Initialize engine components
        this.linkExtractor = new LinkExtractor();
        this.pageProcessor = new PageProcessor();
        this.crawlerEngine = new CrawlerEngine(
            linkExtractor,
            pageProcessor,
            executorService
        );

        // Initialize services
        this.extractionService = new ExtractionService(extractedDataDao);
        this.downloadService = new DownloadService(downloadedFileDao);
        this.exportService = new ExportService(
            crawlSessionDao,
            pageDao,
            navigationFlowDao,
            extractedDataDao,
            downloadedFileDao,
            externalUrlDao,
            internalLinkDao
        );
        this.crawlerService = new CrawlerService(
            crawlSessionDao,
            pageDao,
            navigationFlowDao,
            extractionRuleDao,
            downloadedFileDao,
            externalUrlDao,
            internalLinkDao,
            crawlerEngine,
            linkExtractor,
            extractionService,
            downloadService
        );
    }

    public static AppContext getInstance() {
        if (instance == null) {
            synchronized (AppContext.class) {
                if (instance == null) {
                    instance = new AppContext();
                }
            }
        }
        return instance;
    }

    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        HibernateConfig.shutdown();
    }
}
