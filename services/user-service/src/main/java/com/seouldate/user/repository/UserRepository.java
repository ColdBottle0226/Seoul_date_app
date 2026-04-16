package com.seouldate.user.repository;

import com.seouldate.user.domain.User;
import com.seouldate.user.dto.request.auth.SignupRequest;
import com.seouldate.user.dto.response.auth.SignupResponse;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    SignupResponse save(SignupRequest request);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByProviderAndProviderId(User.AuthProvider provider, String providerId);
}
