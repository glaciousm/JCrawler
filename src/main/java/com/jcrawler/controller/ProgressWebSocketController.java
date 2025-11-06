package com.jcrawler.controller;

import com.jcrawler.dto.ProgressUpdate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ProgressWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Test endpoint to verify WebSocket connectivity
     */
    @MessageMapping("/test/{sessionId}")
    @SendTo("/topic/crawler/{sessionId}/progress")
    public ProgressUpdate testConnection(@DestinationVariable Long sessionId) {
        log.info("WebSocket test message received for session: {}", sessionId);
        return ProgressUpdate.builder()
                .type(ProgressUpdate.ProgressType.LOG)
                .sessionId(sessionId)
                .timestamp(LocalDateTime.now())
                .data(Map.of("level", "INFO", "message", "WebSocket connection established"))
                .build();
    }

    /**
     * Send a custom progress update (used for testing or manual notifications)
     */
    public void sendProgressUpdate(Long sessionId, ProgressUpdate update) {
        messagingTemplate.convertAndSend("/topic/crawler/" + sessionId + "/progress", update);
    }
}
