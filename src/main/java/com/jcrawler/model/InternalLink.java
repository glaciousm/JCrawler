package com.jcrawler.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "internal_link", indexes = {
        @Index(name = "idx_session_internal", columnList = "session_id"),
        @Index(name = "idx_found_on_page", columnList = "found_on_page")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InternalLink {

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
}
