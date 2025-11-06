package com.jcrawler.service;

import com.jcrawler.dto.ProgressUpdate;
import com.jcrawler.model.DownloadedFile;
import com.jcrawler.repository.CrawlSessionRepository;
import com.jcrawler.repository.DownloadedFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class DownloadService {

    private final DownloadedFileRepository downloadedFileRepository;
    private final CrawlSessionRepository sessionRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Value("${jcrawler.download.directory:downloads}")
    private String downloadDirectory;

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();

    @Async("downloadExecutor")
    public void downloadFile(String url, Long sessionId, Long pageId) {
        log.info("Downloading file: {}", url);

        DownloadedFile downloadedFile = DownloadedFile.builder()
                .sessionId(sessionId)
                .pageId(pageId)
                .url(url)
                .downloadedAt(LocalDateTime.now())
                .build();

        try {
            // Create session-specific download directory
            Path sessionDir = Paths.get(downloadDirectory, "session_" + sessionId);
            Files.createDirectories(sessionDir);

            // Extract filename from URL
            String fileName = extractFileName(url);
            String fileExtension = extractFileExtension(fileName);

            // Download the file
            Request request = new Request.Builder()
                    .url(url)
                    .build();

            Response response = httpClient.newCall(request).execute();

            if (!response.isSuccessful()) {
                throw new RuntimeException("Failed to download file: HTTP " + response.code());
            }

            // Get file info
            String mimeType = response.header("Content-Type");
            long fileSize = response.body().contentLength();

            // Save file to disk
            Path filePath = sessionDir.resolve(fileName);
            File file = filePath.toFile();

            // Handle duplicate filenames
            int counter = 1;
            while (file.exists()) {
                String nameWithoutExt = fileName.substring(0, fileName.lastIndexOf('.'));
                fileName = nameWithoutExt + "_" + counter + fileExtension;
                filePath = sessionDir.resolve(fileName);
                file = filePath.toFile();
                counter++;
            }

            try (InputStream inputStream = response.body().byteStream();
                 OutputStream outputStream = new FileOutputStream(file)) {

                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }

            response.close();

            // Update downloaded file record
            downloadedFile.setLocalPath(filePath.toString());
            downloadedFile.setFileName(fileName);
            downloadedFile.setFileSize(fileSize > 0 ? fileSize : file.length());
            downloadedFile.setMimeType(mimeType);
            downloadedFile.setFileExtension(fileExtension);
            downloadedFile.setDownloadSuccess(true);

            downloadedFileRepository.save(downloadedFile);

            // Update session total downloads
            final String finalFileName = fileName;
            sessionRepository.findById(sessionId).ifPresent(session -> {
                session.setTotalDownloaded(session.getTotalDownloaded() + 1);
                sessionRepository.save(session);

                // Send WebSocket update
                ProgressUpdate update = ProgressUpdate.fileDownloaded(
                        sessionId,
                        finalFileName,
                        downloadedFile.getFileSize(),
                        session.getTotalDownloaded()
                );
                messagingTemplate.convertAndSend("/topic/crawler/" + sessionId + "/progress", update);
            });

            log.info("Successfully downloaded: {}", fileName);

        } catch (Exception e) {
            log.error("Failed to download file: {}", url, e);
            downloadedFile.setDownloadSuccess(false);
            downloadedFile.setErrorMessage(e.getMessage());
            downloadedFileRepository.save(downloadedFile);
        }
    }

    public List<DownloadedFile> getDownloadedFiles(Long sessionId) {
        return downloadedFileRepository.findBySessionId(sessionId);
    }

    public DownloadedFile getDownloadedFile(Long fileId) {
        return downloadedFileRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("File not found"));
    }

    private String extractFileName(String url) {
        try {
            String decoded = URLDecoder.decode(url, StandardCharsets.UTF_8);
            String path = decoded.split("\\?")[0]; // Remove query parameters
            String[] parts = path.split("/");
            String fileName = parts[parts.length - 1];

            // If no filename, generate one
            if (fileName.isEmpty() || !fileName.contains(".")) {
                fileName = "file_" + System.currentTimeMillis() + ".dat";
            }

            return fileName;
        } catch (Exception e) {
            return "file_" + System.currentTimeMillis() + ".dat";
        }
    }

    private String extractFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0 && lastDot < fileName.length() - 1) {
            return fileName.substring(lastDot);
        }
        return "";
    }
}
