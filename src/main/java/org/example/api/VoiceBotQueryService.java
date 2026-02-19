package org.example.api;

import org.example.voicebot.ContextCatalog;
import org.example.voicebot.CustomerContextStore;
import org.example.voicebot.KnowledgeBase;
import org.example.voicebot.OpenAIService;
import org.example.voicebot.OpenAIService.ChatMessage;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Service that answers user queries using the same voicebot logic as Main:
 * knowledge base, context catalog, and OpenAI with category/context routing.
 * Maintains in-memory conversation history (max 10 messages) for follow-up questions.
 * Set OPENAI_API_KEY environment variable (or openai.api.key in application.properties).
 */
@Service
public class VoiceBotQueryService {

    private static final int MAX_HISTORY_MESSAGES = 10;

    private final Environment environment;
    private final List<ChatMessage> conversationHistory = Collections.synchronizedList(new ArrayList<>());

    private KnowledgeBase knowledge;
    private ContextCatalog contextCatalog;
    private CustomerContextStore customerData;
    private OpenAIService openAIService;

    public VoiceBotQueryService(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void init() {
        String apiKey = "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";
        if (apiKey == null || apiKey.isBlank()) {
            apiKey = environment.getProperty("openai.api.key", "");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY environment variable or openai.api.key property must be set.");
        }
        knowledge = KnowledgeBase.loadFromClasspath("knowledge.json");
        contextCatalog = ContextCatalog.loadFromClasspath("context_schemas.json");
        customerData = CustomerContextStore.fromDemoData();
        openAIService = new OpenAIService(apiKey);
    }

    /**
     * Answer a single query. Uses and updates in-service conversation history (max 10 messages).
     */
    public String answer(String query) {
        if (query == null || query.isBlank()) {
            return "Please provide a non-empty query.";
        }
        List<ChatMessage> historySnapshot;
        synchronized (conversationHistory) {
            historySnapshot = new ArrayList<>(conversationHistory);
        }
        String reply = openAIService.answerWithCategoryRouting(query, knowledge, contextCatalog, customerData, historySnapshot);
        synchronized (conversationHistory) {
            conversationHistory.add(new ChatMessage("user", query));
            conversationHistory.add(new ChatMessage("assistant", reply));
            while (conversationHistory.size() > MAX_HISTORY_MESSAGES) {
                conversationHistory.remove(0);
                conversationHistory.remove(0);
            }
        }
        return reply;
    }

    /**
     * Returns a snapshot of the current conversation history (max 10 messages).
     */
    public List<ConversationHistoryResponse.HistoryMessage> getConversationHistory() {
        synchronized (conversationHistory) {
            return conversationHistory.stream()
                .map(m -> new ConversationHistoryResponse.HistoryMessage(m.getRole(), m.getContent()))
                .toList();
        }
    }
}
