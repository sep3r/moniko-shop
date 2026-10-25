package com.monikoshop.repository;

import com.monikoshop.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("delete from PasswordResetToken t where t.user.id = :userId")
    void removeAllForUser(@Param("userId") Long userId);

    @Modifying
    @Query("delete from PasswordResetToken t where t.expiresAt < :now")
    void removeExpired(@Param("now") LocalDateTime now);

    @Query("select max(t.createdAt) from PasswordResetToken t where t.user.id = :userId")
    LocalDateTime findLatestCreatedAt(@Param("userId") Long userId);
}
