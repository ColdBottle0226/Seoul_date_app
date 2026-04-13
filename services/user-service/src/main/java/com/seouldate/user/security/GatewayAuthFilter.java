package com.seouldate.user.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Gateway 헤더 기반 인증 필터.
 *
 * <p>MSA 에서 JWT 검증은 API Gateway 책임이다.
 * 이 필터는 Gateway 가 주입한 헤더를 신뢰하여 SecurityContext 를 설정한다.
 *
 * <p>헤더 규칙:
 * <ul>
 *   <li>{@code X-User-Seq}  — 인증된 사용자 식별자 (일반 API)</li>
 *   <li>{@code X-User-Role} — 사용자 권한 (USER, ADMIN)</li>
 *   <li>{@code X-Internal-Service} — 내부 서비스 식별자 (내부 API)</li>
 * </ul>
 *
 * <p>경로별 정책:
 * <pre>
 * /api/auth/**     → 필터 스킵 (공개 API)
 * /api/internal/** → X-Internal-Service 필수, 없으면 403 즉시 반환
 * /api/users/**    → X-User-Seq 있으면 SecurityContext 설정,
 *                    없으면 Spring Security 가 401 반환
 * </pre>
 */
@Component
public class GatewayAuthFilter extends OncePerRequestFilter {

    static final String HEADER_USER_SEQ       = "X-User-Seq";
    static final String HEADER_USER_ROLE      = "X-User-Role";
    static final String HEADER_INTERNAL_SERVICE = "X-Internal-Service";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String path = request.getRequestURI();

        if (path.startsWith("/api/internal")) {
            String internalService = request.getHeader(HEADER_INTERNAL_SERVICE);
            if (internalService == null || internalService.isBlank()) {
                writeForbidden(response);
                return;
            }
            setAuthentication("internal:" + internalService, "ROLE_INTERNAL");
            chain.doFilter(request, response);
            return;
        }

        String userSeqHeader = request.getHeader(HEADER_USER_SEQ);
        if (userSeqHeader != null && !userSeqHeader.isBlank()) {
            String role = request.getHeader(HEADER_USER_ROLE);
            String authority = "ROLE_" + (role != null && !role.isBlank() ? role : "USER");
            setAuthentication(userSeqHeader, authority);
        }

        chain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/auth")
                || path.startsWith("/actuator")
                || "/error".equals(path);
    }

    private void setAuthentication(String principal, String authority) {
        var auth = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority(authority)));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private void writeForbidden(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
                "{\"success\":false,\"code\":\"CMN_003\",\"message\":\"접근 권한이 없습니다.\"}");
    }
}
