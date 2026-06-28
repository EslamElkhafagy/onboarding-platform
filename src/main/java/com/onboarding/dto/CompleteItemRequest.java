package com.onboarding.dto;

import jakarta.validation.constraints.NotNull;

public record CompleteItemRequest(@NotNull Boolean completed) {}
