package com.seouldate.data;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Seoul Data Service
 *
 * 서울 공공데이터 수집 서비스
 * - 서울 실시간 도시데이터 (5분마다)
 * - 일반음식점/모범음식점 인허가 (1일 1회)
 * - 문화행사 (1시간마다)
 * - 기상청 단기예보 (1시간마다)
 * - Kafka Producer: seoul.place.updated, seoul.event.updated, seoul.realtime.congestion
 *
 * Port: 8085
 * DB: 없음 (Stateless — Kafka만 사용)
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling
public class SeoulDataServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SeoulDataServiceApplication.class, args);
    }
}
