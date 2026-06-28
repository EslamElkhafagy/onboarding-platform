package com.onboarding.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record InviteRequest(
        @Email @NotBlank String email,
        @NotBlank String fullName,
        LocalDate startDate
) {}
