package com.onboarding.controller;

import com.onboarding.dto.InsightsResponse;
import com.onboarding.dto.ProgressResponse;
import com.onboarding.dto.UserResponse;
import com.onboarding.repository.UserRepository;
import com.onboarding.security.AuthPrincipal;
import com.onboarding.service.AdminAnalyticsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Admin dashboard endpoints: onboarding progress and question insights. Company-scoped. */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminAnalyticsService analyticsService;
    private final UserRepository userRepository;

    public AdminController(AdminAnalyticsService analyticsService, UserRepository userRepository) {
        this.analyticsService = analyticsService;
        this.userRepository = userRepository;
    }

    @GetMapping("/progress")
    public List<ProgressResponse> progress(@AuthenticationPrincipal AuthPrincipal me) {
        return analyticsService.progress(me.companyId());
    }

    @GetMapping("/insights")
    public InsightsResponse insights(@AuthenticationPrincipal AuthPrincipal me) {
        return analyticsService.insights(me.companyId());
    }

    /** Everyone in the admin's company — used to pick who to assign a checklist to. */
    @GetMapping("/users")
    public List<UserResponse> users(@AuthenticationPrincipal AuthPrincipal me) {
        return userRepository.findByCompanyId(me.companyId()).stream()
                .map(u -> new UserResponse(u.getId(), u.getEmail(), u.getFullName(),
                        u.getRole().name(), u.getCompanyId()))
                .toList();
    }
}
