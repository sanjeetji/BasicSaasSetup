package com.basic.saas.repository;

import com.basic.saas.model.entity.Client;
import com.basic.saas.model.entity.RefreshToken;
import com.basic.saas.model.entity.SuperAdmin;
import com.basic.saas.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);

    Optional<RefreshToken> findByClientAndRevokedFalseAndExpiryDateAfter(Client client, LocalDateTime now);

    Optional<RefreshToken> findByUserAndRevokedFalseAndExpiryDateAfter(User user, LocalDateTime now);

    Optional<RefreshToken> findBySuperAdminAndRevokedFalseAndExpiryDateAfter(SuperAdmin superAdmin, LocalDateTime now);
}