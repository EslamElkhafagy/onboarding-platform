package com.onboarding.ai;

/**
 * Generates a grounded answer from a system prompt + user prompt. Implementations talk to a
 * specific LLM provider; callers (the RAG service) depend only on this interface.
 */
public interface ChatClient {

    String complete(String systemPrompt, String userPrompt);
}
