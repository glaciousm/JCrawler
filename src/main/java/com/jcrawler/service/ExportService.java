package com.jcrawler.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jcrawler.dao.*;
import com.jcrawler.model.*;
import com.opencsv.CSVWriter;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Slf4j
public class ExportService {

    private final CrawlSessionDao sessionDao;
    private final PageDao pageDao;
    private final NavigationFlowDao flowDao;
    private final ExtractedDataDao extractedDataDao;
    private final DownloadedFileDao downloadedFileDao;
    private final ExternalUrlDao externalUrlDao;
    private final InternalLinkDao internalLinkDao;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(SerializationFeature.INDENT_OUTPUT);

    public ExportService(CrawlSessionDao sessionDao, PageDao pageDao, NavigationFlowDao flowDao,
                        ExtractedDataDao extractedDataDao, DownloadedFileDao downloadedFileDao,
                        ExternalUrlDao externalUrlDao, InternalLinkDao internalLinkDao) {
        this.sessionDao = sessionDao;
        this.pageDao = pageDao;
        this.flowDao = flowDao;
        this.extractedDataDao = extractedDataDao;
        this.downloadedFileDao = downloadedFileDao;
        this.externalUrlDao = externalUrlDao;
        this.internalLinkDao = internalLinkDao;
    }

    // Export methods for UI
    public void exportToJson(Long sessionId, String filePath) throws Exception {
        ExportData data = collectAllData(sessionId);

        List<Map<String, String>> flatData = new ArrayList<>();
        int pageId = 1;

        if (data.pages != null) {
            for (Page page : data.pages) {
                List<String> externalUrls = data.externalUrls.stream()
                    .filter(eu -> eu.getFoundOnPage().equals(page.getUrl()))
                    .map(ExternalUrl::getUrl)
                    .toList();

                List<String> downloads = data.downloadedFiles.stream()
                    .filter(df -> df.getPageId().equals(page.getId()))
                    .map(DownloadedFile::getUrl)
                    .toList();

                int rowsNeeded = Math.max(1, Math.max(externalUrls.size(), downloads.size()));

                for (int i = 0; i < rowsNeeded; i++) {
                    Map<String, String> row = new LinkedHashMap<>();
                    row.put("id", String.valueOf(pageId));
                    row.put("pageUrl", page.getUrl() != null ? page.getUrl() : "");
                    row.put("externalUrl", i < externalUrls.size() ? externalUrls.get(i) : "");
                    row.put("downloadFile", i < downloads.size() ? downloads.get(i) : "");
                    flatData.add(row);
                }
                pageId++;
            }
        }

        objectMapper.writeValue(new File(filePath), flatData);
        log.info("Exported session {} to JSON: {}", sessionId, filePath);
    }

    public void exportToCsv(Long sessionId, String filePath) throws Exception {
        ExportData data = collectAllData(sessionId);
        Path parentDir = Paths.get(filePath).getParent();
        if (parentDir != null) {
            Files.createDirectories(parentDir);
        }

        try (CSVWriter writer = new CSVWriter(new FileWriter(filePath))) {
            // Header
            writer.writeNext(new String[]{"ID", "Page URL", "External URL", "Download File"});

            if (data.pages != null) {
                int pageId = 1;
                for (Page page : data.pages) {
                    // Get external URLs and downloads for this page
                    List<String> externalUrls = data.externalUrls.stream()
                        .filter(eu -> eu.getFoundOnPage().equals(page.getUrl()))
                        .map(ExternalUrl::getUrl)
                        .toList();

                    List<String> downloads = data.downloadedFiles.stream()
                        .filter(df -> df.getPageId().equals(page.getId()))
                        .map(DownloadedFile::getUrl)
                        .toList();

                    // Calculate how many rows needed (at least 1)
                    int rowsNeeded = Math.max(1, Math.max(externalUrls.size(), downloads.size()));

                    for (int i = 0; i < rowsNeeded; i++) {
                        String externalUrl = i < externalUrls.size() ? externalUrls.get(i) : "";
                        String downloadFile = i < downloads.size() ? downloads.get(i) : "";

                        writer.writeNext(new String[]{
                            String.valueOf(pageId),
                            page.getUrl() != null ? page.getUrl() : "",
                            externalUrl,
                            downloadFile
                        });
                    }
                    pageId++;
                }
            }
        }
        log.info("Exported session {} to CSV: {}", sessionId, filePath);
    }

