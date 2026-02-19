package org.example.voicebot;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

/**
 * OpenAI Chat Completions API for text input → text response.
 * Uses KnowledgeBase, ContextCatalog, and per-context customer data (only the relevant context is sent).
 */
public class OpenAIService {

    private static final String CHAT_URL = "https://api.openai.com/v1/chat/completions";
    private static final String DEFAULT_MODEL = "gpt-4o-mini";
    private static final double TEMPERATURE = 0.3;
    private static final int MAX_TOKENS = 256;
    private static final int CLASSIFY_MAX_TOKENS = 30;
    private static final double CLASSIFY_TEMPERATURE = 0;

    private final String apiKey;
    private final String model;
    private final Gson gson = new Gson();
    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(15))
        .build();

    public OpenAIService(String apiKey) {
        this(apiKey, DEFAULT_MODEL);
    }

    public OpenAIService(String apiKey, String model) {
        this.apiKey = apiKey;
        this.model = model != null ? model : DEFAULT_MODEL;
    }

    /**
     * Classifies which context (table) the query relates to via AI, then answers using only that context's data.
     * Only the relevant context's schema and customer data are sent to OpenAI, not all user info.
     */
    public String answerWithCategoryRouting(String customerQuery, KnowledgeBase fullKnowledge,
                                           ContextCatalog catalog, CustomerContextStore customerData) {
        return answerWithCategoryRouting(customerQuery, fullKnowledge, catalog, customerData, null);
    }

    /**
     * Same as above but with conversation history so follow-up questions (e.g. "What is the amount?")
     * are understood in context (e.g. loan amount after "Do I have an active loan?").
     */
    public String answerWithCategoryRouting(String customerQuery, KnowledgeBase fullKnowledge,
                                           ContextCatalog catalog, CustomerContextStore customerData,
                                           List<ChatMessage> conversationHistory) {
        // Use conversation context for classification when the current query is a follow-up (e.g. "What is the amount?")
        String queryForClassification = buildQueryWithContext(customerQuery, conversationHistory);

        // 1) Classify knowledge category for routing (balance, loans, etc.)
        List<Category> categories = fullKnowledge.getCategories();
        KnowledgeBase knowledge = fullKnowledge;
        if (categories != null && categories.size() > 1) {
            String category = classifyCategory(queryForClassification, categories);
            System.out.println("Category: " + category);
            if (category != null && !category.isBlank()) {
                KnowledgeBase filtered = fullKnowledge.forCategory(category);
                if (filtered != null && !filtered.getEntries().isEmpty()) knowledge = filtered;
            }
        } else if (categories != null && categories.size() == 1) {
            knowledge = fullKnowledge.forCategory(categories.get(0).getType());
        }

        // 2) Use AI to recognise which context (database table) the query is about — with conversation context
        String selectedContext = catalog != null ? classifyContext(queryForClassification, catalog) : null;
        System.out.println("Context: " + (selectedContext != null ? selectedContext : "(none)"));

        // 3) Build prompt with only the selected context's schema + data (never all contexts)
        String singleContextDataSection = null;
        if (customerData != null && selectedContext != null && !selectedContext.isBlank()) {
            singleContextDataSection = customerData.toPromptSectionForContext(selectedContext);
        }
        String systemPrompt = VoiceBotPromptBuilder.buildSystemPromptForContext(
            knowledge, catalog, selectedContext, singleContextDataSection);

        // 4) Send with conversation history so the model can resolve "it", "the amount", etc.
        if (conversationHistory != null && !conversationHistory.isEmpty()) {
            JsonArray messages = new JsonArray();
            messages.add(messageObject("system", systemPrompt));
            for (ChatMessage m : conversationHistory) {
                messages.add(messageObject(m.getRole(), m.getContent()));
            }
            messages.add(messageObject("user", customerQuery));
            return chat(messages, TEMPERATURE, MAX_TOKENS);
        }
        return chat(systemPrompt, customerQuery);
    }

    /**
     * Builds a string that includes recent conversation so classifiers understand follow-up questions.
     */
    private static String buildQueryWithContext(String currentQuery, List<ChatMessage> history) {
        if (history == null || history.isEmpty()) return currentQuery;
        // Include last 2 exchanges (4 messages) so "What is the amount?" has loan context
        int maxMessages = Math.min(4, history.size());
        int start = history.size() - maxMessages;
        StringBuilder sb = new StringBuilder();
        sb.append("Recent conversation:\n");
        for (int i = start; i < history.size(); i++) {
            ChatMessage m = history.get(i);
            sb.append(m.getRole()).append(": ").append(m.getContent()).append("\n");
        }
        sb.append("\nCurrent question: ").append(currentQuery);
        return sb.toString();
    }

    /**
     * Uses the context catalog only (no customer data) to pick which context/table the customer query relates to.
     * Returns the context name (e.g. "balance_and_usage", "loans") or null if none matched.
     */
    public String classifyContext(String customerQuery, ContextCatalog catalog) {
        if (catalog == null) return null;
        List<ContextSchema> contexts = catalog.getContexts();
        if (contexts == null || contexts.isEmpty()) return null;
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a classifier. The customer will ask a question. ");
        prompt.append("Reply with exactly one context name from the list below — the context (database table) that best matches the question.\n\n");
        for (ContextSchema ctx : contexts) {
            prompt.append("Context: ").append(ctx.getName()).append("\n");
            if (ctx.getDescription() != null) prompt.append("  Description: ").append(ctx.getDescription()).append("\n");
            if (ctx.getExampleQueries() != null && !ctx.getExampleQueries().isEmpty()) {
                prompt.append("  Example questions: ").append(String.join("; ", ctx.getExampleQueries())).append("\n");
            }
            prompt.append("\n");
        }
        prompt.append("Reply with only the context name, nothing else.");
        String raw = chat(prompt.toString(), customerQuery, CLASSIFY_TEMPERATURE, CLASSIFY_MAX_TOKENS);
        if (raw == null || raw.isBlank()) return null;
        String trimmed = raw.trim();
        for (ContextSchema ctx : contexts) {
            if (ctx.getName().equalsIgnoreCase(trimmed)) return ctx.getName();
        }
        return null;
    }

    /**
     * Uses category names and their issue lists to pick the one category that best matches the user query.
     */
    public String classifyCategory(String userQuery, List<Category> categories) {
        if (categories == null || categories.isEmpty()) return null;
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a classifier. For the customer message, reply with exactly one category name from the list below.\n\n");
        for (Category c : categories) {
            prompt.append("Category: ").append(c.getType()).append("\n");
            prompt.append("Issues: ").append(String.join(", ", c.getIssues())).append("\n\n");
        }
        prompt.append("Reply with only the category name, nothing else.");
        String raw = chat(prompt.toString(), userQuery, CLASSIFY_TEMPERATURE, CLASSIFY_MAX_TOKENS);
        if (raw == null || raw.isBlank()) return null;
        String trimmed = raw.trim();
        for (Category c : categories) {
            if (c.getType().equalsIgnoreCase(trimmed)) return c.getType();
        }
        return null;
    }

    public String chat(String systemPrompt, String userMessage) {
        return chat(systemPrompt, userMessage, TEMPERATURE, MAX_TOKENS);
    }

    private String chat(String systemPrompt, String userMessage, double temperature, int maxTokens) {
        JsonArray messages = new JsonArray();
        messages.add(messageObject("system", systemPrompt));
        messages.add(messageObject("user", userMessage));
        return chat(messages, temperature, maxTokens);
    }

    private JsonObject messageObject(String role, String content) {
        JsonObject o = new JsonObject();
        o.addProperty("role", role);
        o.addProperty("content", content);
        return o;
    }

    private String chat(JsonArray messages, double temperature, int maxTokens) {
        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        body.add("messages", messages);
        body.addProperty("temperature", temperature);
        body.addProperty("max_tokens", maxTokens);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(CHAT_URL))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + apiKey)
            .timeout(Duration.ofSeconds(30))
            .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
            .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                throw new OpenAIException("OpenAI API error: " + response.statusCode() + " " + response.body());
            }
            return extractContent(response.body());
        } catch (OpenAIException e) {
            throw e;
        } catch (Exception e) {
            throw new OpenAIException("Failed to call OpenAI: " + e.getMessage(), e);
        }
    }

    private String extractContent(String jsonBody) {
        JsonObject root = gson.fromJson(jsonBody, JsonObject.class);
        JsonArray choices = root.getAsJsonArray("choices");
        if (choices == null || choices.isEmpty()) {
            throw new OpenAIException("No choices in OpenAI response");
        }
        JsonObject first = choices.get(0).getAsJsonObject();
        JsonObject message = first.getAsJsonObject("message");
        if (message == null || !message.has("content")) {
            throw new OpenAIException("No message content in OpenAI response");
        }
        return message.get("content").getAsString();
    }

    public static class ChatMessage {
        private final String role;
        private final String content;

        public ChatMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }
        public String getRole() { return role; }
        public String getContent() { return content; }
    }

    public static class OpenAIException extends RuntimeException {
        public OpenAIException(String message) { super(message); }
        public OpenAIException(String message, Throwable cause) { super(message, cause); }
    }
}
