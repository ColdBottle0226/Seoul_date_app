package com.seouldate.recommendation.controller;

import com.seouldate.recommendation.client.AIServiceClient;
import com.seouldate.recommendation.client.PlaceServiceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 추천 컨트롤러
 */
@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final AIServiceClient aiServiceClient;
    private final PlaceServiceClient placeServiceClient;

    @PostMapping
    public ResponseEntity<Map<String, Object>> requestRecommendation(
            @RequestBody Map<String, Object> request
    ) {
        // AI Service 호출하여 추천 받기
        Map<String, Object> aiResponse = aiServiceClient.requestRecommendation(request);

        return ResponseEntity.ok(aiResponse);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}
