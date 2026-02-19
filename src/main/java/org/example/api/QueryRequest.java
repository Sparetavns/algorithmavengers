package org.example.api;

/**
 * Request body for /api/query: the user query string only.
 * Conversation history is maintained on the backend, not sent from the UI.
 */
public class QueryRequest {

    private String query;

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }
}
