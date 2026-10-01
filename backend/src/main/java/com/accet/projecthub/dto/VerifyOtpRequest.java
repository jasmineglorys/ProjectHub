package com.accet.projecthub.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyOtpRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid address")
    @Size(max = 150)
    private String email;

    @NotBlank(message = "Verification code is required")
    @Pattern(regexp = "[0-9]{6}", message = "Enter the 6-digit verification code")
    private String otp;
}