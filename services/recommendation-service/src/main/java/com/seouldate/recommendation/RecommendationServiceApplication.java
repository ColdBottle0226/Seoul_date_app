package com.seouldate.recommendation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Recommendation Service
 *
 * 데이트 코스 추천 서비스
 * - 추천 요청 수신 → ai-service HTTP 호출 → 코스 저장
 * - 추천 결과 Redis 캐시 (rec:{userId}:{sha256(context)} TTL 10분)
 * - 코스 저장(즐겨찾기), 피드백(별점·평점) 관리
 * - OpenFeign + Eureka로 place-service, ai-service 호출
 *
 * Port: 8083
 * DB: MySQL recommendation_db (port 3308) + Redis
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@EnableJpaAuditing
public class RecommendationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RecommendationServiceApplication.class, args);
    }
}
