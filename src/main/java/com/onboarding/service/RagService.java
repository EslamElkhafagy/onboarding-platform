package com.onboarding.service;

import com.onboarding.ai.ChatClient;
import com.onboarding.ai.EmbeddingClient;
import com.onboarding.config.ApiException;
import com.onboarding.dto.*;
import com.onboarding.entity.*;
import com.onboarding.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Retrieval-augmented chat: embed the question, pull the most similar chunks within the
 * caller's company, and have the LLM answer strictly from them. If nothing relevant is
 * found we don't call the LLM — we return a fallback pointing to a human and record
 * was_answered = false, which feeds the admin "documentation gaps" insights.
 */
@Service
public class RagService {

    private static final String FALLBACK_ANSWER =
            "I couldn't find anything about that in your company's documents. "
            + "Please reach out to your HR contact, and consider flagging this so the docs can be improved.";

    private static final String SYSTEM_PROMPT = """
            You are an onboarding assistant for a company's new hires. Answer ONLY using the
            provided context excerpts from the company's documents. If the context does not
            contain the answer, say you don't know and suggest contacting HR — do not invent
            facts. Be concise and friendly. Cite nothing inline; citations are tracked separately.
            """;

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final MessageSourceRepository messageSourceRepository;
    private final DocumentRepository documentRepository;
    private final ChunkVectorDao vectorDao;
    private final EmbeddingClient embeddingClient;
    private final ChatClient chatClient;

    private final int topK;
    private final double minScore;

    public RagService(ConversationRepository conversationRepository,
                      MessageRepository messageRepository,
                      MessageSourceRepository messageSourceRepository,
                      DocumentRepository documentRepository,
                      ChunkVectorDao vectorDao,
                      EmbeddingClient embeddingClient,
                      ChatClient chatClient,
                      @Value("${app.rag.top-k:5}") int topK,
                      @Value("${app.rag.min-score:0.25}") double minScore) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.messageSourceRepository = messageSourceRepository;
        this.documentRepository = documentRepository;
        this.vectorDao = vectorDao;
        this.embeddingClient = embeddingClient;
        this.chatClient = chatClient;
        this.topK = topK;
        this.minScore = minScore;
    }

    @Transactional
    public AnswerResponse ask(UUID companyId, UUID userId, AskRequest req) {
        Conversation conversation = resolveConversation(companyId, userId, req);

        // Record the question.
        Message userMessage = new Message();
        userMessage.setConversationId(conversation.getId());
        userMessage.setRole(MessageRole.USER);
        userMessage.setContent(req.question());
        messageRepository.save(userMessage);

        // Retrieve relevant chunks, scoped to this company (and to one document if requested).
        float[] queryEmbedding = embeddingClient.embed(req.question());
        List<ChunkVectorDao.Match> matches = vectorDao.search(companyId, req.documentId(), queryEmbedding, topK)
                .stream()
                .filter(m -> m.score() >= minScore)
                .toList();

        Message assistantMessage = new Message();
        assistantMessage.setConversationId(conversation.getId());
        assistantMessage.setRole(MessageRole.ASSISTANT);

        List<SourceView> sources;
        if (matches.isEmpty()) {
            assistantMessage.setContent(FALLBACK_ANSWER);
            assistantMessage.setWasAnswered(false);
            messageRepository.save(assistantMessage);
            sources = List.of();
        } else {
            String answer = chatClient.complete(SYSTEM_PROMPT, buildUserPrompt(req.question(), matches));
            assistantMessage.setContent(answer);
            assistantMessage.setWasAnswered(true);
            messageRepository.save(assistantMessage);
            sources = persistSources(companyId, assistantMessage.getId(), matches);
        }

        return new AnswerResponse(conversation.getId(), assistantMessage.getId(),
                assistantMessage.getContent(), Boolean.TRUE.equals(assistantMessage.getWasAnswered()), sources);
    }

    @Transactional(readOnly = true)
    public List<ConversationResponse> listConversations(UUID userId) {
        return conversationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(ConversationResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ConversationDetailResponse getConversation(UUID companyId, UUID userId, UUID conversationId) {
        Conversation conversation = conversationRepository
                .findByIdAndUserIdAndCompanyId(conversationId, userId, companyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Conversation not found"));

        List<Message> messages = messageRepository.findByConversationIdOrderByCreatedAt(conversationId);
        List<MessageView> views = messages.stream().map(m -> {
            List<SourceView> srcs = m.getRole() == MessageRole.ASSISTANT
                    ? toSourceViews(companyId, messageSourceRepository.findByMessageId(m.getId()))
                    : List.of();
            return new MessageView(m.getId(), m.getRole().name(), m.getContent(),
                    m.getWasAnswered(), m.getCreatedAt(), srcs);
        }).toList();

        return new ConversationDetailResponse(conversation.getId(), conversation.getTitle(),
                conversation.getCreatedAt(), views);
    }

    private Conversation resolveConversation(UUID companyId, UUID userId, AskRequest req) {
        if (req.conversationId() != null) {
            return conversationRepository
                    .findByIdAndUserIdAndCompanyId(req.conversationId(), userId, companyId)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Conversation not found"));
        }
        Conversation c = new Conversation();
        c.setCompanyId(companyId);
        c.setUserId(userId);
        c.setTitle(truncate(req.question(), 80));
        return conversationRepository.save(c);
    }

    private String buildUserPrompt(String question, List<ChunkVectorDao.Match> matches) {
        StringBuilder sb = new StringBuilder("Context excerpts:\n\n");
        int i = 1;
        for (ChunkVectorDao.Match m : matches) {
            sb.append("[").append(i++).append("] ").append(m.content()).append("\n\n");
        }
        sb.append("Question: ").append(question);
        return sb.toString();
    }

    private List<SourceView> persistSources(UUID companyId, UUID messageId, List<ChunkVectorDao.Match> matches) {
        Map<UUID, String> filenames = filenamesFor(companyId, matches);
        List<SourceView> views = new ArrayList<>(matches.size());
        for (ChunkVectorDao.Match m : matches) {
            MessageSource src = new MessageSource();
            src.setMessageId(messageId);
            src.setDocumentId(m.documentId());
            src.setChunkId(m.chunkId());
            src.setScore((float) m.score());
            messageSourceRepository.save(src);
            views.add(new SourceView(m.documentId(), filenames.get(m.documentId()),
                    m.chunkId(), (float) m.score()));
        }
        return views;
    }

    private List<SourceView> toSourceViews(UUID companyId, List<MessageSource> sources) {
        Set<UUID> docIds = sources.stream().map(MessageSource::getDocumentId).collect(Collectors.toSet());
        Map<UUID, String> filenames = new HashMap<>();
        for (UUID docId : docIds) {
            documentRepository.findByIdAndCompanyId(docId, companyId)
                    .ifPresent(d -> filenames.put(docId, d.getFilename()));
        }
        return sources.stream()
                .map(s -> new SourceView(s.getDocumentId(), filenames.get(s.getDocumentId()),
                        s.getChunkId(), s.getScore()))
                .toList();
    }

    private Map<UUID, String> filenamesFor(UUID companyId, List<ChunkVectorDao.Match> matches) {
        Map<UUID, String> filenames = new HashMap<>();
        for (UUID docId : matches.stream().map(ChunkVectorDao.Match::documentId).collect(Collectors.toSet())) {
            documentRepository.findByIdAndCompanyId(docId, companyId)
                    .ifPresent(d -> filenames.put(docId, d.getFilename()));
        }
        return filenames;
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