    public void exportToExcel(Long sessionId, String filePath) throws Exception {
        ExportData data = collectAllData(sessionId);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Crawl Data");

            // Header
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("ID");
            headerRow.createCell(1).setCellValue("Page URL");
            headerRow.createCell(2).setCellValue("External URL");
            headerRow.createCell(3).setCellValue("Download File");

            int currentRow = 1;
            int pageId = 1;

            if (data.pages != null) {
                for (Page page : data.pages) {
                    // Get external URLs and downloads for this page
                    List<String> externalUrls = data.externalUrls.stream()
                        .filter(eu -> eu.getFoundOnPage().equals(page.getUrl()))
                        .map(ExternalUrl::getUrl)
                        .toList();

                    List<String> downloads = data.downloadedFiles.stream()
                        .filter(df -> df.getPageId().equals(page.getId()))
                        .map(DownloadedFile::getUrl)
                        .toList();

                    // Calculate how many rows needed (at least 1)
                    int rowsNeeded = Math.max(1, Math.max(externalUrls.size(), downloads.size()));

                    // Create rows
                    for (int i = 0; i < rowsNeeded; i++) {
                        Row row = sheet.createRow(currentRow + i);
                        row.createCell(0).setCellValue(pageId);
                        row.createCell(1).setCellValue(page.getUrl() != null ? page.getUrl() : "");

                        String externalUrl = i < externalUrls.size() ? externalUrls.get(i) : "";
                        String downloadFile = i < downloads.size() ? downloads.get(i) : "";

                        row.createCell(2).setCellValue(externalUrl);
                        row.createCell(3).setCellValue(downloadFile);
                    }

                    // Merge cells in columns 0 and 1 if more than 1 row
                    if (rowsNeeded > 1) {
                        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(
                            currentRow, currentRow + rowsNeeded - 1, 0, 0)); // Column 0 (ID)
                        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(
                            currentRow, currentRow + rowsNeeded - 1, 1, 1)); // Column 1 (Page URL)
                    }

                    currentRow += rowsNeeded;
                    pageId++;
                }
            }

            Path parentDir = Paths.get(filePath).getParent();
            if (parentDir != null) {
                Files.createDirectories(parentDir);
            }

            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                workbook.write(fos);
            }
        }
        log.info("Exported session {} to Excel: {}", sessionId, filePath);
    }

    public void exportToPdf(Long sessionId, String filePath) throws Exception {
        ExportData data = collectAllData(sessionId);

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            PDPageContentStream contentStream = new PDPageContentStream(document, page);
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

            float yPosition = 750;
            float margin = 50;
            float fontSize = 10;
            float lineHeight = 12;

            // Title
            contentStream.beginText();
            contentStream.setFont(fontBold, 14);
            contentStream.newLineAtOffset(margin, yPosition);
            contentStream.showText("Crawl Session Export - ID: " + sessionId);
            contentStream.endText();
            yPosition -= 30;

            // Header
            contentStream.beginText();
            contentStream.setFont(fontBold, fontSize);
            contentStream.newLineAtOffset(margin, yPosition);
            contentStream.showText("ID | Page URL | External URL | Download File");
            contentStream.endText();
            yPosition -= 20;

            // Data rows
            contentStream.setFont(font, fontSize);
            int pageId = 1;

            if (data.pages != null) {
                for (Page pg : data.pages) {
                    List<String> externalUrls = data.externalUrls.stream()
                        .filter(eu -> eu.getFoundOnPage().equals(pg.getUrl()))
                        .map(ExternalUrl::getUrl)
                        .toList();

                    List<String> downloads = data.downloadedFiles.stream()
                        .filter(df -> df.getPageId().equals(pg.getId()))
                        .map(DownloadedFile::getUrl)
                        .toList();

                    int rowsNeeded = Math.max(1, Math.max(externalUrls.size(), downloads.size()));

                    for (int i = 0; i < rowsNeeded; i++) {
                        String externalUrl = i < externalUrls.size() ? externalUrls.get(i) : "";
                        String downloadFile = i < downloads.size() ? downloads.get(i) : "";

                        String line = pageId + " | " +
                                     (pg.getUrl() != null ? pg.getUrl().substring(0, Math.min(30, pg.getUrl().length())) : "") + " | " +
                                     (externalUrl.length() > 30 ? externalUrl.substring(0, 30) : externalUrl) + " | " +
                                     (downloadFile.length() > 30 ? downloadFile.substring(0, 30) : downloadFile);

                        contentStream.beginText();
                        contentStream.newLineAtOffset(margin, yPosition);
                        contentStream.showText(line);
                        contentStream.endText();

                        yPosition -= lineHeight;
                        if (yPosition < 50) {
                            contentStream.close();
                            page = new PDPage();
                            document.addPage(page);
                            contentStream = new PDPageContentStream(document, page);
                            yPosition = 750;
                        }
                    }
                    pageId++;
                }
            }

            contentStream.close();

            Path parentDir = Paths.get(filePath).getParent();
            if (parentDir != null) {
                Files.createDirectories(parentDir);
            }

            document.save(filePath);
        }
        log.info("Exported session {} to PDF: {}", sessionId, filePath);
    }

    private ExportData collectAllData(Long sessionId) {
        ExportData data = new ExportData();
        data.session = sessionDao.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));
        data.pages = pageDao.findBySessionId(sessionId);
        data.flows = flowDao.findBySessionId(sessionId);
        data.extractedData = extractedDataDao.findBySessionId(sessionId);
        data.downloadedFiles = downloadedFileDao.findBySessionId(sessionId);
        data.externalUrls = externalUrlDao.findBySessionId(sessionId);
        return data;
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
