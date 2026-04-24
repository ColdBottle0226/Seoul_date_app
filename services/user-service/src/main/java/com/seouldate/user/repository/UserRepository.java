package com.seouldate.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.seouldate.user.domain.User;

/**
 * 회원 Repository
 *
 * <p>이메일 조회는 암호화 컬럼(email_enc) 대신 SHA-256 해시 컬럼(email_hash)을 사용한다.
 * 이메일 검색 전 {@code CryptoUtil.hash(email)} 을 호출하여 해시값을 전달할 것.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    User save(User requestUser);

    /**
     * 이메일 해시로 회원 조회.
     *
     * @param emailHash SHA-256(email.toLowerCase())
     */
    Optional<User> findByEmailHash(String emailHash);

    /**
     * 이메일 해시로 중복 여부 확인.
     *
     * @param emailHash SHA-256(email.toLowerCase())
     */
    Boolean existsByEmailHash(String emailHash);

    /**
     * 소셜 Provider + 소셜 고유 ID로 회원 조회. (소셜 로그인)
     */
    Optional<User> findByProviderCdAndProviderId(User.AuthProvider providerCd, String providerId);

    /**
     * 회원관리번호로 회원 조회.
     */
    Optional<User> findByMbrMngNo(String mbrMngNo);

    /**
     * 회원ID로 회원 조회.
     */
    Optional<User> findByMbrId(String mbrId);
}
