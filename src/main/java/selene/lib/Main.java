package selene.lib;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import selene.lib.category.CSVCategorizer;
import selene.lib.category.manual.ColumnNameProcessor;
import selene.lib.category.manual.FileMetaData;
import selene.lib.category.manual.FileNameProcessor;
import selene.lib.category.type.ClassificationResult;
import selene.lib.elastic.ElasticsearchMappingGenerator;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class Main {

    private static final String VERSION = "1.0.0";
    private static final ObjectMapper mapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    public static void main(String[] args) {
        if (args.length == 0 || args[0].equals("-h") || args[0].equals("--help")) {
            printUsage();
            System.exit(args.length == 0 ? 1 : 0);
            return;
        }

        if (args[0].equals("-v") || args[0].equals("--version")) {
            System.out.println("csv-classifier " + VERSION);
            return;
        }

        try {
            CliOptions options = parseArguments(args);
            String output = processFile(options);
            writeOutput(output, options.outputFile);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }

    private static String processFile(CliOptions options) throws IOException {
        Path inputPath = Path.of(options.inputFile);
        if (!Files.exists(inputPath)) {
            throw new IOException("File not found: " + options.inputFile);
        }

        // Get file metadata from filename
        String fileName = inputPath.getFileName().toString();
        FileMetaData fileMeta = FileNameProcessor.getMetaData(fileName);

        // Process CSV
        CSVCategorizer categorizer = new CSVCategorizer(options.inputFile);
        categorizer.categorize();
        List<ClassificationResult> results = categorizer.getClassificationResults();

        return switch (options.format) {
            case "csv" -> generateAnnotatedCsv(results, fileMeta, categorizer);
            default -> generateJson(results, categorizer, fileMeta);
        };
    }

    private static String generateJson(List<ClassificationResult> results, CSVCategorizer categorizer, FileMetaData fileMeta) {
        try {
            Map<String, Object> output = new LinkedHashMap<>();

            // File metadata
            Map<String, Object> file = new LinkedHashMap<>();
            file.put("fileName", fileMeta.getFileName());
            file.put("displayName", fileMeta.getDisplayName());
            file.put("loader", fileMeta.getLoader());
            if (fileMeta.getTotalRows() != null) file.put("totalRows", fileMeta.getTotalRows());
            if (fileMeta.getIdColumnName() != null) file.put("idColumnName", fileMeta.getIdColumnName());
            if (fileMeta.getCaseId() != null) file.put("caseId", fileMeta.getCaseId());
            if (fileMeta.getMd5() != null) file.put("md5", fileMeta.getMd5());
            if (fileMeta.getSection() != null) file.put("section", fileMeta.getSection());
            output.put("file", file);

            // Column names list
            List<String> columnNames = results.stream().map(ClassificationResult::getColumnName).toList();
            output.put("columns", columnNames);

            // ES settings and mappings (without category metadata)
            ElasticsearchMappingGenerator.MappingOptions mappingOptions =
                ElasticsearchMappingGenerator.MappingOptions.defaults()
                    .withShards(fileMeta.getNumberOfShards())
                    .withReplicas(fileMeta.getNumberOfReplicas())
                    .withMetadata(false);  // No category metadata in mapping

            ElasticsearchMappingGenerator generator = new ElasticsearchMappingGenerator(
                fileMeta.getElasticIndexName(),
                results,
                mappingOptions
            );

            Map<String, Object> esMapping = generator.generateMapping();
            output.put("elasticsearch", Map.of(
                "indexName", fileMeta.getElasticIndexName(),
                "settings", esMapping.get("settings"),
                "mappings", esMapping.get("mappings")
            ));

            return mapper.writeValueAsString(output);
        } catch (Exception e) {
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    private static String generateAnnotatedCsv(List<ClassificationResult> results, FileMetaData fileMeta, CSVCategorizer categorizer) {
        StringBuilder sb = new StringBuilder();

        // Generate annotated filename
        String annotatedFileName = String.format(
            "[loader=%s,elastic_index_name=%s,elastic_shards=%d,elastic_replicas=%d,display_name=%s]%s",
            fileMeta.getLoader(),
            fileMeta.getElasticIndexName(),
            fileMeta.getNumberOfShards(),
            fileMeta.getNumberOfReplicas(),
            fileMeta.getDisplayName(),
            fileMeta.getFileName()
        );
        sb.append("# Annotated filename: ").append(annotatedFileName).append("\n");

        // Generate annotated column headers
        List<String> annotatedColumns = new ArrayList<>();
        for (ClassificationResult result : results) {
            String colName = result.getColumnName();
            String type = result.getType().name();
            String cats = String.join(",", result.getCategories());
            String annotated = String.format("type[%s]cat[%s]%s", type, cats, colName);
            annotatedColumns.add(annotated);
        }
        sb.append(String.join(",", annotatedColumns));

        return sb.toString();
    }

    private static void writeOutput(String output, String outputFile) throws IOException {
        if (outputFile != null) {
            try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile))) {
                writer.println(output);
            }
        } else {
            System.out.println(output);
        }
    }

    private static CliOptions parseArguments(String[] args) {
        CliOptions options = new CliOptions();

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "-f", "--format" -> options.format = args[++i];
                case "-o", "--output" -> options.outputFile = args[++i];
                default -> {
                    if (!arg.startsWith("-")) {
                        options.inputFile = arg;
                    }
                }
            }
        }

        if (options.inputFile == null) {
            throw new IllegalArgumentException("No input file specified");
        }

        return options;
    }

    private static void printUsage() {
        System.out.println("""
            CSV Data Classifier - Analyze CSV and generate Elasticsearch mappings

            USAGE:
                csv-classifier <file.csv> [options]

            OPTIONS:
                -f, --format <json|csv>   Output format (default: json)
                -o, --output <file>       Write to file instead of stdout
                -h, --help                Show this help
                -v, --version             Show version

            OUTPUT FORMATS:
                json  - Complete JSON with file metadata, columns, and ES mapping
                csv   - Annotated column headers with type/category metadata

            EXAMPLES:
                csv-classifier data.csv                  # Full JSON output
                csv-classifier data.csv -o result.json   # Save to file
                csv-classifier data.csv -f csv           # Annotated CSV headers

            FILE METADATA (in filename):
                [loader=csv,elastic_shards=3,display_name=My Data]data.csv

            COLUMN ANNOTATIONS (in column names):
                type[INTEGER]cat[index,id]MyColumn
            """);
    }

    private static class CliOptions {
        String inputFile;
        String outputFile;
        String format = "json";
    }
}
