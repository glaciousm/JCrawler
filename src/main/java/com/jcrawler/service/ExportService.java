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
        objectMapper.writeValue(new File(filePath), data);
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
            writer.writeNext(new String[]{"Page URL", "Title", "Status Code", "Depth"});

            if (data.pages != null) {
                for (Page page : data.pages) {
                    writer.writeNext(new String[]{
                        page.getUrl() != null ? page.getUrl() : "",
                        page.getTitle() != null ? page.getTitle() : "",
                        String.valueOf(page.getStatusCode()),
                        String.valueOf(page.getDepthLevel())
                    });
                }
            }
        }
        log.info("Exported session {} to CSV: {}", sessionId, filePath);
    }

    public void exportToExcel(Long sessionId, String filePath) throws Exception {
        ExportData data = collectAllData(sessionId);

        try (Workbook workbook = new XSSFWorkbook()) {
            // Session info sheet
            Sheet sessionSheet = workbook.createSheet("Session Info");
            createSessionInfoSheet(sessionSheet, data.session);

            // Pages sheet
            if (data.pages != null && !data.pages.isEmpty()) {
                Sheet pagesSheet = workbook.createSheet("Pages");
                createPagesSheet(pagesSheet, data.pages);
            }

            // Flows sheet
            if (data.flows != null && !data.flows.isEmpty()) {
                Sheet flowsSheet = workbook.createSheet("Navigation Flows");
                createFlowsSheet(flowsSheet, data.flows);
            }

            // Downloads sheet
            if (data.downloadedFiles != null && !data.downloadedFiles.isEmpty()) {
                Sheet downloadsSheet = workbook.createSheet("Downloads");
                createDownloadedFilesSheet(downloadsSheet, data.downloadedFiles);
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

            contentStream.beginText();
            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 14);
            contentStream.newLineAtOffset(50, 750);
            contentStream.showText("Crawl Session Export - ID: " + sessionId);
            contentStream.endText();

            contentStream.beginText();
            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
            contentStream.newLineAtOffset(50, 720);
            contentStream.showText("Start URL: " + data.session.getStartUrl());
            contentStream.newLineAtOffset(0, -15);
            contentStream.showText("Status: " + data.session.getStatus());
            contentStream.newLineAtOffset(0, -15);
            contentStream.showText("Total Pages: " + data.session.getTotalPages());
            contentStream.newLineAtOffset(0, -15);
            contentStream.showText("Total Flows: " + data.session.getTotalFlows());
            contentStream.endText();

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
    }

    private void createPagesSheet(Sheet sheet, List<Page> pages) {
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("ID");
        headerRow.createCell(1).setCellValue("URL");
        headerRow.createCell(2).setCellValue("Title");
        headerRow.createCell(3).setCellValue("Status Code");
        headerRow.createCell(4).setCellValue("Depth");

        int rowNum = 1;
        for (Page page : pages) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(page.getId());
            row.createCell(1).setCellValue(page.getUrl() != null ? page.getUrl() : "");
            row.createCell(2).setCellValue(page.getTitle() != null ? page.getTitle() : "");
            row.createCell(3).setCellValue(page.getStatusCode());
            row.createCell(4).setCellValue(page.getDepthLevel());
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

    private void createDownloadedFilesSheet(Sheet sheet, List<DownloadedFile> files) {
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("ID");
        headerRow.createCell(1).setCellValue("File Name");
        headerRow.createCell(2).setCellValue("URL");
        headerRow.createCell(3).setCellValue("File Size");

        int rowNum = 1;
        for (DownloadedFile file : files) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(file.getId());
            row.createCell(1).setCellValue(file.getFileName() != null ? file.getFileName() : "");
            row.createCell(2).setCellValue(file.getUrl() != null ? file.getUrl() : "");
            row.createCell(3).setCellValue(file.getFileSize() != null ? file.getFileSize() : 0L);
        }
    }

    private void createRow(Sheet sheet, int rowNum, String label, String value) {
        Row row = sheet.createRow(rowNum);
        row.createCell(0).setCellValue(label);
        row.createCell(1).setCellValue(value);
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
