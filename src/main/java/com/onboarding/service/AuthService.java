package com.onboarding.service;

import com.onboarding.config.ApiException;
import com.onboarding.dto.*;
import com.onboarding.entity.Company;
import com.onboarding.entity.Role;
import com.onboarding.entity.User;
import com.onboarding.entity.UserInvite;
import com.onboarding.repository.CompanyRepository;
import com.onboarding.repository.UserInviteRepository;
import com.onboarding.repository.UserRepository;
import com.onboarding.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class AuthService {

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final UserInviteRepository userInviteRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditService auditService;
    private final long inviteExpiryHours;

    private static final SecureRandom RANDOM = new SecureRandom();

    public AuthService(CompanyRepository companyRepository,
                       UserRepository userRepository,
                       UserInviteRepository userInviteRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuditService auditService,
                       @Value("${app.invite.expiry-hours:168}") long inviteExpiryHours) {
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
        this.userInviteRepository = userInviteRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.auditService = auditService;
        this.inviteExpiryHours = inviteExpiryHours;
    }

    /** Creates a new company and its first ADMIN user. */
    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_TAKEN", "Email already in use");
        }
        Company company = companyRepository.save(new Company(req.companyName()));

        User user = new User();
        user.setCompanyId(company.getId());
        user.setEmail(req.email());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setRole(Role.ADMIN);
        user.setFullName(req.fullName());
        user = userRepository.save(user);

        return toAuthResponse(user);
    }

    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "BAD_CREDENTIALS", "Invalid email or password"));
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "BAD_CREDENTIALS", "Invalid email or password");
        }
        return toAuthResponse(user);
    }

    /**
     * Admin invites a new hire into their own company. The hire is created without a usable
     * password and an expiring, single-use invite token is issued; the raw token is returned
     * once so the admin can share the accept link.
     */
    @Transactional
    public InviteResponse invite(UUID companyId, UUID actorUserId, InviteRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_TAKEN", "Email already in use");
        }
        User user = new User();
        user.setCompanyId(companyId);
        user.setEmail(req.email());
        // Unusable placeholder; the hire sets a real password via the invite link.
        user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setRole(Role.NEW_HIRE);
        user.setFullName(req.fullName());
        user.setStartDate(req.startDate());
        user = userRepository.save(user);

        String rawToken = generateRawToken();
        UserInvite invite = new UserInvite();
        invite.setUserId(user.getId());
        invite.setCompanyId(companyId);
        invite.setTokenHash(hashToken(rawToken));
        invite.setExpiresAt(OffsetDateTime.now().plusHours(inviteExpiryHours));
        invite = userInviteRepository.save(invite);

        auditService.record(companyId, actorUserId, "USER_INVITED", "user", user.getId());

        UserResponse userResp = new UserResponse(user.getId(), user.getEmail(), user.getFullName(),
                user.getRole().name(), user.getCompanyId());
        return new InviteResponse(userResp, rawToken, invite.getExpiresAt());
    }

    /** Public: validates an invite token and returns who/what it's for, to greet the hire. */
    @Transactional(readOnly = true)
    public InvitePreviewResponse previewInvite(String token) {
        UserInvite invite = requireUsableInvite(token);
        User user = userRepository.findById(invite.getUserId())
                .orElseThrow(this::invalidInvite);
        Company company = companyRepository.findById(user.getCompanyId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Company not found"));
        return new InvitePreviewResponse(user.getEmail(), user.getFullName(), company.getName());
    }

    /** Public: the hire exchanges an invite token for a password, then is logged in. */
    @Transactional
    public AuthResponse setPassword(SetPasswordRequest req) {
        UserInvite invite = requireUsableInvite(req.token());
        User user = userRepository.findById(invite.getUserId())
                .orElseThrow(this::invalidInvite);

        user.setPasswordHash(passwordEncoder.encode(req.password()));
        userRepository.save(user);

        invite.setAcceptedAt(OffsetDateTime.now());
        userInviteRepository.save(invite);

        auditService.record(user.getCompanyId(), user.getId(), "INVITE_ACCEPTED", "user", user.getId());
        return toAuthResponse(user);
    }

    public UserResponse me(UUID userId, UUID companyId) {
        User user = userRepository.findByIdAndCompanyId(userId, companyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "User not found"));
        return new UserResponse(user.getId(), user.getEmail(), user.getFullName(),
                user.getRole().name(), user.getCompanyId());
    }

    /** Looks up an invite by raw token and asserts it is neither used nor expired. */
    private UserInvite requireUsableInvite(String token) {
        UserInvite invite = userInviteRepository.findByTokenHash(hashToken(token))
                .orElseThrow(this::invalidInvite);
        if (invite.getAcceptedAt() != null) {
            throw new ApiException(HttpStatus.GONE, "INVITE_USED",
                    "This invite has already been used. Try signing in instead.");
        }
        if (invite.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new ApiException(HttpStatus.GONE, "INVITE_EXPIRED",
                    "This invite link has expired. Ask your admin to send a new one.");
        }
        return invite;
    }

    private ApiException invalidInvite() {
        return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_INVITE", "This invite link is invalid.");
    }

    private String generateRawToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private AuthResponse toAuthResponse(User user) {
        String token = jwtService.generateToken(user);
        return new AuthResponse(token,
                new AuthResponse.UserView(user.getId(), user.getEmail(),
                        user.getRole().name(), user.getCompanyId()));
    }
}
