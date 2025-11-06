package com.jcrawler.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "navigation_flow", indexes = {
    @Index(name = "idx_session_depth", columnList = "session_id,depth")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NavigationFlow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long sessionId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "json")
    private List<String> flowPath;

    @Column(nullable = false)
    private Integer depth;

    @Column(nullable = false)
    private LocalDateTime discoveredAt;

    @Column(length = 2048)
    private String startUrl;

    @Column(length = 2048)
    private String endUrl;
}
