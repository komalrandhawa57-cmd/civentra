package com.civentra.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class AIService {
    private static final Logger log = LoggerFactory.getLogger(AIService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient;
    private final boolean enabled;
    private final String apiKey;
    private final String model;
    private final String endpoint;
    private final Duration timeout;

    public AIService(
            @Value("${civentra.ai.enabled:false}") boolean enabled,
            @Value("${civentra.ai.api-key:}") String apiKey,
            @Value("${civentra.ai.model:gpt-4o-mini}") String model,
            @Value("${civentra.ai.endpoint:https://api.openai.com/v1/responses}") String endpoint,
            @Value("${civentra.ai.timeout-seconds:15}") long timeoutSeconds) {
        this.enabled = enabled;
        this.apiKey = apiKey;
        this.model = model;
        this.endpoint = endpoint;
        this.timeout = Duration.ofSeconds(Math.max(1, Math.min(timeoutSeconds, 30)));
        this.httpClient = HttpClient.newBuilder().connectTimeout(this.timeout).build();
    }

    public boolean isConfigured() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }

    public String answer(String prompt) throws IOException, InterruptedException {
        if (!isConfigured()) throw new IllegalStateException("The AI assistant is not configured.");
        if (prompt == null || prompt.isBlank() || prompt.length() > 4000) {
            throw new IllegalArgumentException("Enter a question of up to 4,000 characters.");
        }

        String body = objectMapper.createObjectNode()
                .put("model", model)
                .put("input", "You are CIVENTRA's civic information assistant. Give concise guidance about reporting civic issues and using this application. Do not claim access to a user's records, live provider availability, prices, or case status. Direct users to the relevant CIVENTRA screen to verify those details.\n\nUser question: " + prompt)
                .toString();
        HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                .timeout(timeout)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            log.warn("AI provider returned HTTP status {}", response.statusCode());
            throw new IOException("The AI service is temporarily unavailable.");
        }
        JsonNode root = objectMapper.readTree(response.body());
        String answer = root.path("output_text").asText("");
        if (answer.isBlank()) {
            JsonNode output = root.path("output");
            for (JsonNode item : output) {
                for (JsonNode content : item.path("content")) {
                    if ("output_text".equals(content.path("type").asText())) answer += content.path("text").asText("");
                }
            }
        }
        if (answer.isBlank()) throw new IOException("The AI service returned an empty response.");
        return answer;
    }

    /** Local rule based hints, not an AI result. */
    public String suggestCategory(String title, String description) {
        String text = ((title == null ? "" : title) + " " + (description == null ? "" : description)).toLowerCase();
        if (text.contains("pothole") || text.contains("road") || text.contains("asphalt") || text.contains("crack")) return "Roads & Maintenance";
        if (text.contains("water") || text.contains("pipe") || text.contains("leak") || text.contains("drain")) return "Water & Sanitation";
        if (text.contains("light") || text.contains("lamp") || text.contains("dark") || text.contains("pole")) return "Street Lighting";
        if (text.contains("garbage") || text.contains("trash") || text.contains("waste") || text.contains("clean")) return "Garbage & Waste";
        if (text.contains("park") || text.contains("tree") || text.contains("garden") || text.contains("grass")) return "Parks & Recreation";
        return "General Civic Issue";
    }

    public String suggestPriority(String description) {
        String text = description == null ? "" : description.toLowerCase();
        if (text.contains("danger") || text.contains("emergency") || text.contains("accident") || text.contains("spark") || text.contains("collapse")) return "CRITICAL";
        if (text.contains("severe") || text.contains("blocked") || text.contains("overflow") || text.contains("major")) return "HIGH";
        if (text.contains("minor") || text.contains("small") || text.contains("cosmetic")) return "LOW";
        return "MEDIUM";
    }
}
