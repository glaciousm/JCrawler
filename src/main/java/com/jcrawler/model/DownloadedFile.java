package com.jcrawler.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "downloaded_file", indexes = {
    @Index(name = "idx_session_page", columnList = "session_id,page_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DownloadedFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long sessionId;

    private Long pageId;

    @Column(nullable = false, length = 2048)
    private String url;

    @Column(nullable = false, length = 1000)
    private String localPath;

    @Column(nullable = false, length = 500)
    private String fileName;

    private Long fileSize;

    @Column(length = 100)
    private String mimeType;

    @Column(length = 20)
    private String fileExtension;

    @Column(nullable = false)
    private LocalDateTime downloadedAt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean downloadSuccess = true;

    @Column(length = 1000)
    private String errorMessage;
}
