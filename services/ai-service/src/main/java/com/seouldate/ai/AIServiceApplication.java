package com.seouldate.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * AI Service
 *
 * RAG 기반 추천 생성 서비스
 * - Qdrant 벡터 검색 (장소 TOP-K)
 * - LLM(gpt-4o-mini) 호출하여 구조화된 코스 생성
 * - Kafka Consumer: vector.embed.requested → 임베딩 생성 → Qdrant upsert
 *
 * Port: 8084
 * DB: Redis + Qdrant
 */
@SpringBootApplication
@EnableDiscoveryClient
public class AIServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AIServiceApplication.class, args);
    }
}
