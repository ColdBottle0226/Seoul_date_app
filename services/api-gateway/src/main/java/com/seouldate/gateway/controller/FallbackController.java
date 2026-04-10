package com.seouldate.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Circuit Breaker Fallback Controller
 *
 * 각 서비스의 Circuit Breaker가 Open 상태일 때 호출되는 Fallback 엔드포인트
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/user")
    public ResponseEntity<Map<String, Object>> userFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "User Service Unavailable",
                        "message", "The user service is temporarily unavailable. Please try again later.",
                        "timestamp", LocalDateTime.now()
                ));
    }

    @GetMapping("/place")
    public ResponseEntity<Map<String, Object>> placeFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "Place Service Unavailable",
                        "message", "The place service is temporarily unavailable. Please try again later.",
                        "timestamp", LocalDateTime.now()
                ));
    }

    @GetMapping("/recommendation")
    public ResponseEntity<Map<String, Object>> recommendationFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "Recommendation Service Unavailable",
                        "message", "The recommendation service is temporarily unavailable. Please try again later.",
                        "timestamp", LocalDateTime.now()
                ));
    }

    @GetMapping("/ai")
    public ResponseEntity<Map<String, Object>> aiFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "AI Service Unavailable",
                        "message", "The AI service is temporarily unavailable. Please try again later.",
                        "timestamp", LocalDateTime.now()
                ));
    }

    @GetMapping("/seoul-data")
    public ResponseEntity<Map<String, Object>> seoulDataFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "Seoul Data Service Unavailable",
                        "message", "The Seoul data service is temporarily unavailable. Please try again later.",
                        "timestamp", LocalDateTime.now()
                ));
    }
}
