package com.jcrawler.service;

import com.jcrawler.dao.*;
import com.jcrawler.dto.CrawlRequest;
import com.jcrawler.dto.CrawlResponse;
import com.jcrawler.engine.CrawlerEngine;
import com.jcrawler.engine.LinkExtractor;
import com.jcrawler.model.*;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class CrawlerService {

    private final CrawlSessionDao sessionDao;
    private final PageDao pageDao;
    private final NavigationFlowDao flowDao;
    private final ExtractionRuleDao ruleDao;
    private final DownloadedFileDao downloadedFileDao;
    private final ExternalUrlDao externalUrlDao;
    private final InternalLinkDao internalLinkDao;
    private final CrawlerEngine crawlerEngine;
    private final LinkExtractor linkExtractor;
    private final ExtractionService extractionService;
    private final DownloadService downloadService;

    public CrawlerService(CrawlSessionDao sessionDao, PageDao pageDao, NavigationFlowDao flowDao,
                         ExtractionRuleDao ruleDao, DownloadedFileDao downloadedFileDao,
                         ExternalUrlDao externalUrlDao, InternalLinkDao internalLinkDao,
                         CrawlerEngine crawlerEngine, LinkExtractor linkExtractor,
                         ExtractionService extractionService, DownloadService downloadService) {
        this.sessionDao = sessionDao;
        this.pageDao = pageDao;
        this.flowDao = flowDao;
        this.ruleDao = ruleDao;
        this.downloadedFileDao = downloadedFileDao;
        this.externalUrlDao = externalUrlDao;
        this.internalLinkDao = internalLinkDao;
        this.crawlerEngine = crawlerEngine;
        this.linkExtractor = linkExtractor;
        this.extractionService = extractionService;
        this.downloadService = downloadService;
    }

    // UI callback interface for real-time updates
    public interface UICallback {
        void onLog(String message);
    }

    private UICallback uiCallback;

    public void setUICallback(UICallback callback) {
        this.uiCallback = callback;
    }

    private void logToUI(String message) {
        if (uiCallback != null) {
            uiCallback.onLog(message);
        }
        log.info(message);
    }

    public CrawlResponse startCrawl(CrawlRequest request) {
        logToUI("Starting crawl for URL: " + request.getStartUrl());
        logToUI("JavaScript rendering: " + (request.getEnableJavaScript() ? "ENABLED" : "DISABLED"));

        // Extract domain
        String baseDomain = linkExtractor.extractDomain(request.getStartUrl());
        if (baseDomain == null) {
            throw new IllegalArgumentException("Invalid start URL");
        }

        // Create crawl session
        CrawlSession session = CrawlSession.builder()
                .startUrl(request.getStartUrl())
                .baseDomain(baseDomain)
                .status(CrawlSession.CrawlStatus.INITIALIZED)
                .startTime(LocalDateTime.now())
                .sessionCookies(request.getCookies())
                .maxDepth(request.getMaxDepth())
                .maxPages(request.getMaxPages())
                .requestDelay(request.getRequestDelay())
                .concurrentThreads(request.getConcurrentThreads())
                .enableJavaScript(request.getEnableJavaScript())
                .build();

        // Handle auth config
        if (request.getAuthConfig() != null) {
            Map<String, String> authConfig = new HashMap<>();
            authConfig.put("loginUrl", request.getAuthConfig().getLoginUrl());
            authConfig.put("username", request.getAuthConfig().getUsername());
            authConfig.put("password", request.getAuthConfig().getPassword());
            session.setAuthConfig(authConfig);
        }

        session = sessionDao.save(session);
        final Long sessionId = session.getId();

        // Save extraction rules
        if (request.getExtractionRules() != null) {
            for (CrawlRequest.ExtractionRuleDto ruleDto : request.getExtractionRules()) {
                ExtractionRule rule = ExtractionRule.builder()
                        .sessionId(sessionId)
                        .ruleName(ruleDto.getRuleName())
                        .selectorType(ExtractionRule.SelectorType.valueOf(ruleDto.getSelectorType().toUpperCase()))
                        .selectorValue(ruleDto.getSelectorValue())
                        .attributeToExtract(ruleDto.getAttributeToExtract())
                        .enabled(true)
                        .build();
                ruleDao.save(rule);
            }
        }

        // Update session status to RUNNING using direct UPDATE
        sessionDao.updateStatus(sessionId, CrawlSession.CrawlStatus.RUNNING);

        // Start crawl asynchronously
        crawlerEngine.startCrawl(session, new CrawlerEngine.CrawlCallback() {
            @Override
            public void onPageDiscovered(Page page) {
                logToUI("📄 Discovered page: " + page.getUrl() + " (depth: " + page.getDepthLevel() + ")");
                page = pageDao.save(page);

                // Extract data if rules exist
                List<ExtractionRule> rules = ruleDao.findBySessionIdAndEnabled(sessionId, true);
                if (!rules.isEmpty()) {
                    extractionService.extractData(page, rules);
                }
            }

            @Override
            public void onFlowDiscovered(List<String> flowPath, Integer depth) {
                NavigationFlow flow = NavigationFlow.builder()
                        .sessionId(sessionId)
                        .flowPath(flowPath)
                        .depth(depth)
                        .startUrl(flowPath.get(0))
                        .endUrl(flowPath.get(flowPath.size() - 1))
                        .discoveredAt(LocalDateTime.now())
                        .build();
                flow = flowDao.save(flow);

                // Update session total flows
                sessionDao.incrementTotalFlows(sessionId);
            }

            @Override
            public void onAttachmentFound(String url, Long pageId) {
                try {
                    // Extract filename and extension from URL
                    String fileName = url.substring(url.lastIndexOf('/') + 1);
                    String fileExtension = "";
                    if (fileName.contains(".")) {
                        fileExtension = fileName.substring(fileName.lastIndexOf('.'));
                    }

                    // Save file URL reference without downloading
                    DownloadedFile downloadedFile = DownloadedFile.builder()
                            .sessionId(sessionId)
                            .pageId(pageId)
                            .url(url)
                            .fileName(fileName)
                            .fileExtension(fileExtension)
                            .downloadedAt(LocalDateTime.now())
                            .downloadSuccess(true)
                            .build();
                    downloadedFileDao.save(downloadedFile);

                    // Update session total downloaded
                    sessionDao.incrementTotalDownloaded(sessionId);
                } catch (Exception e) {
                    log.error("Error saving file reference: {}", e.getMessage());
                }
            }

            @Override
            public void onExternalUrlFound(String url, String foundOnPage) {
                try {
                    logToUI("🌐 Found external link: " + url);
                    ExternalUrl externalUrl = ExternalUrl.builder()
                            .sessionId(sessionId)
                            .url(url)
                            .foundOnPage(foundOnPage)
                            .discoveredAt(LocalDateTime.now())
                            .domain(linkExtractor.extractDomain(url))
                            .build();
                    externalUrlDao.save(externalUrl);

                    // Update session total external URLs
                    sessionDao.incrementTotalExternalUrls(sessionId);
                } catch (Exception e) {
                    log.error("Error saving external URL: {}", e.getMessage());
                }
            }

            @Override
            public void onInternalLinkFound(String url, String foundOnPage) {
                try {
                    logToUI("🔗 Found internal link: " + url);
                    InternalLink internalLink = InternalLink.builder()
                            .sessionId(sessionId)
                            .url(url)
                            .foundOnPage(foundOnPage)
                            .discoveredAt(LocalDateTime.now())
                            .build();
                    internalLinkDao.save(internalLink);
                } catch (Exception e) {
                    log.error("Error saving internal link: {}", e.getMessage());
                }
            }

            @Override
            public void onComplete() {
                try {
                    Integer totalPages = pageDao.countBySessionId(sessionId).intValue();
                    Integer totalDownloaded = downloadedFileDao.countBySessionId(sessionId).intValue();
                    sessionDao.markCompleted(sessionId, totalPages, totalDownloaded);
                    logToUI("✅ Crawl completed! Total pages: " + totalPages);
                } catch (Exception ex) {
                    log.error("Error marking session as completed: {}", sessionId, ex);
                }
            }

            @Override
            public void onError(Exception e) {
                logToUI("❌ Error: " + e.getMessage());
                log.error("Crawl error for session: {}", sessionId, e);
                try {
                    sessionDao.markFailed(sessionId);
                } catch (Exception ex) {
                    log.error("Error marking session as failed: {}", sessionId, ex);
                }
            }
        });

        return buildCrawlResponse(session);
    }

    public CrawlResponse pauseCrawl(Long sessionId) {
        // Verify session exists
        CrawlSession session = sessionDao.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));

        crawlerEngine.pauseCrawl(sessionId);
        sessionDao.updateStatus(sessionId, CrawlSession.CrawlStatus.PAUSED);

        // Fetch updated session for response
        session = sessionDao.findById(sessionId).orElse(session);
        return buildCrawlResponse(session);
    }

    public CrawlResponse resumeCrawl(Long sessionId) {
        // Verify session exists
        CrawlSession session = sessionDao.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));

        crawlerEngine.resumeCrawl(sessionId);
        sessionDao.updateStatus(sessionId, CrawlSession.CrawlStatus.RUNNING);

        // Fetch updated session for response
        session = sessionDao.findById(sessionId).orElse(session);
        return buildCrawlResponse(session);
    }

    public CrawlResponse stopCrawl(Long sessionId) {
        // Verify session exists
        CrawlSession session = sessionDao.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));

        crawlerEngine.stopCrawl(sessionId);
        sessionDao.markStopped(sessionId);

        // Fetch updated session for response
        session = sessionDao.findById(sessionId).orElse(session);
        return buildCrawlResponse(session);
    }

    public CrawlResponse getStatus(Long sessionId) {
        CrawlSession session = sessionDao.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));
        return buildCrawlResponse(session);
    }

    private CrawlResponse buildCrawlResponse(CrawlSession session) {
        return CrawlResponse.builder()
                .sessionId(session.getId())
                .status(session.getStatus().name())
                .startUrl(session.getStartUrl())
                .baseDomain(session.getBaseDomain())
                .startTime(session.getStartTime())
                .endTime(session.getEndTime())
                .totalPages(session.getTotalPages())
                .totalFlows(session.getTotalFlows())
                .totalExtracted(session.getTotalExtracted())
                .totalDownloaded(session.getTotalDownloaded())
                .totalExternalUrls(session.getTotalExternalUrls())
                .build();
    }
}
