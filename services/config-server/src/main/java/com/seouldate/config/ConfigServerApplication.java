package com.seouldate.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

/**
 * Spring Cloud Config Server
 *
 * 중앙 설정 관리 서버 - 모든 마이크로서비스의 설정을 Git 또는 파일 시스템에서 관리
 *
 * @EnableConfigServer 애노테이션으로 Config Server 기능 활성화
 * Port: 8888
 * Profile: native (로컬 파일 시스템 기반)
 */
@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}
