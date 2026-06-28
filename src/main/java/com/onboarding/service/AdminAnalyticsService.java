package com.onboarding.service;

import com.onboarding.dto.InsightsResponse;
import com.onboarding.dto.ProgressResponse;
import com.onboarding.entity.OnboardingChecklist;
import com.onboarding.entity.Role;
import com.onboarding.entity.User;
import com.onboarding.repository.InsightsDao;
import com.onboarding.repository.OnboardingChecklistItemRepository;
import com.onboarding.repository.OnboardingChecklistRepository;
import com.onboarding.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Backs the admin dashboard: per-hire onboarding progress and company-wide question insights. */
@Service
public class AdminAnalyticsService {

    private final UserRepository userRepository;
    private final OnboardingChecklistRepository checklistRepository;
    private final OnboardingChecklistItemRepository checklistItemRepository;
    private final InsightsDao insightsDao;

    public AdminAnalyticsService(UserRepository userRepository,
                                 OnboardingChecklistRepository checklistRepository,
                                 OnboardingChecklistItemRepository checklistItemRepository,
                                 InsightsDao insightsDao) {
        this.userRepository = userRepository;
        this.checklistRepository = checklistRepository;
        this.checklistItemRepository = checklistItemRepository;
        this.insightsDao = insightsDao;
    }

    @Transactional(readOnly = true)
    public List<ProgressResponse> progress(UUID companyId) {
        List<User> hires = userRepository.findByCompanyIdAndRole(companyId, Role.NEW_HIRE);
        return hires.stream().map(hire -> {
            List<OnboardingChecklist> checklists =
                    checklistRepository.findByCompanyIdAndUserIdOrderByAssignedAtDesc(companyId, hire.getId());
            int total = 0;
            int completed = 0;
            for (OnboardingChecklist c : checklists) {
                total += (int) checklistItemRepository.countByChecklistId(c.getId());
                completed += (int) checklistItemRepository.countByChecklistIdAndCompletedTrue(c.getId());
            }
            int percent = total == 0 ? 0 : Math.round(completed * 100f / total);
            return new ProgressResponse(hire.getId(), hire.getFullName(), hire.getEmail(),
                    checklists.size(), total, completed, percent);
        }).toList();
    }

    @Transactional(readOnly = true)
    public InsightsResponse insights(UUID companyId) {
        Map<String, Long> counts = insightsDao.counts(companyId);
        List<InsightsResponse.QuestionCount> top = insightsDao.topQuestions(companyId, 10).stream()
                .map(q -> new InsightsResponse.QuestionCount(q.question(), q.count()))
                .toList();
        List<InsightsResponse.Gap> gaps = insightsDao.recentGaps(companyId, 20).stream()
                .map(g -> new InsightsResponse.Gap(g.question(), g.askedAt()))
                .toList();
        return new InsightsResponse(
                counts.getOrDefault("totalQuestions", 0L),
                counts.getOrDefault("unanswered", 0L),
                top, gaps);
    }
}
