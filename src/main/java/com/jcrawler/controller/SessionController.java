package com.jcrawler.controller;

import com.jcrawler.dto.CrawlRequest;
import com.jcrawler.model.ExtractionRule;
import com.jcrawler.repository.ExtractionRuleRepository;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SessionController {

    private final ExtractionRuleRepository ruleRepository;

    @PostMapping("/session/import")
    public ResponseEntity<Map<String, String>> importSession(@RequestBody SessionImportRequest request) {
        // Parse cookie string into map
        Map<String, String> cookies = parseCookies(request.getCookieString());
        return ResponseEntity.ok(cookies);
    }

    @PostMapping("/rules")
    public ResponseEntity<ExtractionRule> addRule(@Valid @RequestBody AddRuleRequest request) {
        ExtractionRule rule = ExtractionRule.builder()
                .sessionId(request.getSessionId())
                .ruleName(request.getRuleName())
                .selectorType(ExtractionRule.SelectorType.valueOf(request.getSelectorType().toUpperCase()))
                .selectorValue(request.getSelectorValue())
                .attributeToExtract(request.getAttributeToExtract())
                .enabled(true)
                .build();

        rule = ruleRepository.save(rule);
        return ResponseEntity.ok(rule);
    }

    @GetMapping("/rules/{sessionId}")
    public ResponseEntity<List<ExtractionRule>> getRules(@PathVariable Long sessionId) {
        List<ExtractionRule> rules = ruleRepository.findBySessionId(sessionId);
        return ResponseEntity.ok(rules);
    }

    @DeleteMapping("/rules/{ruleId}")
    public ResponseEntity<Void> deleteRule(@PathVariable Long ruleId) {
        ruleRepository.deleteById(ruleId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/rules/{ruleId}/toggle")
    public ResponseEntity<ExtractionRule> toggleRule(@PathVariable Long ruleId) {
        ExtractionRule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("Rule not found"));

        rule.setEnabled(!rule.getEnabled());
        rule = ruleRepository.save(rule);
        return ResponseEntity.ok(rule);
    }

    private Map<String, String> parseCookies(String cookieString) {
        Map<String, String> cookies = new java.util.HashMap<>();
        if (cookieString == null || cookieString.trim().isEmpty()) {
            return cookies;
        }

        String[] parts = cookieString.split(";");
        for (String part : parts) {
            String[] keyValue = part.trim().split("=", 2);
            if (keyValue.length == 2) {
                cookies.put(keyValue[0].trim(), keyValue[1].trim());
            }
        }

        return cookies;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionImportRequest {
        private String cookieString;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddRuleRequest {
        private Long sessionId;
        private String ruleName;
        private String selectorType;
        private String selectorValue;
        private String attributeToExtract;
    }
}
