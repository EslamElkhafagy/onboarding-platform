package com.onboarding.ai;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Offline, $0 chat for the "free" provider mode (app.ai.provider=free). No network, no keys:
 * instead of calling an LLM it returns an extractive answer built from the retrieved context
 * excerpts that {@code RagService} packs into the user prompt. Honest about being a stub so it
 * isn't mistaken for a real generated answer.
 *
 * RagService only calls this when relevant chunks were found, so there is always context to echo.
 */
@Component
@ConditionalOnProperty(prefix = "app.ai", name = "provider", havingValue = "free")
public class LocalStubChatClient implements ChatClient {

    private static final int MAX_CHARS = 1200;

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        String context = extractContext(userPrompt);
        StringBuilder sb = new StringBuilder("Based on your company's documents:\n\n");
        if (context.isEmpty()) {
            sb.append("(no excerpt text was available)");
        } else if (context.length() > MAX_CHARS) {
            sb.append(context, 0, MAX_CHARS).append("…");
        } else {
            sb.append(context);
        }
        sb.append("\n\n— Generated in offline free mode (app.ai.provider=free); ")
          .append("no LLM was called. Set AI keys and switch to a paid/ollama provider for a real answer.");
        return sb.toString();
    }

    /** Pulls the text RagService places between "Context excerpts:" and "Question:". */
    private String extractContext(String userPrompt) {
        if (userPrompt == null) return "";
        String marker = "Context excerpts:";
        int start = userPrompt.indexOf(marker);
        start = (start < 0) ? 0 : start + marker.length();
        int end = userPrompt.lastIndexOf("Question:");
        if (end < 0 || end < start) end = userPrompt.length();
        return userPrompt.substring(start, end).trim();
    }
}
