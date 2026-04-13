package com.seouldate.user.controller;

import com.seouldate.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 내부 서비스 간 통신 컨트롤러 (/api/internal)
 *
 * <p>접근 제어: {@link com.seouldate.user.security.GatewayAuthFilter} 가
 * X-Internal-Service 헤더를 검사하며, 헤더가 없으면 403 반환.
 *
 * <pre>
 * GET /api/internal/users/{userSeq}              사용자 기본 정보 조회
 * GET /api/internal/users/{userSeq}/exists       사용자 존재·활성 여부 확인
 * GET /api/internal/users/{userSeq}/preferences  취향 설정 조회 (추천 서비스용)
 * </pre>
 */
@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
public class InternalUserController {

    private final UserService userService;

    // TODO: 각 엔드포인트 구현 (Issue 별 PR)
}
