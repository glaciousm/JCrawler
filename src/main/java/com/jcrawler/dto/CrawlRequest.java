package com.jcrawler.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrawlRequest {

    @NotBlank(message = "Start URL is required")
    private String startUrl;

    private Map<String, String> cookies;

    private AuthConfig authConfig;

    private List<ExtractionRuleDto> extractionRules;

    @Builder.Default
    @Min(0)
    @Max(50)
    private Integer maxDepth = 0; // 0 means infinite

    @Builder.Default
    @Min(0)
    @Max(10000)
    private Integer maxPages = 0; // 0 means infinite

    @Builder.Default
    @Min(0)
    private Double requestDelay = 1.0;

    @Builder.Default
    @Min(1)
    @Max(20)
    private Integer concurrentThreads = 5;

    @Builder.Default
    private List<String> allowedFileExtensions = List.of(".pdf", ".docx", ".doc", ".xlsx", ".xls", ".zip", ".png", ".jpg", ".jpeg");

    @Builder.Default
    private Boolean downloadFiles = true;

    @Builder.Default
    private Boolean enableJavaScript = false;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthConfig {
        private String loginUrl;
        private String username;
        private String password;
        private Map<String, String> loginPayload;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExtractionRuleDto {
        @NotBlank
        private String ruleName;

        @NotBlank
        private String selectorType; // CSS or XPATH

        @NotBlank
        private String selectorValue;

        private String attributeToExtract; // text, href, src, etc.
    }
}
