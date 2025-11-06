package com.jcrawler.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "crawl_session")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrawlSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 2048)
    private String startUrl;

    @Column(nullable = false)
    private String baseDomain;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CrawlStatus status;

    @Column(nullable = false)
    private LocalDateTime startTime;

    private LocalDateTime endTime;

    @Column(nullable = false)
    @Builder.Default
    private Integer totalPages = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer totalFlows = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer totalExtracted = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer totalDownloaded = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer totalExternalUrls = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private Map<String, String> sessionCookies;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private Map<String, String> authConfig;

    @Column(nullable = false)
    @Builder.Default
    private Integer maxDepth = 10;

    @Column(nullable = false)
    @Builder.Default
    private Integer maxPages = 1000;

    @Column(nullable = false)
    @Builder.Default
    private Double requestDelay = 1.0;

    @Column(nullable = false)
    @Builder.Default
    private Integer concurrentThreads = 5;

    @Column(nullable = false)
    @Builder.Default
    private Boolean enableJavaScript = false;

    public enum CrawlStatus {
        INITIALIZED,
        RUNNING,
        PAUSED,
        COMPLETED,
        FAILED,
        STOPPED
    }
}
