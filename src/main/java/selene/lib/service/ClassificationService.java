package selene.lib.service;

import selene.lib.category.CSVCategorizer;
import selene.lib.category.manual.FileMetaData;
import selene.lib.category.manual.FileNameProcessor;
import selene.lib.category.type.ClassificationResult;
import selene.lib.elastic.ElasticsearchMappingGenerator;
import selene.lib.elastic.FieldsByCategory;

import java.nio.file.Path;
import java.util.*;

/**
 * Service class that orchestrates CSV classification and ES mapping generation.
 */
public class ClassificationService {

    private final String filePath;
    private FileMetaData fileMetaData;
    private List<ClassificationResult> classificationResults;
    private FieldsByCategory fieldsByCategory;
    private Map<String, Object> elasticsearchMapping;

    public ClassificationService(String filePath) {
        this.filePath = filePath;
    }

    /**
     * Runs the full classification pipeline.
     */
    public ClassificationService classify() {
        // Extract file metadata from filename
        String fileName = Path.of(filePath).getFileName().toString();
        this.fileMetaData = FileNameProcessor.getMetaData(fileName);

        // Categorize CSV columns
        CSVCategorizer categorizer = new CSVCategorizer(filePath);
        categorizer.categorize();
        this.classificationResults = categorizer.getClassificationResults();

        // Build category index
        this.fieldsByCategory = new FieldsByCategory(classificationResults);

        // Generate ES mapping
        ElasticsearchMappingGenerator.MappingOptions options = ElasticsearchMappingGenerator.MappingOptions.defaults()
                .withShards(fileMetaData.getNumberOfShards())
                .withReplicas(fileMetaData.getNumberOfReplicas())
                .withMetadata(false);

        ElasticsearchMappingGenerator generator = new ElasticsearchMappingGenerator(
                fileMetaData.getElasticIndexName(),
                classificationResults,
                options
        );
        this.elasticsearchMapping = generator.generateMapping();

        return this;
    }

    /**
     * Generates full JSON output with file metadata, columns, categories, and ES mapping.
     */
    public Map<String, Object> toJsonOutput() {
        Map<String, Object> output = new LinkedHashMap<>();

        // File metadata
        Map<String, Object> file = new LinkedHashMap<>();
        file.put("fileName", fileMetaData.getFileName());
        file.put("displayName", fileMetaData.getDisplayName());
        file.put("loader", fileMetaData.getLoader());
        if (fileMetaData.getTotalRows() != null) file.put("totalRows", fileMetaData.getTotalRows());
        if (fileMetaData.getIdColumnName() != null) file.put("idColumnName", fileMetaData.getIdColumnName());
        if (fileMetaData.getCaseId() != null) file.put("caseId", fileMetaData.getCaseId());
        if (fileMetaData.getMd5() != null) file.put("md5", fileMetaData.getMd5());
        if (fileMetaData.getSection() != null) file.put("section", fileMetaData.getSection());
        output.put("file", file);

        // Elasticsearch column names (sanitized)
        List<String> esColumns = classificationResults.stream()
                .map(r -> ElasticsearchMappingGenerator.sanitizeFieldName(r.getColumnName()))
                .toList();
        output.put("columns", esColumns);

        // Categories object: category -> list of ES field names
        Map<String, List<String>> categories = new LinkedHashMap<>();
        for (String category : fieldsByCategory.getAllCategories()) {
            List<String> fields = fieldsByCategory.getFields(category).stream()
                    .map(ElasticsearchMappingGenerator::sanitizeFieldName)
                    .toList();
            categories.put(category, fields);
        }
        output.put("categories", categories);

        // Elasticsearch config
        output.put("elasticsearch", Map.of(
                "indexName", fileMetaData.getElasticIndexName(),
                "settings", elasticsearchMapping.get("settings"),
                "mappings", elasticsearchMapping.get("mappings")
        ));

        return output;
    }

    /**
     * Generates annotated CSV header output.
     */
    public String toAnnotatedCsv() {
        StringBuilder sb = new StringBuilder();

        // Annotated filename
        String annotatedFileName = String.format(
                "[loader=%s,elastic_index_name=%s,elastic_shards=%d,elastic_replicas=%d,display_name=%s]%s",
                fileMetaData.getLoader(),
                fileMetaData.getElasticIndexName(),
                fileMetaData.getNumberOfShards(),
                fileMetaData.getNumberOfReplicas(),
                fileMetaData.getDisplayName(),
                fileMetaData.getFileName()
        );
        sb.append("# Annotated filename: ").append(annotatedFileName).append("\n");

        // Annotated column headers
        List<String> annotatedColumns = new ArrayList<>();
        for (ClassificationResult result : classificationResults) {
            String colName = result.getColumnName();
            String type = result.getType().name();
            String cats = String.join(",", result.getCategories());
            annotatedColumns.add(String.format("type[%s]cat[%s]%s", type, cats, colName));
        }
        sb.append(String.join(",", annotatedColumns));

        return sb.toString();
    }

    // Getters
    public FileMetaData getFileMetaData() { return fileMetaData; }
    public List<ClassificationResult> getClassificationResults() { return classificationResults; }
    public FieldsByCategory getFieldsByCategory() { return fieldsByCategory; }
    public Map<String, Object> getElasticsearchMapping() { return elasticsearchMapping; }
}
