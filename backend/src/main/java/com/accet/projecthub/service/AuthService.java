package com.accet.projecthub.service;

import com.accet.projecthub.dto.AuthResponse;
import com.accet.projecthub.dto.MessageResponse;
import com.accet.projecthub.dto.LoginRequest;
import com.accet.projecthub.dto.RegisterRequest;
import com.accet.projecthub.dto.UserDto;
import com.accet.projecthub.entity.PasswordResetChallenge;
import com.accet.projecthub.entity.Role;
import com.accet.projecthub.entity.User;
import com.accet.projecthub.exception.BadRequestException;
import com.accet.projecthub.exception.DuplicateResourceException;
import com.accet.projecthub.exception.EmailDeliveryException;
import com.accet.projecthub.exception.ResourceNotFoundException;
import com.accet.projecthub.repository.BookmarkRepository;
import com.accet.projecthub.repository.PasswordResetChallengeRepository;
import com.accet.projecthub.repository.ProjectLikeRepository;
import com.accet.projecthub.repository.UserRepository;
import com.accet.projecthub.security.JwtService;
import com.accet.projecthub.util.Constants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Objects;

@Service
public class AuthService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int MAX_OTP_ATTEMPTS = 5;
    private static final int MAX_EMAILS_PER_HOUR = 5;
    private static final String RESET_EMAIL_MESSAGE =
        "If an account exists for that email, a verification code has been sent.";

    private final UserRepository userRepository;
    private final ProjectLikeRepository likeRepository;
    private final BookmarkRepository bookmarkRepository;
    private final PasswordResetChallengeRepository resetChallengeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JavaMailSender mailSender;
    private final String smtpHost;
    private final String smtpUsername;
    private final String smtpPassword;
    private final String mailFrom;

    public AuthService(UserRepository userRepository,
                       ProjectLikeRepository likeRepository,
                       BookmarkRepository bookmarkRepository,
               PasswordResetChallengeRepository resetChallengeRepository,
                       PasswordEncoder passwordEncoder,
               JwtService jwtService,
               JavaMailSender mailSender,
                       @Value("${spring.mail.host:}") String smtpHost,
                       @Value("${spring.mail.username:}") String smtpUsername,
                       @Value("${spring.mail.password:}") String smtpPassword,
               @Value("${app.mail.from:}") String mailFrom) {
        this.userRepository = userRepository;
        this.likeRepository = likeRepository;
        this.bookmarkRepository = bookmarkRepository;
     this.resetChallengeRepository = resetChallengeRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
     this.mailSender = mailSender;
     this.smtpHost = smtpHost;
     this.smtpUsername = smtpUsername;
     this.smtpPassword = smtpPassword;
     this.mailFrom = mailFrom;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("An account with this email already exists");
        }
        if (userRepository.existsByRollNoIgnoreCase(request.getRollNo().trim())) {
            throw new DuplicateResourceException("An account with this roll number already exists");
        }
        if (!Constants.DEPARTMENTS.contains(request.getDepartment())) {
            throw new BadRequestException("Invalid department: " + request.getDepartment());
        }

        User user = User.builder()
                .name(request.getName().trim())
                .email(email)
                .rollNo(request.getRollNo().trim().toUpperCase())
                .department(request.getDepartment())
                .year(request.getYear())
                .academicYear(request.getAcademicYear().trim())
                // BCrypt hash, never the raw password
                .password(passwordEncoder.encode(request.getPassword()))
                .avatar(request.getAvatar() == null || request.getAvatar().isBlank()
                        ? Constants.DEFAULT_AVATAR : request.getAvatar())
                .role(Role.STUDENT)
                .build();

        User saved = userRepository.save(user);
        return buildAuthResponse(saved);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.getEmail().trim())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        return buildAuthResponse(user);
    }

    @Transactional
    public MessageResponse requestPasswordReset(String email) {
        ensureSmtpConfigured();
        String normalizedEmail = email.trim().toLowerCase();
        userRepository.findByEmailIgnoreCase(normalizedEmail).ifPresent(user -> sendResetOtp(user.getEmail()));
        return new MessageResponse(RESET_EMAIL_MESSAGE);
    }

    @Transactional
    public MessageResponse resendPasswordResetOtp(String email) {
        ensureSmtpConfigured();
        String normalizedEmail = email.trim().toLowerCase();
        userRepository.findByEmailIgnoreCase(normalizedEmail).ifPresent(user -> sendResetOtp(user.getEmail()));
        return new MessageResponse(RESET_EMAIL_MESSAGE);
    }

    private void ensureSmtpConfigured() {
        if (smtpHost.isBlank() || smtpUsername.isBlank() || smtpPassword.isBlank()) {
            throw new EmailDeliveryException(
                    "Password reset email is not configured. Set SMTP_HOST, SMTP_USERNAME, and SMTP_PASSWORD, then restart the backend.");
        }
    }

    @NonNull
    @Transactional(noRollbackFor = BadRequestException.class)
    public String verifyPasswordResetOtp(String email, String otp) {
        String normalizedEmail = email.trim().toLowerCase();
        PasswordResetChallenge challenge = resetChallengeRepository.findLockedByEmail(normalizedEmail)
                .orElseThrow(() -> new BadRequestException("Invalid or expired verification code."));
        LocalDateTime now = LocalDateTime.now();

        if (challenge.getOtpHash() == null || challenge.getExpiresAt() == null
                || !challenge.getExpiresAt().isAfter(now)
                || challenge.getAttempts() >= MAX_OTP_ATTEMPTS) {
            throw new BadRequestException("Invalid or expired verification code.");
        }

        if (!passwordEncoder.matches(otp, challenge.getOtpHash())) {
            challenge.setAttempts(challenge.getAttempts() + 1);
            if (challenge.getAttempts() >= MAX_OTP_ATTEMPTS) {
                challenge.setOtpHash(null);
                challenge.setExpiresAt(null);
            }
            resetChallengeRepository.save(challenge);
            throw new BadRequestException("Invalid or expired verification code.");
        }

        byte[] grantBytes = new byte[32];
        SECURE_RANDOM.nextBytes(grantBytes);
        String resetGrant = Base64.getUrlEncoder().withoutPadding().encodeToString(grantBytes);
        challenge.setOtpHash(null);
        challenge.setExpiresAt(null);
        challenge.setVerifiedTokenHash(hashResetGrant(resetGrant));
        challenge.setVerifiedUntil(now.plusMinutes(10));
        resetChallengeRepository.save(challenge);
        return Objects.requireNonNull(resetGrant);
    }

    @Transactional
    public void resetPassword(String resetGrant, String newPassword) {
        if (resetGrant == null || resetGrant.isBlank()) {
            throw new BadRequestException("The password reset session has expired. Request a new code.");
        }

        PasswordResetChallenge challenge = resetChallengeRepository
                .findByVerifiedTokenHash(hashResetGrant(resetGrant))
                .orElseThrow(() -> new BadRequestException(
                        "The password reset session has expired. Request a new code."));
        if (challenge.getVerifiedUntil() == null
                || !challenge.getVerifiedUntil().isAfter(LocalDateTime.now())) {
            resetChallengeRepository.delete(challenge);
            throw new BadRequestException("The password reset session has expired. Request a new code.");
        }

        User user = userRepository.findByEmailIgnoreCase(challenge.getEmail())
                .orElseThrow(() -> new BadRequestException(
                        "The password reset session has expired. Request a new code."));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        resetChallengeRepository.delete(challenge);
    }

    private void sendResetOtp(String email) {
        LocalDateTime now = LocalDateTime.now();
        PasswordResetChallenge challenge = resetChallengeRepository.findLockedByEmail(email)
                .orElseGet(() -> {
                    PasswordResetChallenge created = new PasswordResetChallenge();
                    created.setEmail(email);
                    return created;
                });

        if (challenge.getLastSentAt() != null
                && challenge.getLastSentAt().isAfter(now.minusSeconds(60))) {
            return;
        }
        if (challenge.getWindowStartedAt() == null
                || !challenge.getWindowStartedAt().isAfter(now.minusHours(1))) {
            challenge.setWindowStartedAt(now);
            challenge.setSendCount(0);
        }
        if (challenge.getSendCount() >= MAX_EMAILS_PER_HOUR) {
            return;
        }

        String otp = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
        SimpleMailMessage message = new SimpleMailMessage();
        if (mailFrom != null && !mailFrom.isBlank()) {
            message.setFrom(mailFrom);
        }
        message.setTo(email);
        message.setSubject("ProjectHub password reset code");
        message.setText("Your ProjectHub password reset code is " + otp
                + ". It expires in 5 minutes. If you did not request this, you can ignore this email.");

        try {
            mailSender.send(message);
        } catch (RuntimeException exception) {
            throw new EmailDeliveryException(
                    "Password reset email could not be sent. Check the SMTP settings and try again.");
        }

        challenge.setOtpHash(passwordEncoder.encode(otp));
        challenge.setExpiresAt(now.plusMinutes(5));
        challenge.setAttempts(0);
        challenge.setLastSentAt(now);
        challenge.setSendCount(challenge.getSendCount() + 1);
        challenge.setVerifiedTokenHash(null);
        challenge.setVerifiedUntil(null);
        resetChallengeRepository.save(challenge);
    }

    private String hashResetGrant(String resetGrant) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(resetGrant.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Password reset is unavailable.");
        }
    }

    @Transactional(readOnly = true)
    public UserDto getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return toDto(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        String token = jwtService.generateToken(
                user.getEmail(), user.getId(), user.getRole().name());

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresInMs(jwtService.getExpirationMs())
                .user(toDto(user))
                .build();
    }

    private UserDto toDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .rollNo(user.getRollNo())
                .department(user.getDepartment())
                .year(user.getYear())
                .academicYear(user.getAcademicYear())
                .avatar(user.getAvatar())
                .role(user.getRole().name())
                .likedProjects(likeRepository.findProjectIdsByUserId(user.getId()))
                .bookmarks(bookmarkRepository.findProjectIdsByUserId(user.getId()))
                .build();
    }
}
