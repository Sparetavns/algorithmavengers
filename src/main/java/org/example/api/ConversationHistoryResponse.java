package org.example.api;

import java.util.List;

/**
 * Response for GET /api/load: list of conversation messages (role + content).
 */
public class ConversationHistoryResponse {

    private final List<HistoryMessage> history;

    public ConversationHistoryResponse(List<HistoryMessage> history) {
        this.history = history;
    }

    public List<HistoryMessage> getHistory() {
        return history;
    }

    public static class HistoryMessage {
        private final String role;
        private final String content;

        public HistoryMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public String getRole() {
            return role;
        }

        public String getContent() {
            return content;
        }
    }
}
