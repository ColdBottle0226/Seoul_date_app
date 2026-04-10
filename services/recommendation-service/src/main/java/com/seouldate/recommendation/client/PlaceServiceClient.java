package com.seouldate.recommendation.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * Place Service Feign Client
 *
 * Eureka를 통한 place-service 호출
 */
@FeignClient(name = "place-service", fallback = PlaceServiceFallback.class)
public interface PlaceServiceClient {

    @GetMapping("/api/places/{placeId}/meta")
    Map<String, Object> getPlaceMeta(@PathVariable("placeId") Long placeId);
}
