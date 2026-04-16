package com.seouldate.user.repository;
import com.seouldate.user.domain.User;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;



@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    User save(User requestUser);

    Optional<User> findByEmail(String email);

    Boolean existsByEmail(String email);

    Optional<User> findByProviderAndProviderId(User.AuthProvider provider, String providerId);
}
