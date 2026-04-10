package com.seouldate.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Spring Cloud Gateway
 *
 * API Gateway - 모든 클라이언트 요청의 진입점
 * - Eureka 기반 동적 라우팅 (lb://서비스명)
 * - JWT GlobalFilter (user-service 공개키 기반 검증)
 * - Redis 기반 Rate Limiting (100 req/min per IP)
 * - Resilience4j Circuit Breaker & TimeLimiter
 * - Zipkin 분산 트레이싱
 *
 * Port: 8080
 */
@SpringBootApplication
@EnableDiscoveryClient
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
