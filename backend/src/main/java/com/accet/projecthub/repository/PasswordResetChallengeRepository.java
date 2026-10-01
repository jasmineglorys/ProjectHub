package com.accet.projecthub.repository;

import com.accet.projecthub.entity.PasswordResetChallenge;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasswordResetChallengeRepository extends JpaRepository<PasswordResetChallenge, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from PasswordResetChallenge c where lower(c.email) = lower(:email)")
    Optional<PasswordResetChallenge> findLockedByEmail(@Param("email") String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PasswordResetChallenge> findByVerifiedTokenHash(String verifiedTokenHash);
}