package com.onboarding.service;

import com.onboarding.config.ApiException;
import com.onboarding.dto.*;
import com.onboarding.entity.*;
import com.onboarding.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Checklist templates (reusable) and per-hire instances. Templates are admin-authored; when
 * one is assigned to a hire its items are COPIED into a fresh instance, so later template
 * edits never rewrite an in-progress checklist. All lookups are tenant-scoped by company.
 *
 * Items may have study documents attached (V6); those links are copied alongside the items at
 * assignment and surfaced on the hire's checklist so they can open and ask about the material.
 */
@Service
public class ChecklistService {

    private final ChecklistTemplateRepository templateRepository;
    private final ChecklistTemplateItemRepository templateItemRepository;
    private final OnboardingChecklistRepository checklistRepository;
    private final OnboardingChecklistItemRepository checklistItemRepository;
    private final ChecklistTemplateItemDocumentRepository templateItemDocRepository;
    private final OnboardingChecklistItemDocumentRepository checklistItemDocRepository;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;

    public ChecklistService(ChecklistTemplateRepository templateRepository,
                            ChecklistTemplateItemRepository templateItemRepository,
                            OnboardingChecklistRepository checklistRepository,
                            OnboardingChecklistItemRepository checklistItemRepository,
                            ChecklistTemplateItemDocumentRepository templateItemDocRepository,
                            OnboardingChecklistItemDocumentRepository checklistItemDocRepository,
                            DocumentRepository documentRepository,
                            UserRepository userRepository) {
        this.templateRepository = templateRepository;
        this.templateItemRepository = templateItemRepository;
        this.checklistRepository = checklistRepository;
        this.checklistItemRepository = checklistItemRepository;
        this.templateItemDocRepository = templateItemDocRepository;
        this.checklistItemDocRepository = checklistItemDocRepository;
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
    }

    // --- Templates (admin) ---

    @Transactional
    public TemplateResponse createTemplate(UUID companyId, UUID createdBy, CreateTemplateRequest req) {
        ChecklistTemplate template = new ChecklistTemplate();
        template.setCompanyId(companyId);
        template.setName(req.name());
        template.setCreatedBy(createdBy);
        template = templateRepository.save(template);
        return toTemplateResponse(template);
    }

    @Transactional(readOnly = true)
    public List<TemplateResponse> listTemplates(UUID companyId) {
        return templateRepository.findByCompanyIdOrderByCreatedAtDesc(companyId)
                .stream().map(this::toTemplateResponse).toList();
    }

    @Transactional(readOnly = true)
    public TemplateResponse getTemplate(UUID companyId, UUID templateId) {
        return toTemplateResponse(requireTemplate(companyId, templateId));
    }

    @Transactional
    public TemplateItemResponse addItem(UUID companyId, UUID templateId, TemplateItemRequest req) {
        requireTemplate(companyId, templateId);
        ChecklistTemplateItem item = new ChecklistTemplateItem();
        item.setTemplateId(templateId);
        item.setTitle(req.title());
        item.setDescription(req.description());
        item.setDueDay(req.dueDay());
        item.setPosition(req.position() != null
                ? req.position()
                : (int) templateItemRepository.countByTemplateId(templateId));
        item = templateItemRepository.save(item);

        List<UUID> docIds = validateDocumentIds(companyId, req.documentIds());
        for (UUID docId : docIds) {
            templateItemDocRepository.save(new ChecklistTemplateItemDocument(item.getId(), docId));
        }
        return TemplateItemResponse.from(item, refViews(companyId, docIds));
    }

    // --- Assignment + instances ---

    /** Copies a template's items (and their attached documents) into a new per-hire checklist. */
    @Transactional
    public ChecklistResponse assign(UUID companyId, UUID assignedBy, AssignChecklistRequest req) {
        ChecklistTemplate template = requireTemplate(companyId, req.templateId());
        userRepository.findByIdAndCompanyId(req.userId(), companyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "User not found"));

        OnboardingChecklist checklist = new OnboardingChecklist();
        checklist.setCompanyId(companyId);
        checklist.setUserId(req.userId());
        checklist.setTemplateId(template.getId());
        checklist.setAssignedBy(assignedBy);
        checklist = checklistRepository.save(checklist);

        List<ChecklistTemplateItem> templateItems =
                templateItemRepository.findByTemplateIdOrderByPosition(template.getId());
        Map<UUID, List<UUID>> docsByTemplateItem = documentIdsByTemplateItem(
                templateItems.stream().map(ChecklistTemplateItem::getId).toList());

