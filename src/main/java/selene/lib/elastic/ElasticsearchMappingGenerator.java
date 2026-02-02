package selene.lib.elastic;

import selene.lib.category.type.ClassificationResult;
import selene.lib.type.ElasticTypes;

import java.util.*;

/**
 * Generates Elasticsearch index mappings with appropriate analyzers
 * based on detected column categories and types.
 */
public class ElasticsearchMappingGenerator {

    private final String indexName;
    private final List<ClassificationResult> columns;
    private final MappingOptions options;

    public ElasticsearchMappingGenerator(String indexName, List<ClassificationResult> columns) {
        this(indexName, columns, MappingOptions.defaults());
    }

    public ElasticsearchMappingGenerator(String indexName, List<ClassificationResult> columns, MappingOptions options) {
        this.indexName = indexName;
        this.columns = columns;
        this.options = options;
    }

    /**
     * Generates the complete Elasticsearch index mapping as a Map structure
     * that can be serialized to JSON.
     */
    public Map<String, Object> generateMapping() {
        Map<String, Object> root = new LinkedHashMap<>();

        // Index settings with analyzers
        root.put("settings", generateSettings());

        // Mappings
        Map<String, Object> mappings = new LinkedHashMap<>();
        mappings.put("dynamic", options.dynamicMapping ? "true" : "strict");
        mappings.put("properties", generateProperties());
        root.put("mappings", mappings);

        return root;
    }

    /**
     * Generates index settings including custom analyzers.
     */
    private Map<String, Object> generateSettings() {
        Map<String, Object> settings = new LinkedHashMap<>();

        // Basic settings
        settings.put("number_of_shards", options.numberOfShards);
        settings.put("number_of_replicas", options.numberOfReplicas);

        // Analysis settings with custom analyzers
        Map<String, Object> analysis = new LinkedHashMap<>();

        // Custom analyzers
        Map<String, Object> analyzers = new LinkedHashMap<>();

        // Email analyzer - lowercase and split on @ and .
        analyzers.put("email_analyzer", Map.of(
            "type", "custom",
            "tokenizer", "uax_url_email",
            "filter", List.of("lowercase", "email_domain_filter")
        ));

        // Name analyzer - handles names with proper case handling
        analyzers.put("name_analyzer", Map.of(
            "type", "custom",
            "tokenizer", "standard",
            "filter", List.of("lowercase", "asciifolding")
        ));

        // Phone analyzer - removes non-digits for phone number matching
        analyzers.put("phone_analyzer", Map.of(
            "type", "custom",
            "tokenizer", "keyword",
            "filter", List.of("phone_filter")
        ));

        // Address analyzer - handles street addresses
        analyzers.put("address_analyzer", Map.of(
            "type", "custom",
            "tokenizer", "standard",
            "filter", List.of("lowercase", "asciifolding", "address_synonym_filter")
        ));

        // Autocomplete analyzer for search-as-you-type
        analyzers.put("autocomplete_analyzer", Map.of(
            "type", "custom",
            "tokenizer", "autocomplete_tokenizer",
            "filter", List.of("lowercase")
        ));

        analyzers.put("autocomplete_search_analyzer", Map.of(
            "type", "custom",
            "tokenizer", "standard",
            "filter", List.of("lowercase")
        ));

        // PII masking analyzer (for sensitive data that needs partial matching)
        analyzers.put("pii_analyzer", Map.of(
            "type", "custom",
            "tokenizer", "keyword",
            "filter", List.of("lowercase")
        ));

        analysis.put("analyzer", analyzers);

        // Custom tokenizers
        Map<String, Object> tokenizers = new LinkedHashMap<>();
        tokenizers.put("autocomplete_tokenizer", Map.of(
            "type", "edge_ngram",
            "min_gram", 2,
            "max_gram", 20,
            "token_chars", List.of("letter", "digit")
        ));
        analysis.put("tokenizer", tokenizers);

        // Custom filters
        Map<String, Object> filters = new LinkedHashMap<>();

        // Email domain filter - extracts domain from email
        filters.put("email_domain_filter", Map.of(
            "type", "pattern_capture",
            "preserve_original", true,
            "patterns", List.of("@(.+)")
        ));

        // Phone filter - keeps only digits
        filters.put("phone_filter", Map.of(
            "type", "pattern_replace",
            "pattern", "[^0-9]",
            "replacement", ""
        ));

        // Address synonym filter for common abbreviations
        filters.put("address_synonym_filter", Map.of(
            "type", "synonym",
            "synonyms", List.of(
                "st, street",
                "ave, avenue",
                "blvd, boulevard",
                "dr, drive",
                "ln, lane",
                "rd, road",
                "ct, court",
                "pl, place",
                "apt, apartment",
                "ste, suite"
            )
        ));

        analysis.put("filter", filters);

        // Custom normalizers
        Map<String, Object> normalizers = new LinkedHashMap<>();
        normalizers.put("lowercase", Map.of(
            "type", "custom",
            "filter", List.of("lowercase")
        ));
        analysis.put("normalizer", normalizers);

        settings.put("analysis", analysis);

        return settings;
    }

