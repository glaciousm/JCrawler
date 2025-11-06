package com.jcrawler.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "extraction_rule")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtractionRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long sessionId;

    @Column(nullable = false, length = 100)
    private String ruleName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SelectorType selectorType;

    @Column(nullable = false, length = 1000)
    private String selectorValue;

    @Column(length = 100)
    private String attributeToExtract;

    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    public enum SelectorType {
        CSS,
        XPATH
    }
}
