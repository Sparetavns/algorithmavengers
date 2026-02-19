package org.example.voicebot;

import com.google.gson.annotations.SerializedName;

/**
 * One knowledge item from the JSON config (category, issue, customer_query, agent_response).
 */
public class KnowledgeEntry {
    private String category;
    private String issue;
    @SerializedName("customer_query")
    private String customerQuery;
    @SerializedName("agent_response")
    private String agentResponse;

    public KnowledgeEntry() {}

    public KnowledgeEntry(String category, String issue, String customerQuery, String agentResponse) {
        this.category = category;
        this.issue = issue;
        this.customerQuery = customerQuery;
        this.agentResponse = agentResponse;
    }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getIssue() { return issue; }
    public void setIssue(String issue) { this.issue = issue; }
    public String getCustomerQuery() { return customerQuery; }
    public void setCustomerQuery(String customerQuery) { this.customerQuery = customerQuery; }
    public String getAgentResponse() { return agentResponse; }
    public void setAgentResponse(String agentResponse) { this.agentResponse = agentResponse; }
}
