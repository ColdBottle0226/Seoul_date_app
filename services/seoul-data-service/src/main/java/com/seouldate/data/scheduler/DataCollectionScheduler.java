package com.seouldate.data.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 데이터 수집 스케줄러
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DataCollectionScheduler {

    /**
     * 실시간 혼잡도 수집 (5분마다)
     */
    @Scheduled(fixedRate = 300000) // 5분
    public void collectRealtimeCongestion() {
        log.info("Collecting realtime congestion data...");
        // TODO: 서울 실시간 도시데이터 API 호출
        // TODO: Kafka seoul.realtime.congestion 토픽으로 발행
    }

    /**
     * 문화행사 수집 (1시간마다)
     */
    @Scheduled(fixedRate = 3600000) // 1시간
    public void collectCulturalEvents() {
        log.info("Collecting cultural events...");
        // TODO: 문화행사 API 호출
        // TODO: Kafka seoul.event.updated 토픽으로 발행
    }

    /**
     * 장소 데이터 수집 (1일 1회, 새벽 2시)
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void collectPlaces() {
        log.info("Collecting places data...");
        // TODO: 일반음식점/모범음식점 API 호출
        // TODO: Kafka seoul.place.updated 토픽으로 발행
    }
}
