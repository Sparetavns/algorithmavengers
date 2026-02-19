package org.example.voicebot;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Configurable knowledge base: array of JSON entries (category, issue, customer_query, agent_response).
 * Load from a JSON file (path or classpath) and use to build the system prompt.
 */
public class KnowledgeBase {
    private static final Gson GSON = new Gson();

    private final List<KnowledgeEntry> entries;

    public KnowledgeBase(List<KnowledgeEntry> entries) {
        this.entries = entries != null ? new ArrayList<>(entries) : new ArrayList<>();
    }

    /**
     * Load knowledge from a JSON file. Path can be absolute, relative, or a classpath resource name (e.g. "knowledge.json").
     */
    public static KnowledgeBase loadFrom(String pathOrResource) {
        List<KnowledgeEntry> entries = parseJsonToEntries(pathOrResource);
        return new KnowledgeBase(entries);
    }

    /**
     * Load from classpath resource (e.g. "knowledge.json" in src/main/resources).
     */
    public static KnowledgeBase loadFromClasspath(String resourceName) {
        try (InputStream in = KnowledgeBase.class.getResourceAsStream(resourceName.startsWith("/") ? resourceName : "/" + resourceName)) {
            if (in == null) {
                throw new IllegalArgumentException("Resource not found: " + resourceName);
            }
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                List<KnowledgeEntry> entries = GSON.fromJson(reader, new TypeToken<List<KnowledgeEntry>>() {}.getType());
                return new KnowledgeBase(entries != null ? entries : new ArrayList<>());
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load knowledge from classpath: " + resourceName, e);
        }
    }

    /**
     * Load from file path (relative to CWD or absolute).
     */
    public static KnowledgeBase loadFromFile(Path path) {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            List<KnowledgeEntry> entries = GSON.fromJson(reader, new TypeToken<List<KnowledgeEntry>>() {}.getType());
            return new KnowledgeBase(entries != null ? entries : new ArrayList<>());
        } catch (IOException e) {
            throw new RuntimeException("Failed to load knowledge from file: " + path, e);
        }
    }

    private static List<KnowledgeEntry> parseJsonToEntries(String pathOrResource) {
        Path path = Path.of(pathOrResource);
        if (Files.isRegularFile(path)) {
            return loadFromFile(path).getEntries();
        }
        return loadFromClasspath(pathOrResource).getEntries();
    }

    public List<KnowledgeEntry> getEntries() {
        return new ArrayList<>(entries);
    }

    /**
     * Groups entries by category and returns one Category per group with the list of issues.
     * Includes "Other" (with its issues) if there are uncategorized entries.
     */
    public List<Category> getCategories() {
        Map<String, List<String>> categoryToIssues = new LinkedHashMap<>();
        for (KnowledgeEntry e : entries) {
            String cat = (e.getCategory() != null && !e.getCategory().isBlank()) ? e.getCategory() : "Other";
            String issue = (e.getIssue() != null && !e.getIssue().isBlank()) ? e.getIssue() : "(no issue)";
            categoryToIssues.computeIfAbsent(cat, k -> new ArrayList<>()).add(issue);
        }
        List<Category> result = categoryToIssues.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(entry -> new Category(entry.getKey(), entry.getValue().stream().distinct().toList()))
            .toList();
        return result;
    }

    /**
     * Returns a new KnowledgeBase containing only entries for the given category.
     * Use "Other" for entries with no category. If the category is unknown or empty, returns an empty KnowledgeBase.
     */
    public KnowledgeBase forCategory(String category) {
        if (category == null || category.isBlank()) {
            return new KnowledgeBase(new ArrayList<>());
        }
        List<KnowledgeEntry> filtered = entries.stream()
            .filter(e -> category.equals("Other")
                ? (e.getCategory() == null || e.getCategory().isBlank())
                : category.equals(e.getCategory()))
            .toList();
        return new KnowledgeBase(filtered);
    }

    /**
     * Build the knowledge section for the system prompt: grouped by category, so the model can answer best.
     */
    public String toPromptSection() {
        if (entries.isEmpty()) {
            return "## Knowledge base\n(No entries loaded.)\n";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("## Knowledge base\n\n");
        sb.append("Use the following Q&A entries to answer the customer. Match their question to the closest customer_query and respond in the same style as agent_response. ");
        sb.append("If multiple entries could apply, pick the best match. If none match well, say you don't have that information and suggest support or the app.\n\n");

        var byCategory = entries.stream()
            .filter(e -> e.getCategory() != null && !e.getCategory().isBlank())
            .collect(Collectors.groupingBy(KnowledgeEntry::getCategory));

        for (String category : byCategory.keySet().stream().sorted().toList()) {
            sb.append("### ").append(category).append("\n\n");
            for (KnowledgeEntry e : byCategory.get(category)) {
                sb.append("- **Issue:** ").append(nullToEmpty(e.getIssue())).append("\n");
                sb.append("  - **Customer query:** ").append(nullToEmpty(e.getCustomerQuery())).append("\n");
                sb.append("  - **Agent response:** ").append(nullToEmpty(e.getAgentResponse())).append("\n\n");
            }
        }

        // entries with no category
        List<KnowledgeEntry> uncategorized = entries.stream()
            .filter(e -> e.getCategory() == null || e.getCategory().isBlank())
            .toList();
        if (!uncategorized.isEmpty()) {
            sb.append("### Other\n\n");
            for (KnowledgeEntry e : uncategorized) {
                sb.append("- **Issue:** ").append(nullToEmpty(e.getIssue())).append("\n");
                sb.append("  - **Customer query:** ").append(nullToEmpty(e.getCustomerQuery())).append("\n");
                sb.append("  - **Agent response:** ").append(nullToEmpty(e.getAgentResponse())).append("\n\n");
            }
        }
        return sb.toString();
    }

    private static String nullToEmpty(String s) {
        return s != null ? s : "";
    }
}
