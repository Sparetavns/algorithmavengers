package org.example.voicebot;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Catalog of context schemas loaded from JSON.
 * Each context has a name, schema (fields + descriptions), and example queries
 * so OpenAI can decide which context to use for a given customer question.
 */
public class ContextCatalog {
    private static final Gson GSON = new Gson();

    private final List<ContextSchema> contexts;

    public ContextCatalog(List<ContextSchema> contexts) {
        this.contexts = contexts != null ? new ArrayList<>(contexts) : new ArrayList<>();
    }

    public List<ContextSchema> getContexts() {
        return new ArrayList<>(contexts);
    }

    /**
     * Returns the context schema for the given name, or null if not found.
     */
    public ContextSchema getContextByName(String name) {
        if (name == null || name.isBlank()) return null;
        for (ContextSchema ctx : contexts) {
            if (name.equals(ctx.getName())) return ctx;
        }
        return null;
    }

    /**
     * Builds the catalog prompt section for a single context (name, description, schema, example queries).
     * Used when only one context's data is being sent to the model.
     */
    public String toPromptSectionForContext(String contextName) {
        ContextSchema ctx = getContextByName(contextName);
        if (ctx == null) return "";
        StringBuilder sb = new StringBuilder();
        sb.append("## Customer context (relevant to this question)\n\n");
        sb.append("### Context: ").append(ctx.getName()).append("\n");
        sb.append("- Description: ").append(ctx.getDescription() != null ? ctx.getDescription() : "").append("\n");
        if (ctx.getSchema() != null && !ctx.getSchema().isEmpty()) {
            sb.append("- Schema (fields):\n");
            for (ContextSchema.SchemaField f : ctx.getSchema()) {
                sb.append("  - ").append(f.getField()).append(": ").append(f.getDescription() != null ? f.getDescription() : "").append("\n");
            }
        }
        if (ctx.getExampleQueries() != null && !ctx.getExampleQueries().isEmpty()) {
            sb.append("- Example queries: ").append(String.join("; ", ctx.getExampleQueries())).append("\n");
        }
        sb.append("\n");
        return sb.toString();
    }

    /**
     * Load catalog from classpath resource (e.g. "context_schemas.json").
     */
    public static ContextCatalog loadFromClasspath(String resourceName) {
        String res = resourceName.startsWith("/") ? resourceName : "/" + resourceName;
        try (InputStream in = ContextCatalog.class.getResourceAsStream(res)) {
            if (in == null) {
                throw new IllegalArgumentException("Resource not found: " + resourceName);
            }
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                return parse(reader);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load context catalog from classpath: " + resourceName, e);
        }
    }

    /**
     * Load catalog from file path.
     */
    public static ContextCatalog loadFromFile(Path path) {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return parse(reader);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load context catalog from file: " + path, e);
        }
    }

    private static ContextCatalog parse(Reader reader) {
        Wrapper wrapper = GSON.fromJson(reader, Wrapper.class);
        List<ContextSchema> list = wrapper != null && wrapper.contexts != null ? wrapper.contexts : new ArrayList<>();
        return new ContextCatalog(list);
    }

    /** JSON root: { "contexts": [ ... ] } */
    private static class Wrapper {
        List<ContextSchema> contexts;
    }

    /**
     * Build the "context catalog" section for the system prompt so the model knows
     * which context (table) to use for which kind of query.
     */
    public String toPromptSection() {
        if (contexts.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("## Customer context catalog\n\n");
        sb.append("Use the following context names and schemas to decide where to find the answer. ");
        sb.append("Each context is a logical 'table' of customer data. Answer from the context that matches the customer's question.\n\n");
        for (ContextSchema ctx : contexts) {
            sb.append("### Context: ").append(ctx.getName()).append("\n");
            sb.append("- Description: ").append(ctx.getDescription() != null ? ctx.getDescription() : "").append("\n");
            if (ctx.getSchema() != null && !ctx.getSchema().isEmpty()) {
                sb.append("- Schema (fields):\n");
                for (ContextSchema.SchemaField f : ctx.getSchema()) {
                    sb.append("  - ").append(f.getField()).append(": ").append(f.getDescription() != null ? f.getDescription() : "").append("\n");
                }
            }
            if (ctx.getExampleQueries() != null && !ctx.getExampleQueries().isEmpty()) {
                sb.append("- Example queries (use this context for questions like): ");
                sb.append(String.join("; ", ctx.getExampleQueries())).append("\n");
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}
