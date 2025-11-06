package com.jcrawler.engine;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
public class LinkExtractor {

    private static final List<String> ATTACHMENT_EXTENSIONS = List.of(
            ".pdf", ".docx", ".doc", ".xlsx", ".xls", ".zip", ".tar.gz",
            ".png", ".jpg", ".jpeg", ".gif", ".svg", ".csv", ".txt"
    );

    public Set<String> extractLinks(Document document, String baseUrl, String baseDomain) {
        Set<String> links = new HashSet<>();
        Elements anchorElements = document.select("a[href]");

        log.info("=== LINK EXTRACTION ===");
        log.info("Base URL: {}", baseUrl);
        log.info("Base Domain: {} (normalized: {})", baseDomain, normalizeHost(baseDomain));
        log.info("Total <a> tags with href: {}", anchorElements.size());

        for (Element element : anchorElements) {
            String href = element.attr("abs:href");
            if (href.isEmpty()) {
                continue;
            }

            String normalizedUrl = normalizeUrl(href, baseUrl);
            boolean sameDomain = normalizedUrl != null && isSameDomain(normalizedUrl, baseDomain);

            // Extract domain from the link for debugging
            String linkDomain = normalizedUrl != null ? extractDomain(normalizedUrl) : "null";

            if (anchorElements.size() <= 10) { // Only log details if few links
                log.info("  Link: {} -> normalized: {} (domain: {}) [same domain: {}]",
                    href, normalizedUrl, linkDomain, sameDomain);
            }

            if (sameDomain) {
                links.add(normalizedUrl);
            }
        }

        log.info("✅ Found {} internal links to crawl", links.size());
        log.info("=======================");
        return links;
    }

    public Set<String> extractExternalUrls(Document document, String baseUrl, String baseDomain) {
        Set<String> externalUrls = new HashSet<>();
        Elements anchorElements = document.select("a[href]");

        log.debug("Extracting external URLs from {}", baseUrl);

        for (Element element : anchorElements) {
            String href = element.attr("abs:href");
            if (href.isEmpty()) {
                continue;
            }

            String normalizedUrl = normalizeUrl(href, baseUrl);
            // Collect URLs that are NOT on the same domain (external links)
            if (normalizedUrl != null && !isSameDomain(normalizedUrl, baseDomain)) {
                externalUrls.add(normalizedUrl);
            }
        }

        log.info("Found {} external URLs", externalUrls.size());

        return externalUrls;
    }

    public Set<String> extractAttachmentUrls(Document document, String baseUrl, String baseDomain, List<String> allowedExtensions) {
        Set<String> attachments = new HashSet<>();
        Elements anchorElements = document.select("a[href]");

        for (Element element : anchorElements) {
            String href = element.attr("abs:href");
            if (href.isEmpty()) {
                continue;
            }

            String normalizedUrl = normalizeUrl(href, baseUrl);
            if (normalizedUrl != null && isSameDomain(normalizedUrl, baseDomain) && isAttachment(normalizedUrl, allowedExtensions)) {
                attachments.add(normalizedUrl);
            }
        }

        return attachments;
    }

    public String normalizeUrl(String url, String baseUrl) {
        try {
            URL base = new URL(baseUrl);
            URL normalized = new URL(base, url);

            // Remove fragment
            String result = normalized.getProtocol() + "://" + normalized.getHost();
            if (normalized.getPort() != -1 && normalized.getPort() != normalized.getDefaultPort()) {
                result += ":" + normalized.getPort();
            }
            result += normalized.getPath();
            if (normalized.getQuery() != null) {
                result += "?" + normalized.getQuery();
            }

            return result;
        } catch (MalformedURLException e) {
            return null;
        }
    }

    public boolean isSameDomain(String url, String baseDomain) {
        try {
            URL urlObj = new URL(url);
            String urlHost = normalizeHost(urlObj.getHost());
            String normalizedBaseDomain = normalizeHost(baseDomain);
            return urlHost.equals(normalizedBaseDomain);
        } catch (MalformedURLException e) {
            return false;
        }
    }

    private String normalizeHost(String host) {
        // Remove www. prefix for comparison to handle www/non-www variations
        if (host != null && host.startsWith("www.")) {
            return host.substring(4);
        }
        return host;
    }

    public boolean isAttachment(String url, List<String> allowedExtensions) {
        String lowerUrl = url.toLowerCase();
        // Remove query parameters for extension check
        String urlWithoutQuery = lowerUrl.split("\\?")[0];

        for (String ext : allowedExtensions) {
            if (urlWithoutQuery.endsWith(ext.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    public String extractDomain(String url) {
        try {
            URL urlObj = new URL(url);
            return normalizeHost(urlObj.getHost());
        } catch (MalformedURLException e) {
            return null;
        }
    }
}
