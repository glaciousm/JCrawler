package com.jcrawler.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jcrawler.dto.ExportRequest;
import com.jcrawler.model.*;
import com.jcrawler.repository.*;
import com.opencsv.CSVWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExportService {

    private final CrawlSessionRepository sessionRepository;
    private final PageRepository pageRepository;
    private final NavigationFlowRepository flowRepository;
    private final ExtractedDataRepository extractedDataRepository;
    private final DownloadedFileRepository downloadedFileRepository;
    private final ExternalUrlRepository externalUrlRepository;
    private final InternalLinkRepository internalLinkRepository;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(SerializationFeature.INDENT_OUTPUT);

    public Map<String, String> export(ExportRequest request) throws Exception {
        Map<String, String> exportedFiles = new HashMap<>();

        CrawlSession session = sessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));

        // Create export directory
        Path exportDir = Paths.get("exports", "session_" + session.getId() + "_" + System.currentTimeMillis());
        Files.createDirectories(exportDir);

        // Collect data
        ExportData exportData = collectExportData(session, request);

        // Export to requested formats
        for (ExportRequest.ExportFormat format : request.getFormats()) {
            String filePath = switch (format) {
                case JSON -> exportToJson(exportData, exportDir);
                case CSV -> exportToCsv(exportData, exportDir);
                case EXCEL -> exportToExcel(exportData, exportDir);
                case PDF -> exportToPdf(exportData, exportDir);
            };
            exportedFiles.put(format.name(), filePath);
        }

        return exportedFiles;
    }

    private ExportData collectExportData(CrawlSession session, ExportRequest request) {
        ExportData data = new ExportData();
        data.session = session;

        if (request.getIncludePages()) {
            data.pages = pageRepository.findBySessionId(session.getId());
            data.externalUrls = externalUrlRepository.findBySessionId(session.getId());
            data.downloadedFiles = downloadedFileRepository.findBySessionId(session.getId());

            // Populate structured data for each page
            for (Page page : data.pages) {
                // Child pages - use discovered internal links
                List<Page.ChildPage> childPages = new ArrayList<>();
                List<InternalLink> discoveredLinks = internalLinkRepository.findBySessionIdAndFoundOnPage(session.getId(), page.getUrl());
                for (InternalLink link : discoveredLinks) {
                    childPages.add(new Page.ChildPage(link.getUrl()));
                }
                page.setChildPages(childPages);

                // External URLs
                List<Page.ExternalUrlInfo> urls = new ArrayList<>();
                for (ExternalUrl externalUrl : data.externalUrls) {
                    if (externalUrl.getFoundOnPage() != null && externalUrl.getFoundOnPage().equals(page.getUrl())) {
                        urls.add(new Page.ExternalUrlInfo(externalUrl.getUrl()));
                    }
                }
                page.setUrls(urls);

                // Downloaded files
                List<Page.DownloadInfo> downloads = new ArrayList<>();
                for (DownloadedFile file : data.downloadedFiles) {
                    if (file.getPageId() != null && file.getPageId().equals(page.getId())) {
                        downloads.add(new Page.DownloadInfo(file.getFileName() != null ? file.getFileName() : file.getUrl()));
                    }
                }
                page.setDownloads(downloads);
            }
        }

        if (request.getIncludeFlows()) {
            data.flows = flowRepository.findBySessionId(session.getId());
        }

        if (request.getIncludeExtractedData()) {
            data.extractedData = extractedDataRepository.findBySessionId(session.getId());
        }

        if (request.getIncludeDownloadedFiles() && data.downloadedFiles == null) {
            data.downloadedFiles = downloadedFileRepository.findBySessionId(session.getId());
        }

        if (data.externalUrls == null) {
            data.externalUrls = externalUrlRepository.findBySessionId(session.getId());
        }

        return data;
    }

    private String exportToJson(ExportData data, Path exportDir) throws IOException {
        Path jsonFile = exportDir.resolve("export.json");
        objectMapper.writeValue(jsonFile.toFile(), data);
        log.info("Exported to JSON: {}", jsonFile);
        return jsonFile.toString();
    }

    private String exportToCsv(ExportData data, Path exportDir) throws IOException {
        // Export pages to CSV
        if (data.pages != null && !data.pages.isEmpty()) {
            Path pagesFile = exportDir.resolve("pages.csv");
            try (CSVWriter writer = new CSVWriter(new FileWriter(pagesFile.toFile()))) {
                // Header
                writer.writeNext(new String[]{"ID", "URL", "Parent URL", "Depth", "Status Code", "Title", "Visited At"});

                // Data
                for (Page page : data.pages) {
                    writer.writeNext(new String[]{
                            String.valueOf(page.getId()),
                            page.getUrl(),
                            page.getParentUrl(),
                            String.valueOf(page.getDepthLevel()),
                            String.valueOf(page.getStatusCode()),
                            page.getTitle(),
                            page.getVisitedAt().toString()
                    });
                }
            }
        }

        // Export extracted data to CSV
        if (data.extractedData != null && !data.extractedData.isEmpty()) {
            Path extractedFile = exportDir.resolve("extracted_data.csv");
            try (CSVWriter writer = new CSVWriter(new FileWriter(extractedFile.toFile()))) {
                writer.writeNext(new String[]{"ID", "Page ID", "Rule ID", "Value", "Extracted At"});

                for (ExtractedData ed : data.extractedData) {
                    writer.writeNext(new String[]{
                            String.valueOf(ed.getId()),
                            String.valueOf(ed.getPageId()),
                            String.valueOf(ed.getRuleId()),
                            ed.getExtractedValue(),
                            ed.getExtractedAt().toString()
                    });
                }
            }
        }

        // Export flows to CSV
        if (data.flows != null && !data.flows.isEmpty()) {
            Path flowsFile = exportDir.resolve("flows.csv");
            try (CSVWriter writer = new CSVWriter(new FileWriter(flowsFile.toFile()))) {
                writer.writeNext(new String[]{"ID", "Depth", "Flow Path", "Discovered At"});

                for (NavigationFlow flow : data.flows) {
                    writer.writeNext(new String[]{
                            String.valueOf(flow.getId()),
                            String.valueOf(flow.getDepth()),
                            String.join(" → ", flow.getFlowPath()),
                            flow.getDiscoveredAt().toString()
                    });
                }
            }
        }

        log.info("Exported to CSV: {}", exportDir);
        return exportDir.toString();
    }

    private String exportToExcel(ExportData data, Path exportDir) throws IOException {
        Path excelFile = exportDir.resolve("export.xlsx");

        try (Workbook workbook = new XSSFWorkbook()) {
            // Session info sheet
            Sheet sessionSheet = workbook.createSheet("Session Info");
            createSessionInfoSheet(sessionSheet, data.session);

            // Pages sheet
            if (data.pages != null && !data.pages.isEmpty()) {
                Sheet pagesSheet = workbook.createSheet("Pages");
                createPagesSheet(pagesSheet, data.pages);

                // Page Details sheet - shows child pages, external URLs, and files for each page
                Sheet pageDetailsSheet = workbook.createSheet("Page Details");
                createPageDetailsSheet(pageDetailsSheet, data);
            }

            // Flows sheet
            if (data.flows != null && !data.flows.isEmpty()) {
                Sheet flowsSheet = workbook.createSheet("Flows");
                createFlowsSheet(flowsSheet, data.flows);
            }

            // Extracted data sheet
            if (data.extractedData != null && !data.extractedData.isEmpty()) {
                Sheet extractedSheet = workbook.createSheet("Extracted Data");
                createExtractedDataSheet(extractedSheet, data.extractedData);
            }

            // Downloaded files sheet
            if (data.downloadedFiles != null && !data.downloadedFiles.isEmpty()) {
                Sheet filesSheet = workbook.createSheet("Downloaded Files");
                createDownloadedFilesSheet(filesSheet, data.downloadedFiles);
            }

            try (FileOutputStream fos = new FileOutputStream(excelFile.toFile())) {
                workbook.write(fos);
            }
        }

        log.info("Exported to Excel: {}", excelFile);
        return excelFile.toString();
    }

    private String exportToPdf(ExportData data, Path exportDir) throws IOException {
        Path pdfFile = exportDir.resolve("export.pdf");

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 16);
                contentStream.newLineAtOffset(50, 750);
                contentStream.showText("JCrawler Export Report");
                contentStream.endText();

                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                contentStream.newLineAtOffset(50, 720);
                contentStream.showText("Session ID: " + data.session.getId());
                contentStream.newLineAtOffset(0, -20);
                contentStream.showText("Start URL: " + data.session.getStartUrl());
                contentStream.newLineAtOffset(0, -20);
                contentStream.showText("Status: " + data.session.getStatus());
                contentStream.newLineAtOffset(0, -20);
                contentStream.showText("Total Pages: " + data.session.getTotalPages());
                contentStream.newLineAtOffset(0, -20);
                contentStream.showText("Total Flows: " + data.session.getTotalFlows());
                contentStream.newLineAtOffset(0, -20);
                contentStream.showText("Total Extracted: " + data.session.getTotalExtracted());
                contentStream.endText();
            }

            document.save(pdfFile.toFile());
        }

        log.info("Exported to PDF: {}", pdfFile);
        return pdfFile.toString();
    }

    // Helper methods for Excel sheets
    private void createSessionInfoSheet(Sheet sheet, CrawlSession session) {
        int rowNum = 0;
        createRow(sheet, rowNum++, "Session ID", String.valueOf(session.getId()));
        createRow(sheet, rowNum++, "Start URL", session.getStartUrl());
        createRow(sheet, rowNum++, "Base Domain", session.getBaseDomain());
        createRow(sheet, rowNum++, "Status", session.getStatus().name());
        createRow(sheet, rowNum++, "Start Time", session.getStartTime().toString());
        if (session.getEndTime() != null) {
            createRow(sheet, rowNum++, "End Time", session.getEndTime().toString());
        }
        createRow(sheet, rowNum++, "Total Pages", String.valueOf(session.getTotalPages()));
        createRow(sheet, rowNum++, "Total Flows", String.valueOf(session.getTotalFlows()));
        createRow(sheet, rowNum++, "Total Extracted", String.valueOf(session.getTotalExtracted()));
    }

    private void createPagesSheet(Sheet sheet, List<Page> pages) {
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("ID");
        headerRow.createCell(1).setCellValue("URL");
        headerRow.createCell(2).setCellValue("Parent URL");
        headerRow.createCell(3).setCellValue("Depth");
        headerRow.createCell(4).setCellValue("Status Code");
        headerRow.createCell(5).setCellValue("Title");

        int rowNum = 1;
        for (Page page : pages) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(page.getId());
            row.createCell(1).setCellValue(page.getUrl() != null ? page.getUrl() : "");
            row.createCell(2).setCellValue(page.getParentUrl() != null ? page.getParentUrl() : "");
            row.createCell(3).setCellValue(page.getDepthLevel());
            row.createCell(4).setCellValue(page.getStatusCode());
            row.createCell(5).setCellValue(page.getTitle() != null ? page.getTitle() : "");
        }
    }

    private void createFlowsSheet(Sheet sheet, List<NavigationFlow> flows) {
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("ID");
        headerRow.createCell(1).setCellValue("Depth");
        headerRow.createCell(2).setCellValue("Flow Path");

        int rowNum = 1;
        for (NavigationFlow flow : flows) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(flow.getId());
            row.createCell(1).setCellValue(flow.getDepth());
            row.createCell(2).setCellValue(String.join(" → ", flow.getFlowPath()));
        }
    }

    private void createExtractedDataSheet(Sheet sheet, List<ExtractedData> extractedDataList) {
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("ID");
        headerRow.createCell(1).setCellValue("Page ID");
        headerRow.createCell(2).setCellValue("Rule ID");
        headerRow.createCell(3).setCellValue("Value");

        int rowNum = 1;
        for (ExtractedData data : extractedDataList) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(data.getId());
            row.createCell(1).setCellValue(data.getPageId());
            row.createCell(2).setCellValue(data.getRuleId());
            row.createCell(3).setCellValue(data.getExtractedValue());
        }
    }

    private void createDownloadedFilesSheet(Sheet sheet, List<DownloadedFile> files) {
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("ID");
        headerRow.createCell(1).setCellValue("File Name");
        headerRow.createCell(2).setCellValue("URL");
        headerRow.createCell(3).setCellValue("Local Path");
        headerRow.createCell(4).setCellValue("File Size");

        int rowNum = 1;
        for (DownloadedFile file : files) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(file.getId());
            row.createCell(1).setCellValue(file.getFileName() != null ? file.getFileName() : "");
            row.createCell(2).setCellValue(file.getUrl() != null ? file.getUrl() : "");
            row.createCell(3).setCellValue(file.getLocalPath() != null ? file.getLocalPath() : "");
            row.createCell(4).setCellValue(file.getFileSize() != null ? file.getFileSize() : 0L);
        }
    }

    private void createRow(Sheet sheet, int rowNum, String label, String value) {
        Row row = sheet.createRow(rowNum);
        row.createCell(0).setCellValue(label);
        row.createCell(1).setCellValue(value);
    }

    private void createPageDetailsSheet(Sheet sheet, ExportData data) {
        int rowNum = 0;

        // Header
        Row headerRow = sheet.createRow(rowNum++);
        headerRow.createCell(0).setCellValue("Page URL");
        headerRow.createCell(1).setCellValue("Child Pages (Internal Links)");
        headerRow.createCell(2).setCellValue("External URLs");
        headerRow.createCell(3).setCellValue("Downloaded Files");

        // For each page, find related data
        for (Page page : data.pages) {
            String pageUrl = page.getUrl() != null ? page.getUrl() : "";

            // Use the structured fields that were already populated
            List<String> childPages = new ArrayList<>();
            if (page.getChildPages() != null) {
                for (Page.ChildPage cp : page.getChildPages()) {
                    childPages.add(cp.getPageUrl());
                }
            }

            List<String> externalUrls = new ArrayList<>();
            if (page.getUrls() != null) {
                for (Page.ExternalUrlInfo url : page.getUrls()) {
                    externalUrls.add(url.getUrl());
                }
            }

            List<String> downloadedFiles = new ArrayList<>();
            if (page.getDownloads() != null) {
                for (Page.DownloadInfo download : page.getDownloads()) {
                    downloadedFiles.add(download.getDownload());
                }
            }

            // Find the maximum count to determine how many rows we need for this page
            int maxCount = Math.max(Math.max(childPages.size(), externalUrls.size()), downloadedFiles.size());

            // If no related data, still create one row for the page
            if (maxCount == 0) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(pageUrl);
                row.createCell(1).setCellValue("");
                row.createCell(2).setCellValue("");
                row.createCell(3).setCellValue("");
            } else {
                // Create multiple rows, one for each item
                for (int i = 0; i < maxCount; i++) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(pageUrl);
                    row.createCell(1).setCellValue(i < childPages.size() ? childPages.get(i) : "");
                    row.createCell(2).setCellValue(i < externalUrls.size() ? externalUrls.get(i) : "");
                    row.createCell(3).setCellValue(i < downloadedFiles.size() ? downloadedFiles.get(i) : "");
                }
            }
        }

        // Auto-size columns
        for (int i = 0; i < 4; i++) {
            sheet.setColumnWidth(i, 8000);
        }
    }

    // Data transfer object
    private static class ExportData {
        CrawlSession session;
        List<Page> pages;
        List<NavigationFlow> flows;
        List<ExtractedData> extractedData;
        List<DownloadedFile> downloadedFiles;
        List<ExternalUrl> externalUrls;
    }
}
