package org.example;

import org.example.voicebot.ContextCatalog;
import org.example.voicebot.CustomerContextStore;
import org.example.voicebot.KnowledgeBase;
import org.example.voicebot.OpenAIService;
import org.example.voicebot.OpenAIService.ChatMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Customer support bot: text in → text out using OpenAI and configurable JSON knowledge base.
 * Uses a context catalog (context_schemas.json) so the model can choose which customer context
 * (e.g. balance_and_usage, loans) to use for each question.
 */
public class Main {

    /** Optional: path to knowledge JSON file; otherwise uses classpath resource "knowledge.json". */
    private static final String KNOWLEDGE_PATH_ENV = "../../../src/main/resources/knowledge.json";
    private static final String CONTEXT_SCHEMAS_PATH_ENV = "../../../src/main/resources/context_schemas.json";
    private static final String OPENAI_API_KEY = "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";

    public static void main(String[] args) {
        String apiKey = OPENAI_API_KEY;
        if (apiKey == null || apiKey.isBlank()) {
            System.err.println("Set OPENAI_API_KEY environment variable.");
            return;
        }

        KnowledgeBase knowledge = loadKnowledge();
        ContextCatalog contextCatalog = loadContextCatalog();
        // Only the context relevant to the query is sent to OpenAI (AI picks context from catalog; we pass that slice only).
        CustomerContextStore customerData = CustomerContextStore.fromDemoData(); // Or null; or a store that fetches by context from DB.

        OpenAIService openAI = new OpenAIService(apiKey);

        // Keep conversation history so follow-ups like "What is the amount?" are understood in context (e.g. loan amount).
        List<ChatMessage> conversationHistory = new ArrayList<>();
        final int maxHistoryMessages = 10; // Last 5 exchanges to avoid token overflow

        System.out.println("Support bot ready. Type customer query (or 'quit' to exit).");
        System.out.println();

        try (Scanner sc = new Scanner(System.in)) {
            while (true) {
                System.out.print("Customer: ");
                if (!sc.hasNextLine()) break;
                String query = sc.nextLine().trim();
                if (query.isEmpty()) continue;
                if ("quit".equalsIgnoreCase(query)) break;

                try {
                    String reply = openAI.answerWithCategoryRouting(query, knowledge, contextCatalog, customerData, conversationHistory);
                    System.out.println("Bot: " + reply);
                    // Append this exchange to history for next turn
                    conversationHistory.add(new ChatMessage("user", query));
                    conversationHistory.add(new ChatMessage("assistant", reply));
                    while (conversationHistory.size() > maxHistoryMessages) {
                        conversationHistory.remove(0);
                        conversationHistory.remove(0);
                    }
                } catch (Exception e) {
                    System.err.println("Error: " + e.getMessage());
                }
                System.out.println();
            }
        }
        System.out.println("Goodbye.");
    }

    private static KnowledgeBase loadKnowledge() {
        String path = KNOWLEDGE_PATH_ENV;
        if (path != null && !path.isBlank()) {
            Path p = Path.of(path);
            if (Files.isRegularFile(p)) {
                return KnowledgeBase.loadFromFile(p);
            }
        }
        return KnowledgeBase.loadFromClasspath("knowledge.json");
    }

    private static ContextCatalog loadContextCatalog() {
        String path = CONTEXT_SCHEMAS_PATH_ENV;
        if (path != null && !path.isBlank()) {
            Path p = Path.of(path);
            if (Files.isRegularFile(p)) {
                return ContextCatalog.loadFromFile(p);
            }
        }
        return ContextCatalog.loadFromClasspath("context_schemas.json");
    }
}
