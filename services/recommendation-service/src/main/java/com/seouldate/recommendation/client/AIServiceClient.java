package com.seouldate.recommendation.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * AI Service Feign Client
 *
 * Eureka를 통한 ai-service 호출
 */
@FeignClient(name = "ai-service", fallback = AIServiceFallback.class)
public interface AIServiceClient {

    @PostMapping("/api/ai/recommend")
    Map<String, Object> requestRecommendation(@RequestBody Map<String, Object> request);
}
