package com.accet.projecthub.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "password_reset_challenges",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_password_reset_email", columnNames = "email"),
                @UniqueConstraint(name = "uk_password_reset_grant", columnNames = "verified_token_hash")
        },
        indexes = @Index(name = "idx_password_reset_expiry", columnList = "expires_at")
)
@Getter
@Setter
@NoArgsConstructor
public class PasswordResetChallenge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String email;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "user_id")
        private User user;

    @Column(name = "otp_hash", length = 100)
    private String otpHash;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "last_sent_at")
    private LocalDateTime lastSentAt;

    @Column(name = "window_started_at")
    private LocalDateTime windowStartedAt;

    @Column(name = "send_count", nullable = false)
    private int sendCount;

    @Column(name = "verified_token_hash", length = 64)
    private String verifiedTokenHash;

    @Column(name = "verified_until")
    private LocalDateTime verifiedUntil;
}