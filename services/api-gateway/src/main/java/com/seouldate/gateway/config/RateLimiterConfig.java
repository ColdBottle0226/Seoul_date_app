package com.seouldate.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

/**
 * Rate Limiter Key Resolver 설정
 *
 * Spring Cloud Gateway는 Redis Token Bucket 알고리즘으로 Rate Limit을 구현한다.
 * KeyResolver가 반환하는 값을 "버킷 키"로 사용해 각 주체별로 독립적인 토큰 버킷을 유지한다.
 *
 * 토큰 버킷 알고리즘:
 *   - replenishRate: 초당 충전되는 토큰 수 (처리량 상한)
 *   - burstCapacity: 버킷 최대 용량 (순간 폭발 허용량)
 *   - 요청 1개 = 토큰 1개 소모
 *   - 버킷이 비면 429 Too Many Requests 반환
 *
 * ⚠️ KeyResolver Bean이 여러 개일 경우 application.yml에서
 *    key-resolver: "#{@빈이름}" 으로 명시적으로 지정해야 한다.
 */
@Configuration
public class RateLimiterConfig {

    /**
     * [IP 기반 Rate Limiter] — 기본(Primary)
     *
     * 요청자의 IP 주소를 버킷 키로 사용한다.
     * 비로그인 사용자 또는 /api/auth/** 같은 공개 엔드포인트에 적합.
     *
     * 주의: X-Forwarded-For 헤더가 있으면 프록시 IP가 아닌 원본 IP를 사용.
     * Nginx reverse proxy 뒤에 있을 경우 올바른 IP가 전달되도록
     * Nginx의 proxy_set_header X-Real-IP $remote_addr 설정 필요.
     */
    @Primary
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            // X-Forwarded-For 헤더 우선 (Nginx, Load Balancer 뒤에 있을 때)
            String forwarded = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                // X-Forwarded-For: client, proxy1, proxy2 → 첫 번째(원본 클라이언트) IP
                String clientIp = forwarded.split(",")[0].trim();
                return Mono.just(clientIp);
            }

            // 직접 연결 IP
            String ip = exchange.getRequest().getRemoteAddress() != null
                    ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                    : "unknown";
            return Mono.just(ip);
        };
    }

    /**
     * [사용자 ID 기반 Rate Limiter]
     *
     * JWT 인증 후 JwtAuthenticationFilter가 추가하는 X-User-Id 헤더를 버킷 키로 사용.
     * 인증된 사용자 개별 트래픽 제어에 사용 가능 (IP 공유 환경 대응).
     *
     * 사용 방법 (application.yml):
     *   key-resolver: "#{@userKeyResolver}"
     *
     * 주의: 이 Resolver를 사용하는 라우트는 반드시 JWT 인증이 선행되어야 한다.
     *       X-User-Id가 없으면 "anonymous"로 처리되어 한 버킷에 묶임.
     */
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            if (userId != null && !userId.isBlank()) {
                return Mono.just("user:" + userId);
            }
            // 미인증 요청은 IP 기반으로 폴백
            String ip = exchange.getRequest().getRemoteAddress() != null
                    ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                    : "anonymous";
            return Mono.just("anon:" + ip);
        };
    }
}