    /**
     * Generates field mappings for all columns.
     */
    private Map<String, Object> generateProperties() {
        Map<String, Object> properties = new LinkedHashMap<>();

        for (ClassificationResult column : columns) {
            String fieldName = sanitizeFieldName(column.getColumnName());
            properties.put(fieldName, generateFieldMapping(column));
        }

        return properties;
    }

    /**
     * Generates mapping for a single field based on its type and categories.
     */
    private Map<String, Object> generateFieldMapping(ClassificationResult column) {
        Map<String, Object> mapping = new LinkedHashMap<>();
        ElasticTypes type = column.getType();
        List<String> categories = column.getCategories() != null ? column.getCategories() : List.of();

        // Determine the best mapping based on type and categories
        switch (type) {
            case TEXT:
                mapping = generateTextMapping(column, categories);
                break;
            case KEYWORD:
                mapping = generateKeywordMapping(column, categories);
                break;
            case TEXT_WITH_KEYWORD:
                mapping = generateTextWithKeywordMapping(column, categories);
                break;
            case INTEGER:
                mapping.put("type", "integer");
                break;
            case LONG:
                mapping.put("type", "long");
                break;
            case FLOAT:
                mapping.put("type", "float");
                break;
            case DOUBLE:
                mapping.put("type", "double");
                break;
            case BOOLEAN:
                mapping.put("type", "boolean");
                break;
            case DATE:
                mapping = generateDateMapping(categories);
                break;
            case IP:
                mapping.put("type", "ip");
                break;
            case GEO_POINT:
                mapping.put("type", "geo_point");
                break;
            case BINARY:
                mapping.put("type", "binary");
                break;
            case OBJECT:
                mapping.put("type", "object");
                mapping.put("dynamic", true);
                break;
            case NESTED:
                mapping.put("type", "nested");
                break;
            case WILDCARD:
                mapping.put("type", "wildcard");
                break;
            default:
                // Default to keyword for unknown types
                mapping.put("type", "keyword");
                mapping.put("ignore_above", 256);
                break;
        }

        // Add metadata about detected categories as meta field
        if (options.includeMetadata && !categories.isEmpty()) {
            mapping.put("meta", Map.of("detected_categories", categories));
        }

        return mapping;
    }

    /**
     * Generates text field mapping with appropriate analyzer.
     */
    private Map<String, Object> generateTextMapping(ClassificationResult column, List<String> categories) {
        Map<String, Object> mapping = new LinkedHashMap<>();
        mapping.put("type", "text");

        // Choose analyzer based on categories
        String analyzer = determineAnalyzer(categories);
        if (analyzer != null) {
            mapping.put("analyzer", analyzer);
        }

        // Add keyword sub-field for aggregations
        if (options.addKeywordSubfield) {
            Map<String, Object> fields = new LinkedHashMap<>();
            fields.put("keyword", Map.of(
                "type", "keyword",
                "ignore_above", 256
            ));

            // Add autocomplete sub-field for name fields
            if (isNameField(categories)) {
                fields.put("autocomplete", Map.of(
                    "type", "text",
                    "analyzer", "autocomplete_analyzer",
                    "search_analyzer", "autocomplete_search_analyzer"
                ));
            }

            mapping.put("fields", fields);
        }

        return mapping;
    }

    /**
     * Generates keyword field mapping.
     */
    private Map<String, Object> generateKeywordMapping(ClassificationResult column, List<String> categories) {
        Map<String, Object> mapping = new LinkedHashMap<>();
        mapping.put("type", "keyword");

        // Set appropriate ignore_above based on category
        if (containsAny(categories, "hash", "md5", "sha256", "sha512")) {
            mapping.put("ignore_above", 512);
        } else if (containsAny(categories, "email", "email-address")) {
            mapping.put("ignore_above", 320); // Max email length
            // Add normalizer for case-insensitive matching
            mapping.put("normalizer", "lowercase");
        } else if (containsAny(categories, "bitcoin", "ethereum", "crypto")) {
            mapping.put("ignore_above", 128);
        } else {
            mapping.put("ignore_above", 256);
        }

        return mapping;
    }

