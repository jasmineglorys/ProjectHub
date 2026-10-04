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
import jakarta.mail.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class AuthService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int MAX_EMAILS_PER_HOUR = 5;
    private static final String RESET_EMAIL_MESSAGE =
        "If an account exists for that email, a password reset link has been sent.";

    private final UserRepository userRepository;
    private final ProjectLikeRepository likeRepository;
    private final BookmarkRepository bookmarkRepository;
    private final PasswordResetChallengeRepository resetChallengeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JavaMailSender mailSender;
    private final String smtpHost;
    private final int smtpPort;
    private final String smtpUsername;
    private final String smtpPassword;
    private final String mailFrom;
    private final String frontendUrl;

    public AuthService(UserRepository userRepository,
                       ProjectLikeRepository likeRepository,
                       BookmarkRepository bookmarkRepository,
               PasswordResetChallengeRepository resetChallengeRepository,
                       PasswordEncoder passwordEncoder,
               JwtService jwtService,
               JavaMailSender mailSender,
                       @Value("${spring.mail.host:}") String smtpHost,
                       @Value("${spring.mail.port:587}") int smtpPort,
                       @Value("${spring.mail.username:}") String smtpUsername,
                       @Value("${spring.mail.password:}") String smtpPassword,
                       @Value("${app.mail.from:}") String mailFrom,
                       @Value("${app.frontend.url:http://localhost:5173}") String frontendUrl) {
        this.userRepository = userRepository;
        this.likeRepository = likeRepository;
        this.bookmarkRepository = bookmarkRepository;
     this.resetChallengeRepository = resetChallengeRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
     this.mailSender = mailSender;
     this.smtpHost = smtpHost;
    this.smtpPort = smtpPort;
     this.smtpUsername = smtpUsername;
     this.smtpPassword = smtpPassword;
     this.mailFrom = mailFrom;
        this.frontendUrl = frontendUrl == null || frontendUrl.isBlank()
            ? "http://localhost:5173"
            : frontendUrl.trim().replaceAll("/+$", "");
        LOGGER.info("SMTP configuration loaded: host={}, port={}, username configured={}, password configured={}, sender configured={}, STARTTLS enabled=true, authentication enabled=true",
            smtpHost, smtpPort, !smtpUsername.isBlank(), !smtpPassword.isBlank(),
            mailFrom != null && !mailFrom.isBlank());
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
        userRepository.findByEmailIgnoreCase(normalizedEmail).ifPresent(user -> {
            try {
                sendResetLink(user);
            } catch (EmailDeliveryException exception) {
                LOGGER.warn("Password reset request completed with a generic response after email delivery failed.");
            }
        });
        return new MessageResponse(RESET_EMAIL_MESSAGE);
    }

    private void ensureSmtpConfigured() {
        if (smtpHost.isBlank() || smtpUsername.isBlank() || smtpPassword.isBlank()) {
            throw new EmailDeliveryException(
                    "Password reset email is not configured. Set SMTP_HOST, SMTP_USERNAME, and SMTP_PASSWORD, then restart the backend.");
        }
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        if (token == null || token.isBlank()) {
            throw new BadRequestException("This password reset link has expired. Request a new link.");
        }
        if (!token.matches("[A-Za-z0-9_-]{43}")) {
            throw new BadRequestException("This password reset link is invalid. Request a new link.");
        }

        PasswordResetChallenge challenge = resetChallengeRepository
                .findByVerifiedTokenHash(hashResetGrant(token))
                .orElseThrow(() -> new BadRequestException(
                        "This password reset link is invalid or expired. Request a new link."));
        if (challenge.getVerifiedUntil() == null
                || !challenge.getVerifiedUntil().isAfter(LocalDateTime.now())) {
            resetChallengeRepository.delete(challenge);
            throw new BadRequestException("This password reset link has expired. Request a new link.");
        }

        User user = challenge.getUser();
        if (user == null) {
            user = userRepository.findByEmailIgnoreCase(challenge.getEmail())
                .orElseThrow(() -> new BadRequestException(
                    "This password reset link is invalid or expired. Request a new link."));
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        resetChallengeRepository.delete(challenge);
    }

    private void sendResetLink(User user) {
        String email = user.getEmail();
        LocalDateTime now = LocalDateTime.now();
        PasswordResetChallenge challenge = resetChallengeRepository.findLockedByEmail(email)
                .orElseGet(() -> {
                    PasswordResetChallenge created = new PasswordResetChallenge();
                    created.setEmail(email);
                    return created;
                });
        challenge.setUser(user);

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

        byte[] tokenBytes = new byte[32];
        SECURE_RANDOM.nextBytes(tokenBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        String resetUrl = frontendUrl.replaceAll("/$", "") + "/reset-password?token=" + token;
        String escapedResetUrl = HtmlUtils.htmlEscape(resetUrl);
        String text = "Hello,\n\nWe received a request to reset your ProjectHub password.\n\n"
                + "Use this link to create a new password: " + resetUrl + "\n\n"
                + "This link will expire after 30 minutes and can only be used once.\n\n"
                + "If you did not request a password reset, you can safely ignore this email.\n\n"
                + "Regards,\nACCET ProjectHub Team";
        String html = "<div style=\"font-family:Arial,sans-serif;line-height:1.6;color:#1f2937;max-width:560px;margin:0 auto\">"
                + "<h2 style=\"color:#183153\">Reset Your ProjectHub Password</h2>"
                + "<p>Hello,</p><p>We received a request to reset your ProjectHub password.</p>"
                + "<p>Click below to create a new password:</p>"
                + "<p><a href=\"" + escapedResetUrl + "\" style=\"display:inline-block;padding:12px 20px;"
                + "background:#183153;color:#fff;text-decoration:none;border-radius:6px\">Reset Password</a></p>"
                + "<p>If the button does not work, open this link:<br><a href=\"" + escapedResetUrl + "\">"
                + escapedResetUrl + "</a></p><p>This link will expire after 30 minutes and can only be used once.</p>"
                + "<p>If you did not request a password reset, you can safely ignore this email.</p>"
                + "<p>Regards,<br>ACCET ProjectHub Team</p></div>";

        try {
            MimeMessageHelper message = new MimeMessageHelper(
                    mailSender.createMimeMessage(), true, StandardCharsets.UTF_8.name());
            if (mailFrom != null && !mailFrom.isBlank()) {
                message.setFrom(mailFrom);
            }
            message.setTo(email);
            message.setSubject("Reset Your ProjectHub Password");
            message.setText(text, html);
            mailSender.send(message.getMimeMessage());
        } catch (MessagingException | RuntimeException exception) {
            LOGGER.error("Password reset email delivery failed. Check SMTP host, authentication, and sender settings.",
                    exception);
            throw new EmailDeliveryException(
                    "Password reset email could not be sent. Check backend logs for the SMTP error; Gmail requires an App Password.");
        }

        challenge.setOtpHash(null);
        challenge.setExpiresAt(null);
        challenge.setAttempts(0);
        challenge.setLastSentAt(now);
        challenge.setSendCount(challenge.getSendCount() + 1);
        challenge.setVerifiedTokenHash(hashResetGrant(token));
        challenge.setVerifiedUntil(now.plusMinutes(30));
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
