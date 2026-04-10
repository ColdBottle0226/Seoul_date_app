package com.seouldate.recommendation.client;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Place Service Fallback
 */
@Component
public class PlaceServiceFallback implements PlaceServiceClient {

    @Override
    public Map<String, Object> getPlaceMeta(Long placeId) {
        return Map.of(
                "error", "Place Service Unavailable",
                "placeId", placeId
        );
    }
}
