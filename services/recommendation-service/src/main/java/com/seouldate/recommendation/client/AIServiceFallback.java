package com.seouldate.recommendation.client;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * AI Service Fallback
 */
@Component
public class AIServiceFallback implements AIServiceClient {

    @Override
    public Map<String, Object> requestRecommendation(Map<String, Object> request) {
        return Map.of(
                "error", "AI Service Unavailable",
                "message", "The AI service is currently unavailable. Please try again later."
        );
    }
}
