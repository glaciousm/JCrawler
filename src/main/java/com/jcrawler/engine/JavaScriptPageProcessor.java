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
        Platform.runLater(() -> {
            try {
                log.info("Initializing JavaFX WebView for JavaScript rendering...");
                log.warn("NOTE: WebView processes pages sequentially. Concurrent threads setting will be ignored for JavaScript rendering.");
                webView = new WebView();
                webEngine = webView.getEngine();
                webEngine.setJavaScriptEnabled(true);
                webEngine.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) JCrawler/1.0 WebView");
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
                result.errorMessage = "Page load timeout";
                result.statusCode = 0;
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

                    int linkCount = result.document.select("a[href]").size();
                    log.info("✅ WebView rendered page successfully!");
                    log.info("   HTML length: {} chars", html.length());
                    log.info("   Found {} <a> tags with href attribute", linkCount);

                    // Log first few links for debugging
                    if (linkCount > 0) {
                        var links = result.document.select("a[href]");
                        log.info("   Sample links:");
                        for (int i = 0; i < Math.min(3, linkCount); i++) {
                            var link = links.get(i);
                            log.info("     - {}", link.attr("abs:href"));
                        }
                    } else {
                        log.warn("   ⚠️ NO LINKS FOUND! The page might not have rendered properly or has no links.");
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
