package com.seouldate.recommendation.repository;

import com.seouldate.recommendation.domain.RecommendationRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecommendationRequestRepository extends JpaRepository<RecommendationRequest, Long> {

    List<RecommendationRequest> findByUserIdOrderByCreatedAtDesc(Long userId);
}