    /**
     * Generates text field with keyword sub-field.
     */
    private Map<String, Object> generateTextWithKeywordMapping(ClassificationResult column, List<String> categories) {
        Map<String, Object> mapping = new LinkedHashMap<>();
        mapping.put("type", "text");

        String analyzer = determineAnalyzer(categories);
        if (analyzer != null) {
            mapping.put("analyzer", analyzer);
        }

        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("keyword", Map.of(
            "type", "keyword",
            "ignore_above", 256
        ));
        fields.put("raw", Map.of(
            "type", "keyword",
            "ignore_above", 256
        ));
        mapping.put("fields", fields);

        return mapping;
    }

    /**
     * Generates date field mapping with multiple format support.
     */
    private Map<String, Object> generateDateMapping(List<String> categories) {
        Map<String, Object> mapping = new LinkedHashMap<>();
        mapping.put("type", "date");

        // Support multiple date formats
        mapping.put("format", String.join("||",
            "strict_date_optional_time",
            "yyyy-MM-dd",
            "yyyy/MM/dd",
            "MM-dd-yyyy",
            "MM/dd/yyyy",
            "dd-MM-yyyy",
            "dd/MM/yyyy",
            "yyyy-MM-dd HH:mm:ss",
            "epoch_millis",
            "epoch_second"
        ));

        // Handle parse failures gracefully
        mapping.put("ignore_malformed", true);

        return mapping;
    }

    /**
     * Determines the appropriate analyzer based on detected categories.
     */
    private String determineAnalyzer(List<String> categories) {
        if (containsAny(categories, "email", "email-address")) {
            return "email_analyzer";
        }
        if (containsAny(categories, "first-name", "surname", "name", "physician-name")) {
            return "name_analyzer";
        }
        if (containsAny(categories, "phone-number", "phone-number-extension")) {
            return "phone_analyzer";
        }
        if (containsAny(categories, "street-address", "address", "city", "state", "county")) {
            return "address_analyzer";
        }
        if (containsAny(categories, "ssn", "credit-card", "bank-routing-number", "iban")) {
            return "pii_analyzer";
        }
        return null; // Use default analyzer
    }

    /**
     * Checks if field is a name field that would benefit from autocomplete.
     */
    private boolean isNameField(List<String> categories) {
        return containsAny(categories, "first-name", "surname", "name", "physician-name", "job-title");
    }

    /**
     * Utility method to check if categories contain any of the specified values.
     */
    private boolean containsAny(List<String> categories, String... values) {
        for (String category : categories) {
            String lowerCategory = category.toLowerCase();
            for (String value : values) {
                if (lowerCategory.contains(value.toLowerCase())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Sanitizes field name to be valid Elasticsearch field name.
     */
    private String sanitizeFieldName(String name) {
        if (name == null) return "unknown_field";

        // Remove or replace invalid characters
        String sanitized = name
            .replaceAll("[\\[\\]\\{\\}\\(\\)]", "")  // Remove brackets
            .replaceAll("[^a-zA-Z0-9_]", "_")        // Replace special chars with underscore
            .replaceAll("_+", "_")                    // Collapse multiple underscores
            .replaceAll("^_|_$", "");                 // Remove leading/trailing underscores

        // Ensure it doesn't start with a number
        if (sanitized.isEmpty() || Character.isDigit(sanitized.charAt(0))) {
            sanitized = "field_" + sanitized;
        }

        return sanitized.toLowerCase();
    }

    /**
     * Configuration options for mapping generation.
     */
    public static class MappingOptions {
        public int numberOfShards = 1;
        public int numberOfReplicas = 1;
        public boolean dynamicMapping = false;
        public boolean addKeywordSubfield = true;
        public boolean includeMetadata = true;

        public static MappingOptions defaults() {
            return new MappingOptions();
        }

        public MappingOptions withShards(int shards) {
            this.numberOfShards = shards;
            return this;
        }

        public MappingOptions withReplicas(int replicas) {
            this.numberOfReplicas = replicas;
            return this;
        }

        public MappingOptions withDynamicMapping(boolean dynamic) {
            this.dynamicMapping = dynamic;
            return this;
        }

        public MappingOptions withKeywordSubfield(boolean add) {
            this.addKeywordSubfield = add;
            return this;
        }

        public MappingOptions withMetadata(boolean include) {
            this.includeMetadata = include;
            return this;
        }
    }
}
