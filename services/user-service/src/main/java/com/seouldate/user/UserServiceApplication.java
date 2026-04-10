package com.seouldate.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * User Service
 *
 * 회원 관리 서비스
 * - 회원가입/로그인 (이메일, 소셜 OAuth)
 * - JWT Access Token (1h) + Refresh Token (7d) 발급
 * - Refresh Token → Redis 저장
 * - 사용자 취향 설정 관리
 * - RSA 공개키 엔드포인트 제공 (Gateway JWT 검증용)
 *
 * Port: 8081
 * DB: MySQL user_db (port 3306) + Redis
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableJpaAuditing
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
