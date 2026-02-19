package org.example.voicebot;

/**
 * Builds the system prompt: role + rules → knowledge base → (single) context schema + data → safety.
 * Used with context classification so only the relevant context is included.
 */
public class VoiceBotPromptBuilder {

    private static final String ROLE_AND_RULES =
        "You are a friendly telecom customer support agent. Answer only using the information provided below. "
        + "Keep replies concise and helpful (1–3 sentences). "
        + "Match the customer's question to the closest customer_query and respond in the same style as the corresponding agent_response. "
        + "If the answer is not in the provided data, say you don't have that information and suggest calling support or checking the app.";

    private static final String SAFETY =
        "Do not make up plan names, prices, or policies. "
        + "If the customer asks something not covered above, say you don't have that information and offer to transfer to an agent or suggest the app/website.";

    /**
     * Builds the system prompt with only one context's schema and data (no full catalog, no other contexts).
     * Use after AI has classified the query to a single context so only relevant customer data is sent.
     */
    public static String buildSystemPromptForContext(KnowledgeBase knowledge, ContextCatalog catalog,
                                                     String selectedContextName, String singleContextDataSection) {
        StringBuilder sb = new StringBuilder();
        sb.append(ROLE_AND_RULES).append("\n\n");
        if (knowledge != null) sb.append(knowledge.toPromptSection()).append("\n");
        if (catalog != null && selectedContextName != null) sb.append(catalog.toPromptSectionForContext(selectedContextName)).append("\n");
        if (singleContextDataSection != null && !singleContextDataSection.isBlank()) sb.append(singleContextDataSection).append("\n");
        sb.append(SAFETY);
        return sb.toString();
    }
}
