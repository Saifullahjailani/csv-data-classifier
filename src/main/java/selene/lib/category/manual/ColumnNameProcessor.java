package selene.lib.category.manual;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import selene.lib.type.ElasticTypes;
import selene.lib.category.Util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColumnNameProcessor {

    //  type[]cat[]columnname -> ColumnMetaData(columnName,Unknown, null)
    // type[Integer]cat[]

        // Pattern to match type[value] and cat[value] in any order
        private static final Pattern TYPE_PATTERN = Pattern.compile(
                "type\\[([^\\]]+)\\]",
                Pattern.CASE_INSENSITIVE
        );

        private static final Pattern CAT_PATTERN = Pattern.compile(
                "cat\\[([^\\]]*)\\]",
                Pattern.CASE_INSENSITIVE
        );


    public ColumnMetaData getMetaData(String columnName) {
        if (columnName == null || columnName.trim().isEmpty()) {
            return new ColumnMetaData("", ElasticTypes.UNKNOWN, new ArrayList<>());
        }

        columnName = columnName.trim();

        // Extract type[...] if present
        String typeValue = null;
        Matcher typeMatcher = TYPE_PATTERN.matcher(columnName);
        if (typeMatcher.find()) {
            typeValue = typeMatcher.group(1);
        }

        // Extract cat[...] if present
        String catValue = null;
        Matcher catMatcher = CAT_PATTERN.matcher(columnName);
        if (catMatcher.find()) {
            catValue = catMatcher.group(1);
        }

        // Remove type[...] and cat[...] to get actual column name
        String actualColumnName = columnName;
        actualColumnName = TYPE_PATTERN.matcher(actualColumnName).replaceAll("");
        actualColumnName = CAT_PATTERN.matcher(actualColumnName).replaceAll("");
        actualColumnName = actualColumnName.trim();

        // Clean up actual column name
        if (actualColumnName.isEmpty()) {
            actualColumnName = "unnamed";
        }

        // Parse ElastiTypes
        ElasticTypes elasticType = parseElasticType(typeValue);

        // Parse categories
        List<String> categories = parseCategories(catValue);

        return new ColumnMetaData(actualColumnName, elasticType, categories);
    }

    private ElasticTypes parseElasticType(String typeValue){
        try {
            return ElasticTypes.valueOf(typeValue);
        } catch (Exception _){
            return ElasticTypes.UNKNOWN;
        }
    }

    private List<String> parseCategories(String catValue) {
        if (catValue == null || catValue.trim().isEmpty()) {
            return new ArrayList<>();
        }
        List<String> categories = new ArrayList<>();
        String[] parts = catValue.split(",");
        Set<String> allCategories = new HashSet<>(Util.getAllValidCategories());
        for(var cat : parts){
            cat = cat.trim().toLowerCase();
            if(allCategories.contains(cat)){
                categories.add(cat);
            }
        }
        return categories;

    }


    @AllArgsConstructor
    @Getter
    @Setter
    @Builder
    public static class ColumnMetaData{
        private  String columnName;
        private ElasticTypes elasticType;
        private List<String> category;


        public void addCategory(String category){
            this.category.add(category);
        }

        @Override
        public String toString() {

            return "type[" +
                    elasticType.name() +
                    "]" +
                    "cat[" +
                    getCats() +
                    "]" +
                    columnName;
        }

        private String getCats(){
            if(category == null || category.isEmpty()){
                return "";
            }
            return String.join(",",category);
        }
    }


}


