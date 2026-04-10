package com.seouldate.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * CORS 전역 설정
 *
 * Spring Cloud Gateway는 WebFlux 기반이므로
 * 일반 MVC의 WebMvcConfigurer 대신 CorsWebFilter(Reactive) 를 사용해야 한다.
 *
 * application.yml의 globalcors 설정과 병행 사용 가능하나,
 * Bean 등록 방식이 더 명확하고 테스트가 용이하다.
 *
 * allowedOriginPatterns vs allowedOrigins:
 *   allowCredentials=true 일 때는 반드시 allowedOriginPatterns 를 써야 함
 *   (allowedOrigins="*" 와 allowCredentials=true는 함께 사용 불가)
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // 허용할 Origin (운영에서는 실제 도메인으로 교체)
        config.setAllowedOriginPatterns(List.of(
                "http://localhost:3000",   // React/Next.js dev server
                "http://localhost:80",
                "http://localhost:8080"    // 직접 Gateway 접근
        ));

        // 허용 HTTP 메서드
        config.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));

        // 허용 요청 헤더
        config.setAllowedHeaders(List.of("*"));

        // 클라이언트가 읽을 수 있는 응답 헤더
        config.setExposedHeaders(List.of(
                "X-User-Id",      // Gateway가 JWT에서 추출해 추가한 헤더
                "X-Request-Id"    // 요청 추적 ID
        ));

        // 쿠키/인증 정보 포함 허용
        config.setAllowCredentials(true);

        // Preflight 요청 캐시 시간 (seconds)
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsWebFilter(source);
    }
}
