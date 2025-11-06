package com.jcrawler.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportRequest {

    @NotNull
    private Long sessionId;

    @NotNull
    private List<ExportFormat> formats;

    @Builder.Default
    private Boolean includePages = true;

    @Builder.Default
    private Boolean includeFlows = true;

    @Builder.Default
    private Boolean includeExtractedData = true;

    @Builder.Default
    private Boolean includeDownloadedFiles = true;

    public enum ExportFormat {
        JSON,
        CSV,
        EXCEL,
        PDF
    }
}
