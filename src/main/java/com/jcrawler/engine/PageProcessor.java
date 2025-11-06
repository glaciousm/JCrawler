package com.jcrawler.engine;

import com.jcrawler.model.Page;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class PageProcessor {

    private final OkHttpClient httpClient;

    public PageProcessor() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .followRedirects(true)
                .build();
    }

    public PageResult fetchAndParse(String url, Map<String, String> cookies, Long sessionId, String parentUrl, Integer depth) {
        long startTime = System.currentTimeMillis();
        PageResult result = new PageResult();

        try {
            Request.Builder requestBuilder = new Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) JCrawler/1.0");

            // Add cookies if provided
            if (cookies != null && !cookies.isEmpty()) {
                StringBuilder cookieHeader = new StringBuilder();
                cookies.forEach((key, value) -> {
                    if (cookieHeader.length() > 0) {
                        cookieHeader.append("; ");
                    }
                    cookieHeader.append(key).append("=").append(value);
                });
                requestBuilder.header("Cookie", cookieHeader.toString());
            }

            Response response = httpClient.newCall(requestBuilder.build()).execute();

            result.statusCode = response.code();
            result.success = response.isSuccessful();

            if (response.isSuccessful() && response.body() != null) {
                String html = response.body().string();
                result.document = Jsoup.parse(html, url);
                result.title = result.document.title();
                result.contentHash = calculateHash(html);
            } else {
                result.errorMessage = "HTTP " + response.code() + ": " + response.message();
            }

            response.close();

        } catch (IOException e) {
            log.error("Failed to fetch URL: {}", url, e);
            result.success = false;
            result.errorMessage = e.getMessage();
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

    public static class PageResult {
        public boolean success;
        public Integer statusCode;
        public Document document;
        public String title;
        public String contentHash;
        public String errorMessage;
        public Long processingTime;
        public Page page;
    }
}
