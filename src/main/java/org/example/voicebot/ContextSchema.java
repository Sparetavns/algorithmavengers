package org.example.voicebot;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Defines one customer context "table": name, description, schema (fields), and example queries
 * so that the model knows which context to use for a given customer question.
 */
public class ContextSchema {
    private String name;
    private String description;
    private List<SchemaField> schema;
    @SerializedName("example_queries")
    private List<String> exampleQueries;

    public ContextSchema() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<SchemaField> getSchema() { return schema; }
    public void setSchema(List<SchemaField> schema) { this.schema = schema; }
    public List<String> getExampleQueries() { return exampleQueries; }
    public void setExampleQueries(List<String> exampleQueries) { this.exampleQueries = exampleQueries; }

    public static class SchemaField {
        private String field;
        private String description;

        public SchemaField() {}
        public SchemaField(String field, String description) {
            this.field = field;
            this.description = description;
        }
        public String getField() { return field; }
        public void setField(String field) { this.field = field; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
}
