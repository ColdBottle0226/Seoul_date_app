package com.seouldate.user.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * User 엔티티
 *
 * <p>규칙:
 * <ul>
 *   <li>상태 변경은 반드시 도메인 메서드를 통해 수행한다 (@Setter 를 외부에서 직접 호출하지 않는다).</li>
 *   <li>Soft Delete: enabled=false 처리 (탈퇴/정지 통합 관리는 추후 UserStatus enum 으로 분리).</li>
 * </ul>
 */
@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
@Getter
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

    // ─────────────────────────────────────────────────────────────────────────
    // 도메인 메서드 (상태 변경은 여기서만)
    // ─────────────────────────────────────────────────────────────────────────

    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public void disable() {
        this.enabled = false;
    }

    public enum AuthProvider {
        EMAIL, KAKAO, NAVER, GOOGLE
    }

    public enum UserRole {
        USER, ADMIN
    }
}
