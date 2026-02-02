package selene.lib.elastic;

import selene.lib.category.type.ClassificationResult;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility to query fields by their detected categories.
 */
public class FieldsByCategory {

    private final List<ClassificationResult> results;
    private final Map<String, List<String>> categoryToFields;

    public FieldsByCategory(List<ClassificationResult> results) {
        this.results = results;
        this.categoryToFields = buildIndex();
    }

    private Map<String, List<String>> buildIndex() {
        Map<String, List<String>> index = new HashMap<>();
        for (ClassificationResult result : results) {
            for (String category : result.getCategories()) {
                index.computeIfAbsent(category.toLowerCase(), k -> new ArrayList<>())
                     .add(result.getColumnName());
            }
        }
        return index;
    }

    /**
     * Get all field names that have a specific category.
     */
    public List<String> getFields(String category) {
        return categoryToFields.getOrDefault(category.toLowerCase(), List.of());
    }

    /**
     * Get all field names that match any of the given categories.
     */
    public List<String> getFieldsAny(String... categories) {
        Set<String> fields = new LinkedHashSet<>();
        for (String cat : categories) {
            fields.addAll(getFields(cat));
        }
        return new ArrayList<>(fields);
    }

    /**
     * Get all field names that match all of the given categories.
     */
    public List<String> getFieldsAll(String... categories) {
        if (categories.length == 0) return List.of();

        Set<String> fields = new HashSet<>(getFields(categories[0]));
        for (int i = 1; i < categories.length; i++) {
            fields.retainAll(getFields(categories[i]));
        }
        return new ArrayList<>(fields);
    }

    /**
     * Get all available categories.
     */
    public Set<String> getAllCategories() {
        return new TreeSet<>(categoryToFields.keySet());
    }

    /**
     * Get category to fields mapping.
     */
    public Map<String, List<String>> getCategoryMap() {
        return new LinkedHashMap<>(categoryToFields);
    }

    /**
     * Check if a field has a specific category.
     */
    public boolean fieldHasCategory(String fieldName, String category) {
        return getFields(category).contains(fieldName);
    }

    /**
     * Get all categories for a specific field.
     */
    public List<String> getCategoriesForField(String fieldName) {
        return results.stream()
                .filter(r -> r.getColumnName().equals(fieldName))
                .findFirst()
                .map(ClassificationResult::getCategories)
                .orElse(List.of());
    }

    /**
     * Get PII fields (common PII categories).
     */
    public List<String> getPiiFields() {
        return getFieldsAny(
            "email-address", "phone-number", "ssn", "credit-card",
            "first-name", "surname", "street-address", "ip-address",
            "date-of-birth", "driver-license", "passport", "iban"
        );
    }

    /**
     * Get all name-related fields.
     */
    public List<String> getNameFields() {
        return getFieldsAny("first-name", "surname", "name", "physician-name");
    }

    /**
     * Get all location-related fields.
     */
    public List<String> getLocationFields() {
        return getFieldsAny("city", "state", "county", "zip-code", "street-address");
    }
}
