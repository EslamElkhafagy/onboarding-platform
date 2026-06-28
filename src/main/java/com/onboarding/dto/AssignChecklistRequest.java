package com.onboarding.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssignChecklistRequest(
        @NotNull UUID templateId,
        @NotNull UUID userId
) {}
