package com.seouldate.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * JWT 전역 인증 필터 (GlobalFilter)
 *
 * ─── 동작 순서 ───
 * 1. 요청 경로가 WHITELIST에 해당하면 인증 없이 다음 필터로 통과
 * 2. Authorization 헤더에서 "Bearer <token>" 추출
 * 3. JWT 서명 검증 (HMAC-SHA256 공유 시크릿 사용)
 * 4. 검증 성공 → X-User-Id, X-User-Roles 헤더를 추가하여 다운스트림 서비스로 전달
 *    - 다운스트림 서비스는 이 헤더를 신뢰하고 별도 JWT 파싱 없이 사용 가능
 * 5. 검증 실패 → 401 Unauthorized 반환 (에러 종류별 로그 구분)
 *
 * ─── 실패 케이스별 처리 ───
 * - 토큰 만료 (ExpiredJwtException)   → 401 + "TOKEN_EXPIRED" 로그
 * - 서명 불일치 (SignatureException)   → 401 + "INVALID_SIGNATURE" 로그
 * - 토큰 형식 오류 (MalformedJwt...)  → 401 + "MALFORMED_TOKEN" 로그
 * - 헤더 없음                         → 401 + "MISSING_TOKEN" 로그
 *
 * ─── 우선순위 ───
 * Ordered.HIGHEST_PRECEDENCE(-2147483648) 보다 높게 설정: order = -1
 * → Circuit Breaker 필터(order=0)보다 먼저 실행
 * → 인증 실패 시 다운스트림 호출 자체를 차단
 *
 * ─── 향후 개선 포인트 ───
 * 현재 HMAC(공유 시크릿) 방식 → RSA(비대칭 키) 방식 전환 권장
 * 전환 시: user-service의 /api/auth/public-key 서 공개키를 주기적으로 캐시하고
 *          Gateway에서 공개키로 검증 (시크릿 공유 불필요)
 */
@Slf4j
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    @Value("${jwt.secret}")
    private String jwtSecret;

    /**
     * 인증 없이 통과할 경로 목록 (Whitelist)
     *
     * /api/auth/**  : 로그인, 회원가입, 토큰 갱신
     * /actuator/**  : 헬스체크, 메트릭 (내부 모니터링)
     * /eureka/**    : Eureka 대시보드 경유 요청
     * /fallback/**  : Circuit Breaker Fallback 응답 경로
     * /favicon.ico  : 브라우저 자동 요청
     */
    private static final List<String> WHITELIST = List.of(
            "/api/auth",
            "/actuator",
            "/eureka",
            "/fallback",
            "/favicon.ico"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().toString();
        String method = request.getMethod().name();

        // 1. Whitelist 경로는 인증 스킵
        if (isWhitelisted(path)) {
            log.info("[JWT] Skip auth — Whitelist path: {} {}", method, path);
            return chain.filter(exchange);
        }

        // 2. Bearer 토큰 추출
        String token = extractBearerToken(exchange);
        if (token == null) {
            log.warn("[JWT] MISSING_TOKEN — {} {}", method, path);
            return errorResponse(exchange, HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header");
        }

        try {
            // 3. JWT 서명 검증
            Claims claims = validateToken(token);
            String userId = claims.getSubject();
            String roles = claims.get("roles", String.class);

            log.debug("[JWT] Authenticated — userId={}, path={} {}", userId, method, path);

            // 4. 다운스트림 서비스에 사용자 정보 헤더로 전달
            //    (다운스트림은 이 헤더를 신뢰하고 별도 파싱 불필요)
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-User-Id", userId)
                    .header("X-User-Roles", roles != null ? roles : "")
                    // 원본 Authorization 헤더 제거 (다운스트림에 시크릿 미전달)
                    // 필요 시 주석 해제
                    // .headers(h -> h.remove("Authorization"))
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (ExpiredJwtException e) {
            log.warn("[JWT] TOKEN_EXPIRED — path={} {}, subject={}", method, path, e.getClaims().getSubject());
            return errorResponse(exchange, HttpStatus.UNAUTHORIZED, "Token has expired");

        } catch (SignatureException e) {
            log.warn("[JWT] INVALID_SIGNATURE — path={} {}", method, path);
            return errorResponse(exchange, HttpStatus.UNAUTHORIZED, "Invalid token signature");

        } catch (MalformedJwtException e) {
            log.warn("[JWT] MALFORMED_TOKEN — path={} {}", method, path);
            return errorResponse(exchange, HttpStatus.UNAUTHORIZED, "Malformed JWT token");

        } catch (Exception e) {
            log.error("[JWT] UNEXPECTED_ERROR — path={} {}", method, path, e);
            return errorResponse(exchange, HttpStatus.UNAUTHORIZED, "Token validation failed");
        }
    }

    /**
     * Whitelist 매칭
     * startsWith 방식으로 prefix 기반 매칭
     */
    private boolean isWhitelisted(String path) {
        return WHITELIST.stream().anyMatch(path::startsWith);
    }

    /**
     * Authorization 헤더에서 Bearer 토큰 추출
     * "Bearer <token>" → "<token>"
     */
    private String extractBearerToken(ServerWebExchange exchange) {
        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    /**
     * JWT 서명 검증 및 Claims 추출
     * HMAC-SHA256 알고리즘 사용
     */
    private Claims validateToken(String token) {
        return Jwts.parser()
                .verifyWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                        jwtSecret.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 에러 응답 반환
     * HTTP 상태 코드와 간단한 메시지만 반환 (상세 내부 정보 노출 방지)
     */
    private Mono<Void> errorResponse(ServerWebExchange exchange, HttpStatus status, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().add("Content-Type", "application/json");
        byte[] body = ("{\"error\":\"" + message + "\"}").getBytes(StandardCharsets.UTF_8);
        return response.writeWith(
                Mono.just(response.bufferFactory().wrap(body))
        );
    }

    /**
     * 필터 우선순위: -1
     * 양수 = 낮은 우선순위, 음수 = 높은 우선순위
     * -1로 설정하여 대부분의 기본 필터보다 먼저 실행
     */
    @Override
    public int getOrder() {
        return -1;
    }
}
