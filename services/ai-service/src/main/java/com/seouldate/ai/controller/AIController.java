package com.seouldate.ai.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AI 추천 컨트롤러
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AIController {

    @PostMapping("/recommend")
    public ResponseEntity<Map<String, Object>> recommend(
            @RequestBody Map<String, Object> request
    ) {
        // TODO: RAG 파이프라인 구현
        // 1. Qdrant 벡터 검색
        // 2. 컨텍스트 조합
        // 3. LLM 호출
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Recommendation service is ready (implementation pending)"
        ));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}
