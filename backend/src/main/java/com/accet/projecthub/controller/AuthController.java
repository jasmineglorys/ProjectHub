package com.accet.projecthub.controller;

import com.accet.projecthub.dto.AuthResponse;
import com.accet.projecthub.dto.EmailRequest;
import com.accet.projecthub.dto.LoginRequest;
import com.accet.projecthub.dto.MessageResponse;
import com.accet.projecthub.dto.ResetPasswordRequest;
import com.accet.projecthub.dto.RegisterRequest;
import com.accet.projecthub.dto.UserDto;
import com.accet.projecthub.dto.VerifyOtpRequest;
import com.accet.projecthub.security.CustomUserDetails;
import com.accet.projecthub.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final boolean resetCookieSecure;
    private final String resetCookieSameSite;

    public AuthController(AuthService authService,
                          @Value("${app.password-reset.cookie-secure:false}") boolean resetCookieSecure,
                          @Value("${app.password-reset.cookie-same-site:Strict}") String resetCookieSameSite) {
        this.authService = authService;
        this.resetCookieSecure = resetCookieSecure;
        this.resetCookieSameSite = resetCookieSameSite;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody EmailRequest request) {
        return ResponseEntity.ok(authService.requestPasswordReset(request.getEmail()));
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<MessageResponse> resendOtp(@Valid @RequestBody EmailRequest request) {
        return ResponseEntity.ok(authService.resendPasswordResetOtp(request.getEmail()));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<MessageResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        String resetGrant = authService.verifyPasswordResetOtp(request.getEmail(), request.getOtp());
        ResponseCookie cookie = ResponseCookie.from("PH_PASSWORD_RESET", resetGrant)
                .httpOnly(true)
                .secure(resetCookieSecure)
                .sameSite(resetCookieSameSite)
                .path("/api/auth/reset-password")
                .maxAge(600)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new MessageResponse("Verification successful."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(
            @CookieValue(name = "PH_PASSWORD_RESET", required = false) String resetGrant,
            @Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(resetGrant, request.getNewPassword());
        ResponseCookie expiredCookie = ResponseCookie.from("PH_PASSWORD_RESET", "")
                .httpOnly(true)
                .secure(resetCookieSecure)
                .sameSite(resetCookieSameSite)
                .path("/api/auth/reset-password")
                .maxAge(0)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, expiredCookie.toString())
                .body(new MessageResponse("Password reset successfully."));
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> me(@AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(authService.getCurrentUser(principal.getId()));
    }
}
