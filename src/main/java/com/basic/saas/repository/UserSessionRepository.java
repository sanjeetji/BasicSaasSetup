package com.basic.saas.repository;

import com.basic.saas.model.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface UserSessionRepository extends JpaRepository<UserSession, Long> {
    Optional<UserSession> findByUsername(String username);

    @Modifying
    @Query("update UserSession u set u.sessionId=:sid, u.updatedAt=:now where u.username=:username")
    int updateSid(@Param("username") String username, @Param("sid") String sid, @Param("now") Instant now);
}
