package com.onboarding.dto;

import java.util.UUID;

/** Lightweight reference to a document attached to a checklist task (id + display name). */
public record DocumentRefView(UUID id, String filename) {}
