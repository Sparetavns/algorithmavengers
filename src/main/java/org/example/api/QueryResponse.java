package org.example.api;

/**
 * Response for /api/query: answer on success, error message on failure.
 */
public class QueryResponse {

    private final String answer;
    private final String error;

    public QueryResponse(String answer, String error) {
        this.answer = answer;
        this.error = error;
    }

    public String getAnswer() {
        return answer;
    }

    public String getError() {
        return error;
    }
}
