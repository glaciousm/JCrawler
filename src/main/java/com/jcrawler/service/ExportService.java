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
        // Export page details to CSV (same structure as Excel)
        if (data.pages != null && !data.pages.isEmpty()) {
            Path csvFile = exportDir.resolve("page_details.csv");
            try (CSVWriter writer = new CSVWriter(new FileWriter(csvFile.toFile()))) {
                // Header
                writer.writeNext(new String[]{"#", "Page URL", "Child Pages (Internal Links)", "External URLs", "Downloaded Files"});

                int pageNumber = 0;
                // For each page, find related data
                for (Page page : data.pages) {
                    pageNumber++;
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
                        writer.writeNext(new String[]{String.valueOf(pageNumber), pageUrl, "", "", ""});
                    } else {
                        // Create multiple rows, one for each item
                        for (int i = 0; i < maxCount; i++) {
                            writer.writeNext(new String[]{
                                    String.valueOf(pageNumber),
                                    pageUrl,
                                    i < childPages.size() ? childPages.get(i) : "",
                                    i < externalUrls.size() ? externalUrls.get(i) : "",
                                    i < downloadedFiles.size() ? downloadedFiles.get(i) : ""
                            });
                        }
                    }

                    // Add empty separator row between pages
                    if (pageNumber < data.pages.size()) {
                        writer.writeNext(new String[]{"", "", "", "", ""});
                    }
                }
            }
        }

        log.info("Exported to CSV: {}", exportDir);
        return exportDir.toString();
    }

    private String exportToExcel(ExportData data, Path exportDir) throws IOException {
        Path excelFile = exportDir.resolve("export.xlsx");

        try (Workbook workbook = new XSSFWorkbook()) {
            // Only create Page Details sheet
            if (data.pages != null && !data.pages.isEmpty()) {
                Sheet pageDetailsSheet = workbook.createSheet("Page Details");
                createPageDetailsSheetWithMerging(pageDetailsSheet, data, workbook);
            }

            try (FileOutputStream fos = new FileOutputStream(excelFile.toFile())) {
                workbook.write(fos);
            }
        }

        log.info("Exported to Excel: {}", excelFile);
        return excelFile.toString();
    }

    private String exportToPdf(ExportData data, Path exportDir) throws IOException {
        Path pdfFile = exportDir.resolve("page_details.pdf");

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            PDPageContentStream contentStream = new PDPageContentStream(document, page);

            contentStream.beginText();
            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 14);
            contentStream.newLineAtOffset(50, 750);
            contentStream.showText("Page Details Export");
            contentStream.endText();

            contentStream.beginText();
            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
            contentStream.newLineAtOffset(50, 720);

            int yPosition = 720;
            int pageNumber = 0;

            if (data.pages != null && !data.pages.isEmpty()) {
                for (Page pg : data.pages) {
                    pageNumber++;

                    if (yPosition < 100) {
                        // Add new page if running out of space
                        contentStream.endText();
                        contentStream.close();

                        PDPage newPage = new PDPage();
                        document.addPage(newPage);

                        contentStream = new PDPageContentStream(document, newPage);
                        contentStream.beginText();
                        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                        contentStream.newLineAtOffset(50, 750);
                        yPosition = 750;
                    }

                    String pageUrl = pg.getUrl() != null ? pg.getUrl() : "";
                    contentStream.showText("#" + pageNumber + " - Page: " + (pageUrl.length() > 65 ? pageUrl.substring(0, 65) + "..." : pageUrl));
                    contentStream.newLineAtOffset(0, -15);
                    yPosition -= 15;

                    if (pg.getChildPages() != null && !pg.getChildPages().isEmpty()) {
                        contentStream.showText("  Child Pages: " + pg.getChildPages().size());
                        contentStream.newLineAtOffset(0, -15);
                        yPosition -= 15;
                    }

                    if (pg.getUrls() != null && !pg.getUrls().isEmpty()) {
                        contentStream.showText("  External URLs: " + pg.getUrls().size());
                        contentStream.newLineAtOffset(0, -15);
                        yPosition -= 15;
                    }

                    if (pg.getDownloads() != null && !pg.getDownloads().isEmpty()) {
                        contentStream.showText("  Downloaded Files: " + pg.getDownloads().size());
                        contentStream.newLineAtOffset(0, -15);
                        yPosition -= 15;
                    }

                    // Draw horizontal line between page groups
                    if (pageNumber < data.pages.size()) {
                        contentStream.endText();
                        contentStream.setLineWidth(2f);
                        contentStream.moveTo(50, yPosition - 5);
                        contentStream.lineTo(550, yPosition - 5);
                        contentStream.stroke();
                        contentStream.beginText();
                        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                        contentStream.newLineAtOffset(50, yPosition - 20);
                        yPosition -= 20;
                    } else {
                        contentStream.newLineAtOffset(0, -10);
                        yPosition -= 10;
                    }
                }
            }

            contentStream.endText();
            contentStream.close();

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

    private void createPageDetailsSheetWithMerging(Sheet sheet, ExportData data, Workbook workbook) {
        int rowNum = 0;

        // Header
        Row headerRow = sheet.createRow(rowNum++);
        headerRow.createCell(0).setCellValue("#");
        headerRow.createCell(1).setCellValue("Page URL");
        headerRow.createCell(2).setCellValue("Child Pages (Internal Links)");
        headerRow.createCell(3).setCellValue("External URLs");
        headerRow.createCell(4).setCellValue("Downloaded Files");

        // Create styles
        CellStyle centerStyle = workbook.createCellStyle();
        centerStyle.setAlignment(HorizontalAlignment.CENTER);
        centerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        CellStyle thickBottomBorder = workbook.createCellStyle();
        thickBottomBorder.setBorderBottom(BorderStyle.THICK);

        int pageNumber = 0;
        // For each page, find related data
        for (Page page : data.pages) {
            pageNumber++;
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

            int startRow = rowNum;

            // If no related data, still create one row for the page
            if (maxCount == 0) {
                Row row = sheet.createRow(rowNum++);
                Cell numCell = row.createCell(0);
                numCell.setCellValue(pageNumber);
                numCell.setCellStyle(centerStyle);
                row.createCell(1).setCellValue(pageUrl);
                row.createCell(2).setCellValue("");
                row.createCell(3).setCellValue("");
                row.createCell(4).setCellValue("");
            } else {
                // Create multiple rows, one for each item
                for (int i = 0; i < maxCount; i++) {
                    Row row = sheet.createRow(rowNum++);
                    Cell numCell = row.createCell(0);
                    numCell.setCellValue(pageNumber);
                    numCell.setCellStyle(centerStyle);
                    row.createCell(1).setCellValue(pageUrl);
                    row.createCell(2).setCellValue(i < childPages.size() ? childPages.get(i) : "");
                    row.createCell(3).setCellValue(i < externalUrls.size() ? externalUrls.get(i) : "");
                    row.createCell(4).setCellValue(i < downloadedFiles.size() ? downloadedFiles.get(i) : "");
                }

                // Merge cells in first two columns if multiple rows were created for this page
                if (maxCount > 1) {
                    // Merge # column
                    sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(
                            startRow, rowNum - 1, 0, 0
                    ));
                    // Merge Page URL column
                    sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(
                            startRow, rowNum - 1, 1, 1
                    ));
                }
            }

            // Add thick bottom border to last row of this page group
            Row lastRow = sheet.getRow(rowNum - 1);
            if (lastRow != null && pageNumber < data.pages.size()) {
                for (int colIdx = 0; colIdx < 5; colIdx++) {
                    Cell cell = lastRow.getCell(colIdx);
                    if (cell != null) {
                        CellStyle newStyle = workbook.createCellStyle();
                        newStyle.cloneStyleFrom(cell.getCellStyle());
                        newStyle.setBorderBottom(BorderStyle.THICK);
                        cell.setCellStyle(newStyle);
                    }
                }
            }
        }

        // Auto-size columns
        sheet.setColumnWidth(0, 2000);  // # column narrower
        for (int i = 1; i < 5; i++) {
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
