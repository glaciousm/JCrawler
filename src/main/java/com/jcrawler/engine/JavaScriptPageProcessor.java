package com.jcrawler.engine;

import com.jcrawler.model.Page;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.Cookie;
import com.microsoft.playwright.options.WaitUntilState;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
public class JavaScriptPageProcessor {

    private Playwright playwright;
    private Browser browser;
    private BrowserContext sharedContext;

    public JavaScriptPageProcessor() {
        // Initialize Playwright lazily
    }

    private synchronized void initPlaywright() {
        if (playwright == null) {
            log.info("Initializing Playwright...");
            playwright = Playwright.create();
            browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                    .setHeadless(true)
                    .setArgs(java.util.Arrays.asList("--no-sandbox", "--disable-dev-shm-usage")));

            // Create a shared context for all pages
            sharedContext = browser.newContext(new Browser.NewContextOptions()
                    .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) JCrawler/1.0 Playwright"));

            log.info("Playwright initialized successfully");
        }
    }

    public synchronized PageProcessor.PageResult fetchAndParse(String url, Map<String, String> cookies, Long sessionId, String parentUrl, Integer depth) {
        if (playwright == null) {
            initPlaywright();
        }

        long startTime = System.currentTimeMillis();
        PageProcessor.PageResult result = new PageProcessor.PageResult();

        com.microsoft.playwright.Page page = null;

        try {
            // Create new page in shared context
            page = sharedContext.newPage();

            // Navigate to URL and wait for network idle
            Response response = page.navigate(url, new com.microsoft.playwright.Page.NavigateOptions()
                    .setWaitUntil(WaitUntilState.NETWORKIDLE)
                    .setTimeout(30000));

            result.statusCode = response.status();
            result.success = response.ok();

            if (response.ok()) {
                // Wait a bit for React to render
                page.waitForTimeout(1000);

                // Get the fully rendered HTML
                String html = page.content();
                result.document = Jsoup.parse(html, url);
                result.title = page.title();
                result.contentHash = calculateHash(html);

                log.debug("Fetched {} with Playwright: {} links found", url, result.document.select("a[href]").size());
            } else {
                result.errorMessage = "HTTP " + response.status() + ": " + response.statusText();
            }

        } catch (Exception e) {
            log.error("Failed to fetch URL with Playwright: {}", url, e);
            result.success = false;
            result.errorMessage = e.getMessage();
        } finally {
            // Clean up page only
            if (page != null) {
                try {
                    page.close();
                } catch (Exception e) {
                    log.warn("Error closing page", e);
                }
            }
        }

        long endTime = System.currentTimeMillis();
        result.processingTime = endTime - startTime;

        // Build Page entity
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

        return result;
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
        if (sharedContext != null) {
            try {
                sharedContext.close();
            } catch (Exception e) {
                log.warn("Error closing shared context", e);
            }
        }
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }
}
