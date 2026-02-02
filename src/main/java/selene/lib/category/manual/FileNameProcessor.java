package selene.lib.category.manual;

import lombok.AllArgsConstructor;
import lombok.Builder;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileNameProcessor {

    private static final Pattern META_DATA_PATTERN = Pattern.compile(
            "\\[([^\\]]+)\\]",
            Pattern.CASE_INSENSITIVE
    );

    public static FileMetaData getMetaData(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return FileMetaData.builder()
                    .numberOfShards(1)
                    .numberOfReplicas(1)
                    .build();
        }

        var builder = FileMetaData.builder();
        String cleanFileName = fileName;

        // Extract metadata from brackets if present
        Matcher matcher = META_DATA_PATTERN.matcher(fileName);
        if (matcher.find()) {
            String metaDataContent = matcher.group(1);
            for (String pair : metaDataContent.split(",")) {
                String trimmedPair = pair.trim();
                String[] keyValue = trimmedPair.split("=", 2);
                if (keyValue.length == 2) {
                    FileMetaData.setField(
                            builder,
                            keyValue[0].trim().toLowerCase(),
                            keyValue[1].trim());
                }
            }
            cleanFileName = matcher.replaceFirst("").trim();
        }

        // Apply smart defaults based on filename
        applyDefaults(builder, cleanFileName);

        return builder.build();
    }

    private static void applyDefaults(FileMetaData.FileMetaDataBuilder builder, String cleanFileName) {
        // Get base name without extension
        String baseName = removeExtension(cleanFileName);
        String extension = getExtension(cleanFileName);

        // Set the clean filename
        builder.fileName(cleanFileName);

        // Build partial to check what's already set
        FileMetaData partial = builder.build();

        // Re-create builder with defaults
        builder.fileName(cleanFileName);

        // displayName: use base filename if not specified
        if (partial.getDisplayName() == null || partial.getDisplayName().isBlank()) {
            builder.displayName(humanize(baseName));
        } else {
            builder.displayName(partial.getDisplayName());
        }

        // elasticIndexName: derive from filename (lowercase, sanitized)
        if (partial.getElasticIndexName() == null || partial.getElasticIndexName().isBlank()) {
            builder.elasticIndexName(toIndexName(baseName));
        } else {
            builder.elasticIndexName(partial.getElasticIndexName());
        }

        // numberOfShards: default 1
        if (partial.getNumberOfShards() == null) {
            builder.numberOfShards(1);
        } else {
            builder.numberOfShards(partial.getNumberOfShards());
        }

        // numberOfReplicas: default 1
        if (partial.getNumberOfReplicas() == null) {
            builder.numberOfReplicas(1);
        } else {
            builder.numberOfReplicas(partial.getNumberOfReplicas());
        }

        // loader: derive from extension
        if (partial.getLoader() == null || partial.getLoader().isBlank()) {
            builder.loader(extension.isEmpty() ? "csv" : extension);
        } else {
            builder.loader(partial.getLoader());
        }

        // Preserve other fields
        builder.totalRows(partial.getTotalRows());
        builder.idColumnName(partial.getIdColumnName());
        builder.caseId(partial.getCaseId());
        builder.md5(partial.getMd5());
        builder.section(partial.getSection());
    }

    private static String removeExtension(String fileName) {
        if (fileName == null) return "";
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(0, lastDot) : fileName;
    }

    private static String getExtension(String fileName) {
        if (fileName == null) return "";
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot + 1).toLowerCase() : "";
    }

    private static String toIndexName(String name) {
        if (name == null || name.isBlank()) return "index";
        return name.toLowerCase()
                .replaceAll("[^a-z0-9]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
    }

    private static String humanize(String name) {
        if (name == null || name.isBlank()) return "Unnamed";
        // Replace underscores/hyphens with spaces, capitalize words
        String spaced = name.replaceAll("[_-]", " ");
        String[] words = spaced.split("\\s+");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                if (result.length() > 0) result.append(" ");
                result.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    result.append(word.substring(1));
                }
            }
        }
        return result.toString();
    }
}
