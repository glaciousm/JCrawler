package com.jcrawler.controller;

import com.jcrawler.dto.CrawlRequest;
import com.jcrawler.dto.CrawlResponse;
import com.jcrawler.dto.ExportRequest;
import com.jcrawler.model.*;
import com.jcrawler.repository.*;
import com.jcrawler.service.CrawlerService;
import com.jcrawler.service.DownloadService;
import com.jcrawler.service.ExportService;
import com.jcrawler.service.ExtractionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/crawler")
@RequiredArgsConstructor
public class CrawlerController {

    private final CrawlerService crawlerService;
    private final ExportService exportService;
    private final ExtractionService extractionService;
    private final DownloadService downloadService;
    private final PageRepository pageRepository;
    private final NavigationFlowRepository flowRepository;
    private final ExtractedDataRepository extractedDataRepository;
    private final DownloadedFileRepository downloadedFileRepository;
    private final ExternalUrlRepository externalUrlRepository;
    private final InternalLinkRepository internalLinkRepository;

    @PostMapping("/start")
    public ResponseEntity<CrawlResponse> startCrawl(@Valid @RequestBody CrawlRequest request) {
        CrawlResponse response = crawlerService.startCrawl(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/pause")
    public ResponseEntity<CrawlResponse> pauseCrawl(@PathVariable Long id) {
        CrawlResponse response = crawlerService.pauseCrawl(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/resume")
    public ResponseEntity<CrawlResponse> resumeCrawl(@PathVariable Long id) {
        CrawlResponse response = crawlerService.resumeCrawl(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/stop")
    public ResponseEntity<CrawlResponse> stopCrawl(@PathVariable Long id) {
        CrawlResponse response = crawlerService.stopCrawl(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<CrawlResponse> getStatus(@PathVariable Long id) {
        CrawlResponse response = crawlerService.getStatus(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/pages")
    public ResponseEntity<List<Page>> getPages(@PathVariable Long id) {
        List<Page> pages = pageRepository.findBySessionId(id);
        List<ExternalUrl> externalUrls = externalUrlRepository.findBySessionId(id);
        List<DownloadedFile> downloadedFiles = downloadedFileRepository.findBySessionId(id);

        // Populate structured data for each page
        for (Page page : pages) {
            // Child pages - use discovered internal links
            List<Page.ChildPage> childPages = new java.util.ArrayList<>();
            List<InternalLink> discoveredLinks = internalLinkRepository.findBySessionIdAndFoundOnPage(id, page.getUrl());
            for (InternalLink link : discoveredLinks) {
                childPages.add(new Page.ChildPage(link.getUrl()));
            }
            page.setChildPages(childPages);

            // External URLs
            List<Page.ExternalUrlInfo> urls = new java.util.ArrayList<>();
            for (ExternalUrl externalUrl : externalUrls) {
                if (externalUrl.getFoundOnPage() != null && externalUrl.getFoundOnPage().equals(page.getUrl())) {
                    urls.add(new Page.ExternalUrlInfo(externalUrl.getUrl()));
                }
            }
            page.setUrls(urls);

            // Downloaded files
            List<Page.DownloadInfo> downloads = new java.util.ArrayList<>();
            for (DownloadedFile file : downloadedFiles) {
                if (file.getPageId() != null && file.getPageId().equals(page.getId())) {
                    downloads.add(new Page.DownloadInfo(file.getFileName() != null ? file.getFileName() : file.getUrl()));
                }
            }
            page.setDownloads(downloads);
        }

        return ResponseEntity.ok(pages);
    }

    @GetMapping("/{id}/flows")
    public ResponseEntity<List<NavigationFlow>> getFlows(@PathVariable Long id) {
        List<NavigationFlow> flows = flowRepository.findBySessionId(id);
        return ResponseEntity.ok(flows);
    }

    @GetMapping("/{id}/extracted")
    public ResponseEntity<List<ExtractedData>> getExtractedData(@PathVariable Long id) {
        List<ExtractedData> data = extractionService.getExtractedData(id);
        return ResponseEntity.ok(data);
    }

    @GetMapping("/{id}/downloads")
    public ResponseEntity<List<DownloadedFile>> getDownloadedFiles(@PathVariable Long id) {
        List<DownloadedFile> files = downloadService.getDownloadedFiles(id);
        return ResponseEntity.ok(files);
    }

    @GetMapping("/{id}/external-urls")
    public ResponseEntity<List<ExternalUrl>> getExternalUrls(@PathVariable Long id) {
        List<ExternalUrl> urls = externalUrlRepository.findBySessionId(id);
        return ResponseEntity.ok(urls);
    }

    @PostMapping("/{id}/export")
    public ResponseEntity<Map<String, String>> export(@PathVariable Long id, @Valid @RequestBody ExportRequest request) {
        try {
            request.setSessionId(id);
            Map<String, String> exportedFiles = exportService.export(request);
            return ResponseEntity.ok(exportedFiles);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("EXPORT ERROR: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{sessionId}/download/{fileId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long sessionId, @PathVariable Long fileId) {
        try {
            DownloadedFile file = downloadService.getDownloadedFile(fileId);

            if (!file.getSessionId().equals(sessionId)) {
                return ResponseEntity.badRequest().build();
            }

            Path filePath = Paths.get(file.getLocalPath());
            Resource resource = new FileSystemResource(filePath);

            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFileName() + "\"")
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
