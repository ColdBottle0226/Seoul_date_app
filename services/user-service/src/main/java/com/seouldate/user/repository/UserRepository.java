package com.seouldate.user.repository;

<<<<<<< HEAD
import com.seouldate.user.domain.User;
import com.seouldate.user.dto.request.auth.SignupRequest;
import com.seouldate.user.dto.response.auth.SignupResponse;
=======
import java.util.Optional;
>>>>>>> bff3e29 (회원가입 TDD)

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.seouldate.user.domain.User;


@Repository
public interface UserRepository extends JpaRepository<User, Long> {

<<<<<<< HEAD
    SignupResponse save(SignupRequest request);
=======
    User save(User requestUser);
>>>>>>> bff3e29 (회원가입 TDD)

    Optional<User> findByEmail(String email);

    Boolean existsByEmail(String email);

    Optional<User> findByProviderAndProviderId(User.AuthProvider provider, String providerId);
}
