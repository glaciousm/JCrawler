package com.jcrawler.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "page", indexes = {
    @Index(name = "idx_session_url", columnList = "session_id,url"),
    @Index(name = "idx_session_depth", columnList = "session_id,depth_level")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Page {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long sessionId;

    @Column(nullable = false, length = 2048)
    private String url;

    @Column(length = 2048)
    private String parentUrl;

    @Column(nullable = false)
    private Integer depthLevel;

    private Integer statusCode;

    @Column(length = 500)
    private String title;

    @Column(length = 64)
    private String contentHash;

    @Column(nullable = false)
    private LocalDateTime visitedAt;

    private Long processingTimeMs;

    @Column(length = 1000)
    private String errorMessage;

    @Column(nullable = false)
    @Builder.Default
    private Boolean processed = false;

    @Transient
    private List<ChildPage> childPages;

    @Transient
    private List<ExternalUrlInfo> urls;

    @Transient
    private List<DownloadInfo> downloads;

    // Nested classes for structured data
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ChildPage {
        private String pageUrl;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ExternalUrlInfo {
        private String url;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DownloadInfo {
        private String download;
    }
}
