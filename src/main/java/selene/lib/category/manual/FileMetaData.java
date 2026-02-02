package selene.lib.category.manual;

import lombok.*;

@AllArgsConstructor
@Builder
@Getter
@Setter
public class FileMetaData {
    private final String loader;
    private final String totalRows;
    private final String idColumnName;
    private final String elasticIndexName;
    private final Integer numberOfShards;
    private final Integer numberOfReplicas;
    private final String caseId;
    private final String md5;
    private final String displayName;
    private final String section;
    private final String fileName;

    public static void setField(FileMetaDataBuilder builder, String key, String value) {
        switch (key) {
            case "loader" -> builder.loader(value);
            case "total_rows" -> builder.totalRows(value);
            case "id_column_name" -> builder.idColumnName(value);
            case "elastic_index_name" -> builder.elasticIndexName(value);
            case "elastic_shards" -> builder.numberOfShards(parseInteger(value));
            case "elastic_replicas" -> builder.numberOfReplicas(parseInteger(value));
            case "case_id" -> builder.caseId(value);
            case "md5" -> builder.md5(value);
            case "display_name" -> builder.displayName(value);
            case "display_section" -> builder.section(value);
        }

    }
    private static Integer parseInteger(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
