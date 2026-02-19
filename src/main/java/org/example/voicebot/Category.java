package org.example.voicebot;

import java.util.List;

/**
 * Category name plus the list of issues under that category (for classification).
 */
public class Category {
    private final String type;
    private final List<String> issues;

    public Category(String type, List<String> issues) {
        this.type = type;
        this.issues = issues != null ? List.copyOf(issues) : List.of();
    }

    public String getType() {
        return type;
    }

    public List<String> getIssues() {
        return issues;
    }
}
