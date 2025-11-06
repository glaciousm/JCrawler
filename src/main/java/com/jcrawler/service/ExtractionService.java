package com.jcrawler.service;

import com.jcrawler.dao.ExtractedDataDao;
import com.jcrawler.model.ExtractedData;
import com.jcrawler.model.ExtractionRule;
import com.jcrawler.model.Page;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ExtractionService {

    private final ExtractedDataDao extractedDataDao;
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    public ExtractionService(ExtractedDataDao extractedDataDao) {
        this.extractedDataDao = extractedDataDao;
    }

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

                    extractedDataList.add(extractedDataDao.save(data));
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
        log.warn("XPath support is limited. Consider using CSS selectors instead.");
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
        return extractedDataDao.findBySessionId(sessionId);
    }

    public List<ExtractedData> getExtractedDataByRule(Long sessionId, Long ruleId) {
        return extractedDataDao.findBySessionIdAndRuleId(sessionId, ruleId);
    }
}
