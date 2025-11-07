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
        initWebView();
    }

    private void initWebView() {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Exception> initError = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                log.info("========================================");
                log.info("Initializing JavaFX WebView for JavaScript rendering...");
                log.warn("NOTE: WebView processes pages sequentially. Concurrent threads setting will be ignored for JavaScript rendering.");

                webView = new WebView();
                log.info("WebView created");

                webEngine = webView.getEngine();
                log.info("WebEngine obtained");

                webEngine.setJavaScriptEnabled(true);
                log.info("JavaScript enabled");

                webEngine.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) JCrawler/1.0 WebView");
                log.info("User agent set");

                log.info("✅ JavaFX WebView initialized successfully");
                log.info("========================================");
            } catch (Exception e) {
                log.error("❌ CRITICAL: Failed to initialize WebView!", e);
                log.error("Exception type: {}", e.getClass().getName());
                log.error("Exception message: {}", e.getMessage());
                initError.set(e);
            } finally {
                latch.countDown();
            }
        });

        try {
            boolean initialized = latch.await(10, TimeUnit.SECONDS);
            if (!initialized) {
                log.error("CRITICAL: WebView initialization TIMEOUT after 10 seconds!");
                log.error("JavaFX platform may not be running properly");
            } else {
                log.info("WebView initialization completed successfully");
            }
        } catch (InterruptedException e) {
            log.error("WebView initialization interrupted", e);
            Thread.currentThread().interrupt();
        }
    }

    public synchronized PageProcessor.PageResult fetchAndParse(String url, Map<String, String> cookies, Long sessionId, String parentUrl, Integer depth) {
        long startTime = System.currentTimeMillis();
        PageProcessor.PageResult result = new PageProcessor.PageResult();

        log.info("========================================");
        log.info("JavaScriptPageProcessor.fetchAndParse called");
        log.info("URL: {}", url);
        log.info("WebEngine status: {}", webEngine == null ? "NULL" : "INITIALIZED");
        log.info("========================================");

        if (webEngine == null) {
            result.success = false;
            result.errorMessage = "WebView not initialized - JavaFX may have failed to start";
            result.statusCode = 0;
            log.error("CRITICAL: WebEngine is NULL! WebView initialization failed!");
            buildPageEntity(result, url, sessionId, parentUrl, depth, startTime);
            return result;
        }

        CountDownLatch loadLatch = new CountDownLatch(1);
        AtomicReference<String> htmlRef = new AtomicReference<>();
        AtomicReference<String> titleRef = new AtomicReference<>();
        AtomicReference<Exception> errorRef = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                // Set up ONE-TIME load listener
                webEngine.getLoadWorker().stateProperty().addListener(new javafx.beans.value.ChangeListener<Worker.State>() {
                    @Override
                    public void changed(javafx.beans.value.ObservableValue<? extends Worker.State> obs, Worker.State oldState, Worker.State newState) {
                        if (newState == Worker.State.SUCCEEDED) {
                            // Remove listener to prevent multiple triggers
                            webEngine.getLoadWorker().stateProperty().removeListener(this);

                            // Wait for React to render (in background thread)
                            new Thread(() -> {
                                try {
                                    // Wait longer for React to fully render
                                    Thread.sleep(3000);

                                    // Get rendered HTML on FX thread
                                    Platform.runLater(() -> {
                                        try {
                                            String html = (String) webEngine.executeScript("document.documentElement.outerHTML");
                                            htmlRef.set(html);
                                            titleRef.set(webEngine.getTitle());

                                            log.info("WebView rendered page, HTML length: {}", html != null ? html.length() : 0);
                                            if (html != null && html.length() < 5000) {
                                                log.info("Full HTML content:\n{}", html);
                                            } else if (html != null) {
                                                log.info("HTML preview (first 1000 chars):\n{}", html.substring(0, Math.min(1000, html.length())));
                                            }
                                        } catch (Exception e) {
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
                        } else if (newState == Worker.State.FAILED) {
                            webEngine.getLoadWorker().stateProperty().removeListener(this);
                            errorRef.set(new RuntimeException("Failed to load page"));
                            loadLatch.countDown();
                        }
                    }
                });

                // Load the URL
                webEngine.load(url);

            } catch (Exception e) {
                errorRef.set(e);
                loadLatch.countDown();
            }
        });

        try {
            // Wait for page to load (max 30 seconds)
            boolean loaded = loadLatch.await(30, TimeUnit.SECONDS);

            if (!loaded) {
                result.success = false;
                result.errorMessage = "Page load timeout after 30 seconds";
                result.statusCode = 0;
                log.error("WebView page load TIMEOUT for URL: {}", url);
            } else if (errorRef.get() != null) {
                result.success = false;
                result.errorMessage = "WebView error: " + errorRef.get().getMessage();
                result.statusCode = 0;
                log.error("WebView error for URL: {}", url, errorRef.get());
            } else {
                String html = htmlRef.get();
                if (html != null && !html.isEmpty()) {
                    result.document = Jsoup.parse(html, url);
                    result.title = titleRef.get();
                    result.contentHash = calculateHash(html);
                    result.success = true;
                    result.statusCode = 200;

                    int linkCount = result.document.select("a[href]").size();
                    log.info("✅ WebView rendered page successfully!");
                    log.info("   HTML length: {} chars", html.length());
                    log.info("   Title: {}", titleRef.get());
                    log.info("   Found {} <a> tags with href attribute", linkCount);

                    // Log HTML preview for debugging
                    if (html.length() < 500) {
                        log.info("   Full HTML:\n{}", html);
                    } else {
                        log.info("   HTML preview (first 500 chars):\n{}", html.substring(0, 500));
                    }

                    // Log first few links for debugging
                    if (linkCount > 0) {
                        var links = result.document.select("a[href]");
                        log.info("   Sample links:");
                        for (int i = 0; i < Math.min(5, linkCount); i++) {
                            var link = links.get(i);
                            log.info("     - href=\"{}\" abs=\"{}\"", link.attr("href"), link.attr("abs:href"));
                        }
                    } else {
                        log.warn("   ⚠️ NO LINKS FOUND!");
                        log.warn("   This could mean:");
                        log.warn("   1. The page has no <a> tags");
                        log.warn("   2. React/JS hasn't finished rendering yet (increase wait time)");
                        log.warn("   3. WebView doesn't support the JavaScript on this page");
                    }
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
