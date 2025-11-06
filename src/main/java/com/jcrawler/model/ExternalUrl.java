package com.jcrawler.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "external_url", indexes = {
        @Index(name = "idx_session_external", columnList = "session_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalUrl {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long sessionId;

    @Column(nullable = false, length = 2048)
    private String url;

    @Column(length = 2048)
    private String foundOnPage;

    @Column
    private LocalDateTime discoveredAt;

    @Column(length = 255)
    private String domain;
}
