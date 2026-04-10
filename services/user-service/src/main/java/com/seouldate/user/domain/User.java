package com.seouldate.user.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * User 엔티티
 */
@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(length = 255)
    private String password;

    @Column(nullable = false, length = 50)
    private String nickname;

    @Column(length = 20)
    @Enumerated(EnumType.STRING)
    private AuthProvider provider; // EMAIL, KAKAO, NAVER, GOOGLE

    @Column(length = 255)
    private String providerId;

    @Column(length = 20)
    @Enumerated(EnumType.STRING)
    private UserRole role; // USER, ADMIN

    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public enum AuthProvider {
        EMAIL, KAKAO, NAVER, GOOGLE
    }

    public enum UserRole {
        USER, ADMIN
    }
}
