package com.seouldate.user.controller;

import com.seouldate.user.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 인증 컨트롤러
 *
 * - 로그인/회원가입 엔드포인트
 * - JWT 공개키 엔드포인트 (Gateway가 JWT 검증에 사용)
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtUtil jwtUtil;

    /**
     * JWT 공개키 엔드포인트
     * API Gateway가 이 엔드포인트를 호출하여 공개키를 캐시하고 JWT 검증에 사용
     */
    @GetMapping("/public-key")
    public ResponseEntity<Map<String, String>> getPublicKey() {
        // 실제 구현에서는 RSA 공개키를 반환해야 하지만,
        // 현재는 HMAC 방식을 사용하므로 시크릿을 공유하지 않음
        // 추후 RSA 방식으로 전환 시 공개키를 여기서 제공
        return ResponseEntity.ok(Map.of(
                "algorithm", "HMAC-SHA256",
                "message", "Using shared secret key for JWT validation"
        ));
    }

    /**
     * 헬스체크 엔드포인트
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}
