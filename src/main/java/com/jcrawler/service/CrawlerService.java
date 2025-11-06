package com.jcrawler.service;

import com.jcrawler.dto.CrawlRequest;
import com.jcrawler.dto.CrawlResponse;
import com.jcrawler.dto.ProgressUpdate;
import com.jcrawler.engine.CrawlerEngine;
import com.jcrawler.engine.LinkExtractor;
import com.jcrawler.model.*;
import com.jcrawler.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CrawlerService {

    private final CrawlSessionRepository sessionRepository;
    private final PageRepository pageRepository;
    private final NavigationFlowRepository flowRepository;
    private final ExtractionRuleRepository ruleRepository;
    private final DownloadedFileRepository downloadedFileRepository;
    private final ExternalUrlRepository externalUrlRepository;
    private final InternalLinkRepository internalLinkRepository;
    private final CrawlerEngine crawlerEngine;
    private final LinkExtractor linkExtractor;
    private final ExtractionService extractionService;
    private final DownloadService downloadService;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public CrawlResponse startCrawl(CrawlRequest request) {
        log.info("Starting crawl for URL: {}", request.getStartUrl());
        log.info("JavaScript rendering enabled: {}", request.getEnableJavaScript());

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

        session = sessionRepository.save(session);

        // Save extraction rules
        if (request.getExtractionRules() != null) {
            for (CrawlRequest.ExtractionRuleDto ruleDto : request.getExtractionRules()) {
                ExtractionRule rule = ExtractionRule.builder()
                        .sessionId(session.getId())
                        .ruleName(ruleDto.getRuleName())
                        .selectorType(ExtractionRule.SelectorType.valueOf(ruleDto.getSelectorType().toUpperCase()))
                        .selectorValue(ruleDto.getSelectorValue())
                        .attributeToExtract(ruleDto.getAttributeToExtract())
                        .enabled(true)
                        .build();
                ruleRepository.save(rule);
            }
        }

        // Update session status
        session.setStatus(CrawlSession.CrawlStatus.RUNNING);
        sessionRepository.save(session);

        final Long sessionId = session.getId();

        // Start crawl asynchronously
        crawlerEngine.startCrawl(session, new CrawlerEngine.CrawlCallback() {
            @Override
            public void onPageDiscovered(Page page) {
                page = pageRepository.save(page);

                // Extract data if rules exist
                List<ExtractionRule> rules = ruleRepository.findBySessionIdAndEnabled(sessionId, true);
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
                flow = flowRepository.save(flow);

                // Update session total flows
                CrawlSession s = sessionRepository.findById(sessionId).orElse(null);
                if (s != null) {
                    s.setTotalFlows(s.getTotalFlows() + 1);
                    sessionRepository.save(s);
                }

                // Send WebSocket update
                ProgressUpdate update = ProgressUpdate.flowDiscovered(sessionId, flow.getId(), flowPath, s.getTotalFlows());
                messagingTemplate.convertAndSend("/topic/crawler/" + sessionId + "/progress", update);
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
                    downloadedFileRepository.save(downloadedFile);

                    // Update session total downloaded
                    CrawlSession s = sessionRepository.findById(sessionId).orElse(null);
                    if (s != null) {
                        s.setTotalDownloaded(s.getTotalDownloaded() + 1);
                        sessionRepository.save(s);

                        // Send WebSocket update
                        ProgressUpdate update = ProgressUpdate.fileDownloaded(sessionId, fileName, 0L, s.getTotalDownloaded());
                        messagingTemplate.convertAndSend("/topic/crawler/" + sessionId + "/progress", update);
                    }
                } catch (Exception e) {
                    log.error("Error saving file reference: {}", e.getMessage());
                }
            }

            @Override
            public void onExternalUrlFound(String url, String foundOnPage) {
                try {
                    ExternalUrl externalUrl = ExternalUrl.builder()
                            .sessionId(sessionId)
                            .url(url)
                            .foundOnPage(foundOnPage)
                            .discoveredAt(LocalDateTime.now())
                            .domain(linkExtractor.extractDomain(url))
                            .build();
                    externalUrlRepository.save(externalUrl);

                    // Update session total external URLs
                    CrawlSession s = sessionRepository.findById(sessionId).orElse(null);
                    if (s != null) {
                        s.setTotalExternalUrls(s.getTotalExternalUrls() + 1);
                        sessionRepository.save(s);

                        // Send WebSocket update
                        ProgressUpdate update = ProgressUpdate.externalUrlFound(sessionId, url, s.getTotalExternalUrls());
                        messagingTemplate.convertAndSend("/topic/crawler/" + sessionId + "/progress", update);
                    }
                } catch (Exception e) {
                    log.error("Error saving external URL: {}", e.getMessage());
                }
            }

            @Override
            public void onInternalLinkFound(String url, String foundOnPage) {
                try {
                    InternalLink internalLink = InternalLink.builder()
                            .sessionId(sessionId)
                            .url(url)
                            .foundOnPage(foundOnPage)
                            .discoveredAt(LocalDateTime.now())
                            .build();
                    internalLinkRepository.save(internalLink);
                } catch (Exception e) {
                    log.error("Error saving internal link: {}", e.getMessage());
                }
            }

            @Override
            public void onComplete() {
                CrawlSession s = sessionRepository.findById(sessionId).orElse(null);
                if (s != null) {
                    s.setStatus(CrawlSession.CrawlStatus.COMPLETED);
                    s.setEndTime(LocalDateTime.now());
                    s.setTotalPages(pageRepository.countBySessionId(sessionId).intValue());
                    s.setTotalDownloaded(downloadedFileRepository.countBySessionId(sessionId).intValue());
                    sessionRepository.save(s);

                    log.info("Crawl completed for session: {}", sessionId);

                    // Send completion update
                    ProgressUpdate update = ProgressUpdate.builder()
                            .type(ProgressUpdate.ProgressType.CRAWL_COMPLETED)
                            .sessionId(sessionId)
                            .timestamp(LocalDateTime.now())
                            .data(Map.of("message", "Crawl completed successfully"))
                            .build();
                    messagingTemplate.convertAndSend("/topic/crawler/" + sessionId + "/progress", update);
                }
            }

            @Override
            public void onError(Exception e) {
                log.error("Crawl error for session: {}", sessionId, e);
                CrawlSession s = sessionRepository.findById(sessionId).orElse(null);
                if (s != null) {
                    s.setStatus(CrawlSession.CrawlStatus.FAILED);
                    s.setEndTime(LocalDateTime.now());
                    sessionRepository.save(s);
                }

                // Send error update
                ProgressUpdate update = ProgressUpdate.builder()
                        .type(ProgressUpdate.ProgressType.CRAWL_ERROR)
                        .sessionId(sessionId)
                        .timestamp(LocalDateTime.now())
                        .data(Map.of("error", e.getMessage()))
                        .build();
                messagingTemplate.convertAndSend("/topic/crawler/" + sessionId + "/progress", update);
            }
        });

        return buildCrawlResponse(session);
    }

    @Transactional
    public CrawlResponse pauseCrawl(Long sessionId) {
        CrawlSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));

        crawlerEngine.pauseCrawl(sessionId);
        session.setStatus(CrawlSession.CrawlStatus.PAUSED);
        sessionRepository.save(session);

        return buildCrawlResponse(session);
    }

    @Transactional
    public CrawlResponse resumeCrawl(Long sessionId) {
        CrawlSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));

        crawlerEngine.resumeCrawl(sessionId);
        session.setStatus(CrawlSession.CrawlStatus.RUNNING);
        sessionRepository.save(session);

        return buildCrawlResponse(session);
    }

    @Transactional
    public CrawlResponse stopCrawl(Long sessionId) {
        CrawlSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));

        crawlerEngine.stopCrawl(sessionId);
        session.setStatus(CrawlSession.CrawlStatus.STOPPED);
        session.setEndTime(LocalDateTime.now());
        sessionRepository.save(session);

        return buildCrawlResponse(session);
    }

    public CrawlResponse getStatus(Long sessionId) {
        CrawlSession session = sessionRepository.findById(sessionId)
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