        for (ChecklistTemplateItem ti : templateItems) {
            OnboardingChecklistItem item = new OnboardingChecklistItem();
            item.setChecklistId(checklist.getId());
            item.setTitle(ti.getTitle());
            item.setDescription(ti.getDescription());
            item.setDueDay(ti.getDueDay());
            item.setPosition(ti.getPosition());
            item.setCompleted(false);
            item = checklistItemRepository.save(item);

            for (UUID docId : docsByTemplateItem.getOrDefault(ti.getId(), List.of())) {
                checklistItemDocRepository.save(new OnboardingChecklistItemDocument(item.getId(), docId));
            }
        }
        return toChecklistResponse(checklist);
    }

    @Transactional(readOnly = true)
    public List<ChecklistResponse> checklistsForUser(UUID companyId, UUID userId) {
        return checklistRepository.findByCompanyIdAndUserIdOrderByAssignedAtDesc(companyId, userId)
                .stream().map(this::toChecklistResponse).toList();
    }

    /**
     * Toggles a checklist item's completion. New hires may only touch their own checklist;
     * admins may update any item within their company.
     */
    @Transactional
    public ChecklistItemResponse setItemCompleted(UUID companyId, UUID callerId, boolean isAdmin,
                                                  UUID itemId, boolean completed) {
        OnboardingChecklistItem item = checklistItemRepository.findById(itemId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Item not found"));
        OnboardingChecklist checklist = checklistRepository.findByIdAndCompanyId(item.getChecklistId(), companyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Item not found"));

        if (!isAdmin && !checklist.getUserId().equals(callerId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Not your checklist");
        }

        item.setCompleted(completed);
        item.setCompletedAt(completed ? OffsetDateTime.now() : null);
        item = checklistItemRepository.save(item);

        List<UUID> docIds = checklistItemDocRepository.findByChecklistItemId(itemId)
                .stream().map(OnboardingChecklistItemDocument::getDocumentId).toList();
        return ChecklistItemResponse.from(item, refViews(companyId, docIds));
    }

    // --- helpers ---

    private ChecklistTemplate requireTemplate(UUID companyId, UUID templateId) {
        return templateRepository.findByIdAndCompanyId(templateId, companyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Template not found"));
    }

    private TemplateResponse toTemplateResponse(ChecklistTemplate template) {
        UUID companyId = template.getCompanyId();
        List<ChecklistTemplateItem> items =
                templateItemRepository.findByTemplateIdOrderByPosition(template.getId());
        Map<UUID, List<UUID>> docsByItem = documentIdsByTemplateItem(
                items.stream().map(ChecklistTemplateItem::getId).toList());
        Map<UUID, String> names = filenames(companyId, allDocIds(docsByItem));

        List<TemplateItemResponse> itemViews = items.stream()
                .map(i -> TemplateItemResponse.from(i, toRefViews(docsByItem.get(i.getId()), names)))
                .toList();
        return new TemplateResponse(template.getId(), template.getName(), template.getCreatedAt(), itemViews);
    }

    private ChecklistResponse toChecklistResponse(OnboardingChecklist checklist) {
        UUID companyId = checklist.getCompanyId();
        List<OnboardingChecklistItem> items =
                checklistItemRepository.findByChecklistIdOrderByPosition(checklist.getId());
        Map<UUID, List<UUID>> docsByItem = documentIdsByChecklistItem(
                items.stream().map(OnboardingChecklistItem::getId).toList());
        Map<UUID, String> names = filenames(companyId, allDocIds(docsByItem));

        List<ChecklistItemResponse> itemViews = items.stream()
                .map(i -> ChecklistItemResponse.from(i, toRefViews(docsByItem.get(i.getId()), names)))
                .toList();
        int completed = (int) itemViews.stream().filter(ChecklistItemResponse::completed).count();
        return new ChecklistResponse(checklist.getId(), checklist.getUserId(), checklist.getTemplateId(),
                checklist.getAssignedAt(), itemViews.size(), completed, itemViews);
    }

    /** Verifies each id is a document in this company and returns a de-duplicated list. */
    private List<UUID> validateDocumentIds(UUID companyId, List<UUID> documentIds) {
        if (documentIds == null || documentIds.isEmpty()) return List.of();
        List<UUID> unique = new ArrayList<>(new LinkedHashSet<>(documentIds));
        for (UUID id : unique) {
            documentRepository.findByIdAndCompanyId(id, companyId)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND",
                            "Document not found: " + id));
        }
        return unique;
    }

    private Map<UUID, List<UUID>> documentIdsByTemplateItem(List<UUID> itemIds) {
        Map<UUID, List<UUID>> byItem = new HashMap<>();
        if (itemIds.isEmpty()) return byItem;
        for (ChecklistTemplateItemDocument link : templateItemDocRepository.findByTemplateItemIdIn(itemIds)) {
            byItem.computeIfAbsent(link.getTemplateItemId(), k -> new ArrayList<>()).add(link.getDocumentId());
        }
        return byItem;
    }

    private Map<UUID, List<UUID>> documentIdsByChecklistItem(List<UUID> itemIds) {
        Map<UUID, List<UUID>> byItem = new HashMap<>();
        if (itemIds.isEmpty()) return byItem;
        for (OnboardingChecklistItemDocument link : checklistItemDocRepository.findByChecklistItemIdIn(itemIds)) {
            byItem.computeIfAbsent(link.getChecklistItemId(), k -> new ArrayList<>()).add(link.getDocumentId());
        }
        return byItem;
    }

    private List<UUID> allDocIds(Map<UUID, List<UUID>> docsByItem) {
        List<UUID> all = new ArrayList<>();
        docsByItem.values().forEach(all::addAll);
        return all;
    }

    /** Maps a set of document ids to {id, filename} views (skips any not in this company). */
    private Map<UUID, String> filenames(UUID companyId, Collection<UUID> docIds) {
        Map<UUID, String> names = new HashMap<>();
        for (UUID id : new HashSet<>(docIds)) {
            documentRepository.findByIdAndCompanyId(id, companyId)
                    .ifPresent(d -> names.put(id, d.getFilename()));
        }
        return names;
    }

    /** Builds ref views for an item's doc ids using a prefetched filename map (preserves order). */
    private List<DocumentRefView> toRefViews(List<UUID> docIds, Map<UUID, String> names) {
        if (docIds == null || docIds.isEmpty()) return List.of();
        List<DocumentRefView> views = new ArrayList<>(docIds.size());
        for (UUID id : docIds) {
            if (names.containsKey(id)) views.add(new DocumentRefView(id, names.get(id)));
        }
        return views;
    }

    /** Convenience for the single-item paths (addItem, setItemCompleted). */
    private List<DocumentRefView> refViews(UUID companyId, List<UUID> docIds) {
        return toRefViews(docIds, filenames(companyId, docIds));
    }
}
