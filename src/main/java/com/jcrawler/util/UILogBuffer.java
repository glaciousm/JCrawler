package com.jcrawler.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * In-memory log buffer that can be displayed in the UI
 */
public class UILogBuffer {
    private static final UILogBuffer instance = new UILogBuffer();
    private static final int MAX_LOGS = 1000;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private final ConcurrentLinkedQueue<String> logs = new ConcurrentLinkedQueue<>();

    private UILogBuffer() {}

    public static UILogBuffer getInstance() {
        return instance;
    }

    public void log(String message) {
        String timestamped = LocalDateTime.now().format(formatter) + " - " + message;
        logs.offer(timestamped);

        // Keep only last MAX_LOGS entries
        while (logs.size() > MAX_LOGS) {
            logs.poll();
        }
    }

    public String getAllLogs() {
        StringBuilder sb = new StringBuilder();
        for (String log : logs) {
            sb.append(log).append("\n");
        }
        return sb.toString();
    }

    public void clear() {
        logs.clear();
    }
}
