package com.onboarding.controller;

import com.onboarding.dto.*;
import com.onboarding.entity.Role;
import com.onboarding.security.AuthPrincipal;
import com.onboarding.service.AuditService;
import com.onboarding.service.ChecklistService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class ChecklistController {

    private final ChecklistService checklistService;
    private final AuditService auditService;

    public ChecklistController(ChecklistService checklistService, AuditService auditService) {
        this.checklistService = checklistService;
        this.auditService = auditService;
    }

    // --- Templates (admin) ---

    @PostMapping("/checklist-templates")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TemplateResponse> createTemplate(@AuthenticationPrincipal AuthPrincipal me,
                                                           @Valid @RequestBody CreateTemplateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(checklistService.createTemplate(me.companyId(), me.userId(), req));
    }

    @GetMapping("/checklist-templates")
    @PreAuthorize("hasRole('ADMIN')")
    public List<TemplateResponse> listTemplates(@AuthenticationPrincipal AuthPrincipal me) {
        return checklistService.listTemplates(me.companyId());
    }

    @GetMapping("/checklist-templates/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public TemplateResponse getTemplate(@AuthenticationPrincipal AuthPrincipal me, @PathVariable UUID id) {
        return checklistService.getTemplate(me.companyId(), id);
    }

    @PostMapping("/checklist-templates/{id}/items")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TemplateItemResponse> addItem(@AuthenticationPrincipal AuthPrincipal me,
                                                        @PathVariable UUID id,
                                                        @Valid @RequestBody TemplateItemRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(checklistService.addItem(me.companyId(), id, req));
    }

    // --- Assignment + instances ---

    @PostMapping("/checklists/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ChecklistResponse> assign(@AuthenticationPrincipal AuthPrincipal me,
                                                    @Valid @RequestBody AssignChecklistRequest req) {
        ChecklistResponse created = checklistService.assign(me.companyId(), me.userId(), req);
        auditService.record(me.companyId(), me.userId(), "CHECKLIST_ASSIGNED", "checklist", created.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /** A new hire's own checklist(s). */
    @GetMapping("/checklists/me")
    public List<ChecklistResponse> myChecklists(@AuthenticationPrincipal AuthPrincipal me) {
        return checklistService.checklistsForUser(me.companyId(), me.userId());
    }

    /** An admin viewing a specific hire's checklist(s). */
    @GetMapping("/checklists/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public List<ChecklistResponse> userChecklists(@AuthenticationPrincipal AuthPrincipal me,
                                                  @PathVariable UUID userId) {
        return checklistService.checklistsForUser(me.companyId(), userId);
    }

    @PatchMapping("/checklists/items/{itemId}")
    public ChecklistItemResponse completeItem(@AuthenticationPrincipal AuthPrincipal me,
                                              @PathVariable UUID itemId,
                                              @Valid @RequestBody CompleteItemRequest req) {
        boolean isAdmin = me.role() == Role.ADMIN;
        return checklistService.setItemCompleted(me.companyId(), me.userId(), isAdmin, itemId, req.completed());
    }
}
