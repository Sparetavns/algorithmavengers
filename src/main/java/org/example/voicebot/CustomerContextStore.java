package org.example.voicebot;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Holds customer data per context (table) name.
 * Each context name maps to a key-value map of field names to values.
 * Used with ContextCatalog so the prompt includes both schema info and actual data;
 * the model decides which context to use for the customer's question.
 */
public class CustomerContextStore {
    private final Map<String, Map<String, String>> contextData;

    public CustomerContextStore() {
        this.contextData = new LinkedHashMap<>();
    }

    /**
     * Set or replace all data for a given context (e.g. "balance_and_usage", "loans").
     */
    public void putContext(String contextName, Map<String, String> data) {
        if (contextName == null || contextName.isBlank()) return;
        contextData.put(contextName, data != null ? new LinkedHashMap<>(data) : new LinkedHashMap<>());
    }

    /**
     * Set a single field in a context. Creates the context if missing.
     */
    public void put(String contextName, String field, String value) {
        if (contextName == null || contextName.isBlank()) return;
        contextData.computeIfAbsent(contextName, k -> new LinkedHashMap<>()).put(field, value);
    }

    public Map<String, String> getContext(String contextName) {
        Map<String, String> data = contextData.get(contextName);
        return data == null ? Map.of() : new LinkedHashMap<>(data);
    }

    public Map<String, Map<String, String>> getAllContexts() {
        Map<String, Map<String, String>> copy = new LinkedHashMap<>();
        contextData.forEach((k, v) -> copy.put(k, new LinkedHashMap<>(v)));
        return copy;
    }

    /**
     * Build the "Current customer data" section for a single context only.
     * Used when the model has already been given only one context's schema.
     */
    public String toPromptSectionForContext(String contextName) {
        Map<String, String> data = getContext(contextName);
        if (data == null || data.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        sb.append("## Current customer data (for this context)\n\n");
        sb.append("Answer only from the data below.\n\n");
        sb.append("### ").append(contextName).append("\n");
        for (Map.Entry<String, String> field : data.entrySet()) {
            if (field.getValue() != null && !field.getValue().isBlank()) {
                sb.append("- ").append(field.getKey()).append(": ").append(field.getValue()).append("\n");
            }
        }
        sb.append("\n");
        return sb.toString();
    }

    /**
     * Demo store with balance_and_usage and loans data. In production, replace with a store
     * that fetches only the relevant context from your database (by context name).
     */
    public static CustomerContextStore fromDemoData() {
        CustomerContextStore store = new CustomerContextStore();
        Map<String, String> balance = new LinkedHashMap<>();
        balance.put("customer_id", "CUST-1001");
        balance.put("plan_name", "Standard (5GB/day, 56-day)");
        balance.put("balance", "₹47");
        balance.put("data_remaining", "3.2 GB (resets at midnight)");
        balance.put("data_used", "1.8 GB today");
        balance.put("talk_time_used", "120 minutes this month");
        balance.put("validity_end_date", "2025-03-15");
        balance.put("last_recharge_date", "2025-02-01");
        balance.put("last_recharge_amount", "₹299");
        balance.put("active_offers", "None");
        balance.put("call_history_summary", "Last 5: 2 min out, 1 min in, 0.5 min out, 3 min in, 1 min out");
        store.putContext("balance_and_usage", balance);
        store.putContext("loans", Map.of(
            "customer_id", "CUST-1001",
            "has_active_loan", "true",
            "loan_type", "Device loan",
            "outstanding_amount", "₹4,200",
            "emi_amount", "₹700",
            "next_emi_date", "2025-03-01",
            "loan_tenure_months", "6",
            "eligibility_for_advance", "Yes (bill advance up to ₹500)"
        ));
        return store;
    }
}
