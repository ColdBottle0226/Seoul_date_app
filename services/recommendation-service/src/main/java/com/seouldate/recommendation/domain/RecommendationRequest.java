package com.seouldate.recommendation.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 추천 요청 엔티티
 */
@Entity
@Table(name = "recommendation_requests")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(length = 50)
    private String companionType; // ALONE, COUPLE, FRIENDS, FAMILY

    @Column
    private Integer budget;

    @Column(length = 500)
    private String styleTags;

    @Column(length = 1000)
    private String context;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
