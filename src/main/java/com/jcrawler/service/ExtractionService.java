package com.jcrawler.service;

import com.jcrawler.dto.ProgressUpdate;
import com.jcrawler.model.ExtractedData;
import com.jcrawler.model.ExtractionRule;
import com.jcrawler.model.Page;
import com.jcrawler.repository.CrawlSessionRepository;
import com.jcrawler.repository.ExtractedDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExtractionService {

    private final ExtractedDataRepository extractedDataRepository;
    private final CrawlSessionRepository sessionRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    public List<ExtractedData> extractData(Page page, List<ExtractionRule> rules) {
        List<ExtractedData> extractedDataList = new ArrayList<>();

        try {
            // Fetch the page content
            Request request = new Request.Builder()
                    .url(page.getUrl())
                    .build();

            Response response = httpClient.newCall(request).execute();
            if (!response.isSuccessful()) {
                log.warn("Failed to fetch page for extraction: {}", page.getUrl());
                return extractedDataList;
            }

            String html = response.body().string();
            Document document = Jsoup.parse(html, page.getUrl());
            response.close();

            // Apply each extraction rule
            for (ExtractionRule rule : rules) {
                if (!rule.getEnabled()) {
                    continue;
                }

                List<String> extractedValues = extractByRule(document, rule);

                for (String value : extractedValues) {
                    ExtractedData data = ExtractedData.builder()
                            .sessionId(page.getSessionId())
                            .pageId(page.getId())
                            .ruleId(rule.getId())
                            .extractedValue(value)
                            .extractedAt(LocalDateTime.now())
                            .build();

                    extractedDataList.add(extractedDataRepository.save(data));
                }

                // Update session total extracted count
                if (!extractedValues.isEmpty()) {
                    sessionRepository.findById(page.getSessionId()).ifPresent(session -> {
                        session.setTotalExtracted(session.getTotalExtracted() + extractedValues.size());
                        sessionRepository.save(session);
                    });

                    // Send WebSocket update
                    ProgressUpdate update = ProgressUpdate.dataExtracted(
                            page.getSessionId(),
                            rule.getRuleName(),
                            extractedValues.size(),
                            extractedValues.get(0)
                    );
                    messagingTemplate.convertAndSend("/topic/crawler/" + page.getSessionId() + "/progress", update);
                }
            }

        } catch (Exception e) {
            log.error("Error extracting data from page: {}", page.getUrl(), e);
        }

        return extractedDataList;
    }

    private List<String> extractByRule(Document document, ExtractionRule rule) {
        List<String> results = new ArrayList<>();

        try {
            if (rule.getSelectorType() == ExtractionRule.SelectorType.CSS) {
                results = extractByCss(document, rule);
            } else if (rule.getSelectorType() == ExtractionRule.SelectorType.XPATH) {
                results = extractByXPath(document, rule);
            }
        } catch (Exception e) {
            log.error("Error applying extraction rule {}: {}", rule.getRuleName(), e.getMessage());
        }

        return results;
    }

    private List<String> extractByCss(Document document, ExtractionRule rule) {
        List<String> results = new ArrayList<>();
        Elements elements = document.select(rule.getSelectorValue());

        for (Element element : elements) {
            String value = extractAttribute(element, rule.getAttributeToExtract());
            if (value != null && !value.trim().isEmpty()) {
                results.add(value.trim());
            }
        }

        return results;
    }

    private List<String> extractByXPath(Document document, ExtractionRule rule) {
        List<String> results = new ArrayList<>();

        try {
            // Convert JSoup Document to W3C Document for XPath
            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xpath = xPathFactory.newXPath();

            // Note: XPath with JSoup requires additional handling
            // For simplicity, we'll use CSS selectors as the primary method
            // Full XPath support would require converting to W3C DOM
            log.warn("XPath support is limited. Consider using CSS selectors instead.");

        } catch (Exception e) {
            log.error("XPath extraction failed: {}", e.getMessage());
        }

        return results;
    }

    private String extractAttribute(Element element, String attribute) {
        if (attribute == null || attribute.equalsIgnoreCase("text")) {
            return element.text();
        } else if (attribute.equalsIgnoreCase("html")) {
            return element.html();
        } else if (attribute.equalsIgnoreCase("href")) {
            return element.attr("abs:href");
        } else if (attribute.equalsIgnoreCase("src")) {
            return element.attr("abs:src");
        } else {
            return element.attr(attribute);
        }
    }

    public List<ExtractedData> getExtractedData(Long sessionId) {
        return extractedDataRepository.findBySessionId(sessionId);
    }

    public List<ExtractedData> getExtractedDataByRule(Long sessionId, Long ruleId) {
        return extractedDataRepository.findBySessionIdAndRuleId(sessionId, ruleId);
    }
}
