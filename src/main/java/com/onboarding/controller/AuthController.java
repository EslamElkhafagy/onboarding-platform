package com.onboarding.controller;

import com.onboarding.dto.*;
import com.onboarding.security.AuthPrincipal;
import com.onboarding.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(req));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }

    @PostMapping("/invite")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InviteResponse> invite(@AuthenticationPrincipal AuthPrincipal me,
                                                 @Valid @RequestBody InviteRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.invite(me.companyId(), me.userId(), req));
    }

    /** Public: preview an invite (who/what it's for) before the hire sets a password. */
    @GetMapping("/invitations/{token}")
    public InvitePreviewResponse previewInvite(@PathVariable String token) {
        return authService.previewInvite(token);
    }

    /** Public: a hire exchanges their invite token for a password and is logged in. */
    @PostMapping("/set-password")
    public AuthResponse setPassword(@Valid @RequestBody SetPasswordRequest req) {
        return authService.setPassword(req);
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal AuthPrincipal me) {
        return authService.me(me.userId(), me.companyId());
    }
}
