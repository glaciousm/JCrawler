package com.jcrawler.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrawlResponse {

    private Long sessionId;
    private String status;
    private String startUrl;
    private String baseDomain;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer totalPages;
    private Integer totalFlows;
    private Integer totalExtracted;
    private Integer totalDownloaded;
    private Integer totalExternalUrls;
    private String message;
}
