package com.jcrawler.engine;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class LinkExtractor {

    private static final List<String> ATTACHMENT_EXTENSIONS = List.of(
            ".pdf", ".docx", ".doc", ".xlsx", ".xls", ".zip", ".tar.gz",
            ".png", ".jpg", ".jpeg", ".gif", ".svg", ".csv", ".txt"
    );

    public Set<String> extractLinks(Document document, String baseUrl, String baseDomain) {
        Set<String> links = new HashSet<>();
        Elements anchorElements = document.select("a[href]");

        System.out.println("=== LINK EXTRACTION DEBUG ===");
        System.out.println("Base URL: " + baseUrl);
        System.out.println("Base Domain: " + baseDomain);
        System.out.println("Total anchor elements: " + anchorElements.size());

        for (Element element : anchorElements) {
            String href = element.attr("abs:href");
            if (href.isEmpty()) {
                continue;
            }

            String normalizedUrl = normalizeUrl(href, baseUrl);
            boolean sameDomain = normalizedUrl != null && isSameDomain(normalizedUrl, baseDomain);

            System.out.println("  Link: " + href);
            System.out.println("    Normalized: " + normalizedUrl);
            System.out.println("    Same domain: " + sameDomain);

            if (sameDomain) {
                links.add(normalizedUrl);
            }
        }

        System.out.println("Total internal links found: " + links.size());
        System.out.println("===========================");
        return links;
    }

    public Set<String> extractExternalUrls(Document document, String baseUrl, String baseDomain) {
        Set<String> externalUrls = new HashSet<>();
        Elements anchorElements = document.select("a[href]");

        System.out.println("=== EXTRACTING EXTERNAL URLs ===");
        System.out.println("Base domain: " + baseDomain);

        for (Element element : anchorElements) {
            String href = element.attr("abs:href");
            if (href.isEmpty()) {
                continue;
            }

            String normalizedUrl = normalizeUrl(href, baseUrl);
            // Collect URLs that are NOT on the same domain (external links)
            if (normalizedUrl != null && !isSameDomain(normalizedUrl, baseDomain)) {
                externalUrls.add(normalizedUrl);
                System.out.println("  EXTERNAL: " + normalizedUrl);
            }
        }

        System.out.println("Total external URLs found: " + externalUrls.size());
        System.out.println("================================");

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
            return urlObj.getHost().equals(baseDomain);
        } catch (MalformedURLException e) {
            return false;
        }
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
            return urlObj.getHost();
        } catch (MalformedURLException e) {
            return null;
        }
    }
}
