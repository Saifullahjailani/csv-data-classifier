package selene.lib.type;

import ai.philterd.phileas.model.filtering.FilterType;
import selene.lib.category.CustomCategories;
import selene.lib.category.crypto.CryptoType;
import tech.tablesaw.api.StringColumn;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TypeDetector {
    private static final Pattern INTEGER_PATTERN = Pattern.compile("^-?\\d+$");
    private static final Pattern FLOAT_PATTERN = Pattern.compile("^-?\\d*\\.\\d+$");
    private static final Pattern BOOLEAN_PATTERN = Pattern.compile("^(true|false|yes|no|0|1)$", Pattern.CASE_INSENSITIVE);

    private static final double TYPE_CONFIDENCE_THRESHOLD = 0.8; // 80% of non-null values must match

    private static final Set<String> PII_CATEGORIES = Arrays.stream(FilterType.values()).map(FilterType::getType).map(String::toLowerCase).collect(Collectors.toSet());
    private static final Set<String> CRYPTO_CATEGORIES = Arrays.stream(CryptoType.values()).map(CryptoType::getName).map(String::toLowerCase).collect(Collectors.toSet());
    private static final Set<String> CUSTOM_CATEGORIES = Arrays.stream(CustomCategories.values()).map(CustomCategories::getType).map(String::toLowerCase).collect(Collectors.toSet());
    private static final Map<String, FilterType> PII_CATEGORY_MAP = Arrays.stream(FilterType.values()).collect(Collectors.toMap(f -> f.getType().toLowerCase(), f -> f, (a, b) -> a));



    public static ElasticTypes inferColumnType(
            List<String> categories,
            Iterable<String> column
    ){
        if (column == null || !column.iterator().hasNext()) {
            return ElasticTypes.TEXT;
        }

        if (categories != null && !categories.isEmpty()){
            ElasticTypes categoryBasedType = inferFromCategories(categories);
            if (categoryBasedType != ElasticTypes.UNKNOWN) {
                return categoryBasedType;
            }
        }

        return inferFromColumnData(column);
    }

    private static ElasticTypes inferFromCategories(List<String> categories) {
        Set<ElasticTypes> types = new HashSet<>();
        for (String category : categories) {
            ElasticTypes type = getElasticType(category);
            if(type != ElasticTypes.UNKNOWN)
                types.add(type);
        }
        if(types.isEmpty()){
            return ElasticTypes.UNKNOWN;
        }
        if (types.size() == 1) {
            return types.iterator().next();
        }
        return chooseBestType(types);
    }

    private static ElasticTypes chooseBestType(Set<ElasticTypes> types) {
        // Priority 1: Specific structured types (most specific wins)
        if (types.contains(ElasticTypes.DATE)) return ElasticTypes.DATE;
        if (types.contains(ElasticTypes.GEO_POINT)) return ElasticTypes.GEO_POINT;
        if (types.contains(ElasticTypes.BOOLEAN)) return ElasticTypes.BOOLEAN;

        // Priority 2: Numeric types (larger capacity wins)

        if (types.contains(ElasticTypes.DOUBLE)) return ElasticTypes.DOUBLE;
        if (types.contains(ElasticTypes.FLOAT)) return ElasticTypes.FLOAT;
        if (types.contains(ElasticTypes.LONG)) return ElasticTypes.LONG;
        if (types.contains(ElasticTypes.INTEGER)) return ElasticTypes.INTEGER;


        // Priority 3: String types (most flexible/searchable wins)
        if (types.contains(ElasticTypes.TEXT_WITH_KEYWORD)) return ElasticTypes.TEXT_WITH_KEYWORD;
        if (types.contains(ElasticTypes.WILDCARD)) return ElasticTypes.WILDCARD;
        if (types.contains(ElasticTypes.TEXT)) return ElasticTypes.TEXT;
        if (types.contains(ElasticTypes.KEYWORD)) return ElasticTypes.KEYWORD;

        // Priority 4: Fallback
        return ElasticTypes.UNKNOWN;
    }

    private static ElasticTypes getElasticType(String category){
        category = category.trim().toLowerCase();
        if(CRYPTO_CATEGORIES.contains(category)){
            return ElasticTypes.KEYWORD;
        }
        if(CUSTOM_CATEGORIES.contains(category)){
            for (CustomCategories cat : CustomCategories.values()) {
                if (cat.getType().equalsIgnoreCase(category)) {
                    return cat.getElasticTypes();
                }
            }
        }
        if(PII_CATEGORIES.contains(category)){
            FilterType filterType = PII_CATEGORY_MAP.get(category);
            if (filterType != null) {
                return getElasticType(filterType);
            }
        }
        return ElasticTypes.UNKNOWN;
    }


    private static ElasticTypes getElasticType(FilterType type) {
        return switch (type) {
            case FilterType.BANK_ROUTING_NUMBER -> ElasticTypes.INTEGER;

            case FilterType.DATE -> ElasticTypes.DATE;

            case IP_ADDRESS -> ElasticTypes.IP;

            case FilterType.BITCOIN_ADDRESS,
                 FilterType.SSN,
                 FilterType.ZIP_CODE,
                 FilterType.PHONE_NUMBER,
                 FilterType.PHONE_NUMBER_EXTENSION,
                 FilterType.CURRENCY,
                 FilterType.PASSPORT_NUMBER,
                 FilterType.VIN,
                 FilterType.TRACKING_NUMBER,
                 FilterType.CREDIT_CARD,
                 FilterType.EMAIL_ADDRESS,
                 FilterType.IBAN_CODE,
                 FilterType.MAC_ADDRESS,
                 FilterType.FIRST_NAME,
                 FilterType.SURNAME,
                 FilterType.URL -> ElasticTypes.KEYWORD;

            case FilterType.AGE,
                 FilterType.LOCATION_CITY,
                 FilterType.LOCATION_STATE,
                 FilterType.LOCATION_COUNTY,
                 FilterType.HOSPITAL,
                 FilterType.HOSPITAL_ABBREVIATION,
                 FilterType.PERSON,
                 FilterType.PHYSICIAN_NAME,

                 FilterType.STATE_ABBREVIATION -> ElasticTypes.TEXT;

            case FilterType.STREET_ADDRESS -> ElasticTypes.GEO_POINT;

            case FilterType.SECTION -> ElasticTypes.KEYWORD;


            default -> ElasticTypes.UNKNOWN;
        };
    }


    private static ElasticTypes inferFromColumnData(Iterable<String> column){
        Set<ElasticTypes> types = new HashSet<>();

        int size = 0;
        for (String value : column) {
            String trimmed = value.trim();
            size++;
            // Check in order of specificity
            if (BOOLEAN_PATTERN.matcher(trimmed).matches()) {
                types.add(ElasticTypes.BOOLEAN);
            } else if (INTEGER_PATTERN.matcher(trimmed).matches()) {
                types.add(ElasticTypes.INTEGER);
            } else if (FLOAT_PATTERN.matcher(trimmed).matches()) {
                types.add(ElasticTypes.FLOAT);
            }
        }

       ElasticTypes myType = chooseBestType(types);
        if(myType == ElasticTypes.UNKNOWN){
            return ElasticTypes.TEXT_WITH_KEYWORD;
        }
        return myType;
    }

}
