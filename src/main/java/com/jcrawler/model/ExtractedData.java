package com.jcrawler.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "extracted_data", indexes = {
    @Index(name = "idx_session_rule", columnList = "session_id,rule_id"),
    @Index(name = "idx_session_page", columnList = "session_id,page_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtractedData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long sessionId;

    @Column(nullable = false)
    private Long pageId;

    @Column(nullable = false)
    private Long ruleId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String extractedValue;

    @Column(nullable = false)
    private LocalDateTime extractedAt;
}
