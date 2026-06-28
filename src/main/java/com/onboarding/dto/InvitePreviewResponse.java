package com.onboarding.dto;

/** Minimal, public-safe details shown on the accept-invite screen to greet the hire. */
public record InvitePreviewResponse(String email, String fullName, String companyName) {}
