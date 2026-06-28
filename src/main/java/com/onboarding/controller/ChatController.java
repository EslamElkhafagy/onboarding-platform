package com.onboarding.controller;

import com.onboarding.dto.*;
import com.onboarding.security.AuthPrincipal;
import com.onboarding.service.RagService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Chat endpoints for new hires (and admins). Asking a question runs the RAG flow and returns
 * a grounded answer with citations. Conversations are always scoped to the caller.
 */
@RestController
@RequestMapping("/api")
public class ChatController {

    private final RagService ragService;

    public ChatController(RagService ragService) {
        this.ragService = ragService;
    }

    /** Ask a question; starts a new conversation if conversationId is omitted. */
    @PostMapping("/chat/ask")
    public AnswerResponse ask(@AuthenticationPrincipal AuthPrincipal me,
                              @Valid @RequestBody AskRequest req) {
        return ragService.ask(me.companyId(), me.userId(), req);
    }

    @GetMapping("/conversations")
    public List<ConversationResponse> conversations(@AuthenticationPrincipal AuthPrincipal me) {
        return ragService.listConversations(me.userId());
    }

    @GetMapping("/conversations/{id}")
    public ConversationDetailResponse conversation(@AuthenticationPrincipal AuthPrincipal me,
                                                   @PathVariable UUID id) {
        return ragService.getConversation(me.companyId(), me.userId(), id);
    }
}
