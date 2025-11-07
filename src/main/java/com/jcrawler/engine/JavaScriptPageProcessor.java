package com.jcrawler.engine;

import com.jcrawler.model.Page;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class JavaScriptPageProcessor {

    private WebView webView;
    private WebEngine webEngine;
    private final Object lock = new Object();

    public JavaScriptPageProcessor() {
        // Initialize WebView on JavaFX thread
        try {
            initWebView();
        } catch (Exception e) {
            log.error("FATAL: Failed to initialize JavaScriptPageProcessor", e);
            throw new RuntimeException("JavaScriptPageProcessor initialization failed", e);
        }
    }

    private void initWebView() {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                log.info("Initializing JavaFX WebView for JavaScript rendering...");
                log.warn("NOTE: WebView processes pages sequentially. Concurrent threads setting will be ignored for JavaScript rendering.");
                webView = new WebView();
                webEngine = webView.getEngine();
                webEngine.setJavaScriptEnabled(true);
                webEngine.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) JCrawler/1.0 WebView");

                // Add console message listener to catch JavaScript errors
                webEngine.setOnAlert(event -> log.warn("JavaScript alert: {}", event.getData()));
                webEngine.setOnError(event -> log.error("WebEngine error: {}", event.getMessage()));

                log.info("JavaFX WebView initialized successfully");
            } catch (Exception e) {
                log.error("Failed to initialize WebView: {}", e.getMessage(), e);
            } finally {
                latch.countDown();
            }
        });

        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error("WebView initialization timeout", e);
            Thread.currentThread().interrupt();
        }
    }

    public synchronized PageProcessor.PageResult fetchAndParse(String url, Map<String, String> cookies, Long sessionId, String parentUrl, Integer depth) {
        log.info("=== JavaScriptPageProcessor.fetchAndParse called for URL: {}", url);
        long startTime = System.currentTimeMillis();
        PageProcessor.PageResult result = new PageProcessor.PageResult();

        if (webEngine == null) {
            result.success = false;
            result.errorMessage = "WebView not initialized";
            result.statusCode = 0;
            buildPageEntity(result, url, sessionId, parentUrl, depth, startTime);
            return result;
        }

        CountDownLatch loadLatch = new CountDownLatch(1);
        AtomicReference<String> htmlRef = new AtomicReference<>();
        AtomicReference<String> titleRef = new AtomicReference<>();
        AtomicReference<Exception> errorRef = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                // Load the URL
                webEngine.load(url);
                log.info("WebEngine.load() called for: {}", url);

                // Wait for initial page load, then extract HTML after fixed delay
                new Thread(() -> {
                    try {
                        // Poll for document to have content (max 20 seconds)
                        int attempts = 0;
                        int maxAttempts = 40; // 40 * 500ms = 20 seconds

                        while (attempts < maxAttempts) {
                            Thread.sleep(500);
                            attempts++;

                            // Check if body has any content on FX thread
                            AtomicReference<Integer> bodyLengthRef = new AtomicReference<>(0);
                            CountDownLatch checkLatch = new CountDownLatch(1);

                            Platform.runLater(() -> {
                                try {
                                    Object bodyHTML = webEngine.executeScript("document.body ? document.body.innerHTML.length : 0");
                                    bodyLengthRef.set(((Number) bodyHTML).intValue());
                                } catch (Exception e) {
                                    // Ignore
                                } finally {
                                    checkLatch.countDown();
                                }
                            });

                            checkLatch.await(1, TimeUnit.SECONDS);

                            if (bodyLengthRef.get() > 10) {
                                log.info("Body content detected after {} ms", attempts * 500);
                                // Wait additional 2 seconds for React to fully render
                                Thread.sleep(2000);
                                break;
                            }
                        }

                        // Get rendered HTML on FX thread
                        Platform.runLater(() -> {
                            try {
                                // Check page state
                                String location = webEngine.getLocation();
                                String readyState = (String) webEngine.executeScript("document.readyState");
                                log.info("Page location: {}, readyState: {}", location, readyState);

                                String html = (String) webEngine.executeScript("document.documentElement.outerHTML");
                                htmlRef.set(html);

                                // Include diagnostic info in title
                                String actualTitle = webEngine.getTitle();
                                String diagnosticTitle = actualTitle + " [WebView location: " + location + ", readyState: " + readyState + "]";
                                titleRef.set(diagnosticTitle);

                                log.info("WebView rendered page, HTML length: {}, Title: {}", html != null ? html.length() : 0, webEngine.getTitle());
                                if (html != null && html.length() < 5000) {
                                    log.info("HTML content preview: {}", html.substring(0, Math.min(500, html.length())));
                                }

                                // Check if there's a React root element
                                Object rootElement = webEngine.executeScript("document.getElementById('root')");
                                log.info("React root element present: {}", rootElement != null);
                                if (rootElement != null) {
                                    String rootHTML = (String) webEngine.executeScript("document.getElementById('root').innerHTML");
                                    log.info("Root innerHTML length: {}", rootHTML != null ? rootHTML.length() : 0);
                                }
                            } catch (Exception e) {
                                log.error("Failed to extract HTML", e);
                                errorRef.set(e);
                            } finally {
                                loadLatch.countDown();
                            }
                        });
                    } catch (Exception e) {
                        errorRef.set(e);
                        loadLatch.countDown();
                    }
                }).start();

            } catch (Exception e) {
                errorRef.set(e);
                loadLatch.countDown();
            }
        });

        try {
            // Wait for page to load (max 30 seconds)
            boolean loaded = loadLatch.await(30, TimeUnit.SECONDS);

            if (!loaded) {
                Worker.State finalState = webEngine.getLoadWorker().getState();
                result.success = false;
                result.errorMessage = "Page load timeout (stuck in state: " + finalState + ")";
                result.statusCode = 0;
                log.error("WebEngine timeout in state: {}", finalState);
            } else if (errorRef.get() != null) {
                result.success = false;
                result.errorMessage = errorRef.get().getMessage();
                result.statusCode = 0;
            } else {
                String html = htmlRef.get();
                if (html != null && !html.isEmpty()) {
                    result.document = Jsoup.parse(html, url);
                    result.title = titleRef.get();
                    result.contentHash = calculateHash(html);
                    result.success = true;
                    result.statusCode = 200;

                    log.debug("Fetched {} with WebView: {} links found", url, result.document.select("a[href]").size());
                } else {
                    result.success = false;
                    result.errorMessage = "Empty HTML response";
                    result.statusCode = 0;
                }
            }

        } catch (Exception e) {
            log.error("Failed to fetch URL with WebView: {}", url, e);
            result.success = false;
            result.errorMessage = e.getMessage();
            result.statusCode = 0;
        }

        buildPageEntity(result, url, sessionId, parentUrl, depth, startTime);
        log.info("=== JavaScriptPageProcessor.fetchAndParse COMPLETE. Success: {}, Error: {}, HTML length: {}",
                result.success, result.errorMessage, result.document != null ? result.document.html().length() : 0);
        return result;
    }

    private void buildPageEntity(PageProcessor.PageResult result, String url, Long sessionId, String parentUrl, Integer depth, long startTime) {
        long endTime = System.currentTimeMillis();
        result.processingTime = endTime - startTime;

        result.page = Page.builder()
                .sessionId(sessionId)
                .url(url)
                .parentUrl(parentUrl)
                .depthLevel(depth)
                .statusCode(result.statusCode)
                .title(result.title)
                .contentHash(result.contentHash)
                .visitedAt(LocalDateTime.now())
                .processingTimeMs(result.processingTime)
                .errorMessage(result.errorMessage)
                .processed(result.success)
                .build();
    }

    private String calculateHash(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest(content.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private String extractDomain(String url) {
        try {
            java.net.URL urlObj = new java.net.URL(url);
            return urlObj.getHost();
        } catch (Exception e) {
            return "localhost";
        }
    }

    public void shutdown() {
        // WebView cleanup is handled by JavaFX lifecycle
        log.info("JavaScriptPageProcessor shutdown complete");
    }
}
