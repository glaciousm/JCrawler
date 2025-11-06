package com.jcrawler.engine;

import com.jcrawler.model.CrawlSession;
import com.jcrawler.model.Page;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class CrawlerEngine {

    private final PageProcessor pageProcessor;
    private final JavaScriptPageProcessor jsPageProcessor;
    private final LinkExtractor linkExtractor;

    // Track running crawls
    private final Map<Long, CrawlContext> activeCrawls = new ConcurrentHashMap<>();

    public CrawlerEngine(LinkExtractor linkExtractor, PageProcessor pageProcessor, ExecutorService executorService) {
        this.linkExtractor = linkExtractor;
        this.pageProcessor = pageProcessor;
        this.jsPageProcessor = new JavaScriptPageProcessor();
    }

    public void startCrawl(CrawlSession session, CrawlCallback callback) {
        CrawlContext context = new CrawlContext(session);
        activeCrawls.put(session.getId(), context);

        ExecutorService executorService = Executors.newFixedThreadPool(session.getConcurrentThreads());

        // Start crawl in separate thread
        CompletableFuture.runAsync(() -> {
            try {
                crawl(context, executorService, callback);
            } catch (Exception e) {
                log.error("Crawl failed for session {}", session.getId(), e);
                callback.onError(e);
            } finally {
                executorService.shutdown();
                activeCrawls.remove(session.getId());
            }
        });
    }

    private void crawl(CrawlContext context, ExecutorService executor, CrawlCallback callback) throws InterruptedException {
        CrawlSession session = context.session;
        String startUrl = session.getStartUrl();
        String baseDomain = session.getBaseDomain();

        context.toVisit.add(new UrlDepthPair(startUrl, 0, null));

        long lastMetricsUpdate = System.currentTimeMillis();
        int lastPageCount = 0;

        while ((context.activeTasks.get() > 0 || !context.toVisit.isEmpty()) &&
               (session.getMaxPages() == 0 || context.visitedUrls.size() < session.getMaxPages())) {
            if (context.paused) {
                Thread.sleep(1000);
                continue;
            }

            if (context.stopped) {
                break;
            }

            UrlDepthPair current = context.toVisit.poll();
            if (current == null) {
                // No URLs in queue, but tasks may be running - wait a bit
                if (context.activeTasks.get() > 0) {
                    Thread.sleep(100);
                }
                continue;
            }

            if (context.visitedUrls.contains(current.url)) {
                continue;
            }

            if (session.getMaxDepth() > 0 && current.depth > session.getMaxDepth()) {
                continue;
            }

            // Check if URL is a file BEFORE adding to visitedUrls
            if (isFileUrl(current.url)) {
                log.debug("Skipping file URL (not a page): {}", current.url);
                continue;
            }

            context.visitedUrls.add(current.url);
            context.activeTasks.incrementAndGet();

            // Submit crawl task
            executor.submit(() -> {
                try {
                    processSinglePage(context, current, callback);

                    // Rate limiting
                    if (session.getRequestDelay() > 0) {
                        Thread.sleep((long) (session.getRequestDelay() * 1000));
                    }
                } catch (Exception e) {
                    log.error("Error processing page: {}", current.url, e);
                } finally {
                    context.activeTasks.decrementAndGet();
                }
            });

            // Metrics tracking (no WebSocket in desktop app)
            long now = System.currentTimeMillis();
            if (now - lastMetricsUpdate > 5000) {
                int currentPageCount = context.visitedUrls.size();
                double pagesPerSecond = (currentPageCount - lastPageCount) / 5.0;
                log.debug("Metrics: {}/s, {} threads, {} queued", pagesPerSecond, session.getConcurrentThreads(), context.toVisit.size());
                lastMetricsUpdate = now;
                lastPageCount = currentPageCount;
            }
        }

        // Wait for all tasks to complete
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.MINUTES);

        callback.onComplete();
    }

    private void processSinglePage(CrawlContext context, UrlDepthPair urlPair, CrawlCallback callback) {
        CrawlSession session = context.session;

        // Choose between static HTML or JavaScript rendering
        PageProcessor.PageResult result;
        if (session.getEnableJavaScript()) {
            log.debug("Using JavaScript processor for URL: {}", urlPair.url);
            result = jsPageProcessor.fetchAndParse(
                    urlPair.url,
                    session.getSessionCookies(),
                    session.getId(),
                    urlPair.parentUrl,
                    urlPair.depth
            );
        } else {
            log.debug("Using static HTML processor for URL: {}", urlPair.url);
            result = pageProcessor.fetchAndParse(
                    urlPair.url,
                    session.getSessionCookies(),
                    session.getId(),
                    urlPair.parentUrl,
                    urlPair.depth
            );
        }

        // Save page
        callback.onPageDiscovered(result.page);

        // Progress updates removed (no WebSocket in desktop app)

        if (result.success && result.document != null) {
            // Extract links
            Set<String> links = linkExtractor.extractLinks(result.document, urlPair.url, session.getBaseDomain());
            for (String link : links) {
                // Track ALL discovered internal links (but exclude file URLs)
                if (!isFileUrl(link)) {
                    callback.onInternalLinkFound(link, urlPair.url);
                }

                if (!context.visitedUrls.contains(link)) {
                    context.toVisit.add(new UrlDepthPair(link, urlPair.depth + 1, urlPair.url));

                    // Track flow
                    List<String> flowPath = new ArrayList<>();
                    if (urlPair.parentUrl != null) {
                        flowPath.add(urlPair.parentUrl);
                    }
                    flowPath.add(urlPair.url);
                    flowPath.add(link);

                    callback.onFlowDiscovered(flowPath, urlPair.depth + 1);
                }
            }

            // Extract attachment URLs
            Set<String> attachments = linkExtractor.extractAttachmentUrls(
                    result.document,
                    urlPair.url,
                    session.getBaseDomain(),
                    List.of(".pdf", ".docx", ".doc", ".xlsx", ".xls", ".ppt", ".pptx",
                            ".zip", ".rar", ".7z", ".tar", ".gz",
                            ".csv", ".txt", ".json", ".xml",
                            ".png", ".jpg", ".jpeg", ".gif", ".svg", ".bmp", ".webp",
                            ".odt", ".ods", ".odp", ".rtf",
                            ".sql", ".log", ".md")
            );

            for (String attachment : attachments) {
                callback.onAttachmentFound(attachment, result.page.getId());
            }

            // Extract external URLs
            Set<String> externalUrls = linkExtractor.extractExternalUrls(
                    result.document,
                    urlPair.url,
                    session.getBaseDomain()
            );

            for (String externalUrl : externalUrls) {
                // Save all external URLs for each page (including duplicates across pages)
                callback.onExternalUrlFound(externalUrl, urlPair.url);
            }
        }
    }

    public void pauseCrawl(Long sessionId) {
        CrawlContext context = activeCrawls.get(sessionId);
        if (context != null) {
            context.paused = true;
            log.info("Crawl paused for session {}", sessionId);
        }
    }

    public void resumeCrawl(Long sessionId) {
        CrawlContext context = activeCrawls.get(sessionId);
        if (context != null) {
            context.paused = false;
            log.info("Crawl resumed for session {}", sessionId);
        }
    }

    public void stopCrawl(Long sessionId) {
        CrawlContext context = activeCrawls.get(sessionId);
        if (context != null) {
            context.stopped = true;
            log.info("Crawl stopped for session {}", sessionId);
        }
    }

    private boolean isFileUrl(String url) {
        // Check if URL ends with common file extensions that are not HTML pages
        String[] fileExtensions = {
            ".txt", ".xml", ".json", ".csv", ".pdf", ".doc", ".docx",
            ".xls", ".xlsx", ".ppt", ".pptx", ".zip", ".rar", ".tar", ".gz",
            ".jpg", ".jpeg", ".png", ".gif", ".svg", ".ico", ".webp",
            ".mp3", ".mp4", ".avi", ".mov", ".wav", ".ogg",
            ".css", ".js", ".woff", ".woff2", ".ttf", ".eot"
        };

        String lowerUrl = url.toLowerCase();
        for (String ext : fileExtensions) {
            if (lowerUrl.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    // Internal classes
    private static class CrawlContext {
        final CrawlSession session;
        final Queue<UrlDepthPair> toVisit = new ConcurrentLinkedQueue<>();
        final Set<String> visitedUrls = ConcurrentHashMap.newKeySet();
        final AtomicInteger activeTasks = new AtomicInteger(0);
        volatile boolean paused = false;
        volatile boolean stopped = false;

        CrawlContext(CrawlSession session) {
            this.session = session;
        }
    }

    private static class UrlDepthPair {
        final String url;
        final int depth;
        final String parentUrl;

        UrlDepthPair(String url, int depth, String parentUrl) {
            this.url = url;
            this.depth = depth;
            this.parentUrl = parentUrl;
        }
    }

    // Callback interface for crawl events
    public interface CrawlCallback {
        void onPageDiscovered(Page page);
        void onFlowDiscovered(List<String> flowPath, Integer depth);
        void onAttachmentFound(String url, Long pageId);
        void onExternalUrlFound(String url, String foundOnPage);
        void onInternalLinkFound(String url, String foundOnPage);
        void onComplete();
        void onError(Exception e);
    }
}
