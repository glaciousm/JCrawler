package com.jcrawler.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgressUpdate {

    private ProgressType type;
    private Long sessionId;
    private LocalDateTime timestamp;
    private Map<String, Object> data;

    public enum ProgressType {
        PAGE_DISCOVERED,
        FLOW_DISCOVERED,
        DATA_EXTRACTED,
        FILE_DOWNLOADED,
        EXTERNAL_URL_FOUND,
        METRICS,
        LOG,
        CRAWL_COMPLETED,
        CRAWL_ERROR
    }

    public static ProgressUpdate pageDiscovered(Long sessionId, String url, Integer depth, Integer totalPages) {
        return ProgressUpdate.builder()
                .type(ProgressType.PAGE_DISCOVERED)
                .sessionId(sessionId)
                .timestamp(LocalDateTime.now())
                .data(Map.of(
                        "url", url,
                        "depth", depth,
                        "totalPages", totalPages
                ))
                .build();
    }

    public static ProgressUpdate flowDiscovered(Long sessionId, Long flowId, java.util.List<String> path, Integer totalFlows) {
        return ProgressUpdate.builder()
                .type(ProgressType.FLOW_DISCOVERED)
                .sessionId(sessionId)
                .timestamp(LocalDateTime.now())
                .data(Map.of(
                        "flowId", flowId,
                        "path", path,
                        "totalFlows", totalFlows
                ))
                .build();
    }

    public static ProgressUpdate dataExtracted(Long sessionId, String ruleName, Integer count, String value) {
        return ProgressUpdate.builder()
                .type(ProgressType.DATA_EXTRACTED)
                .sessionId(sessionId)
                .timestamp(LocalDateTime.now())
                .data(Map.of(
                        "ruleName", ruleName,
                        "count", count,
                        "value", value
                ))
                .build();
    }

    public static ProgressUpdate fileDownloaded(Long sessionId, String fileName, Long size, Integer totalDownloaded) {
        return ProgressUpdate.builder()
                .type(ProgressType.FILE_DOWNLOADED)
                .sessionId(sessionId)
                .timestamp(LocalDateTime.now())
                .data(Map.of(
                        "fileName", fileName,
                        "size", size,
                        "totalDownloaded", totalDownloaded
                ))
                .build();
    }

    public static ProgressUpdate metrics(Long sessionId, Double pagesPerSecond, Integer activeThreads, Integer queueSize) {
        return ProgressUpdate.builder()
                .type(ProgressType.METRICS)
                .sessionId(sessionId)
                .timestamp(LocalDateTime.now())
                .data(Map.of(
                        "pagesPerSecond", pagesPerSecond,
                        "activeThreads", activeThreads,
                        "queueSize", queueSize
                ))
                .build();
    }

    public static ProgressUpdate externalUrlFound(Long sessionId, String url, Integer totalExternalUrls) {
        return ProgressUpdate.builder()
                .type(ProgressType.EXTERNAL_URL_FOUND)
                .sessionId(sessionId)
                .timestamp(LocalDateTime.now())
                .data(Map.of(
                        "url", url,
                        "totalExternalUrls", totalExternalUrls
                ))
                .build();
    }

    public static ProgressUpdate log(Long sessionId, String level, String message) {
        return ProgressUpdate.builder()
                .type(ProgressType.LOG)
                .sessionId(sessionId)
                .timestamp(LocalDateTime.now())
                .data(Map.of(
                        "level", level,
                        "message", message
                ))
                .build();
    }
}
