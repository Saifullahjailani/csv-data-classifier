package selene.lib;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import selene.lib.category.CSVCategorizer;
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

        CSVCategorizer categorizer = new CSVCategorizer(options.inputFile);
        categorizer.categorize();
        List<ClassificationResult> results = categorizer.getClassificationResults();

        return switch (options.format) {
            case "es-mapping", "mapping" -> generateEsMapping(results, options);
            default -> generateJson(results, categorizer);
        };
    }

    private static String generateJson(List<ClassificationResult> results, CSVCategorizer categorizer) {
        try {
            Map<String, Object> output = new LinkedHashMap<>();
            output.put("rowCount", categorizer.getRowCount());
            output.put("columnCount", categorizer.getColumnCount());

            List<Map<String, Object>> columns = new ArrayList<>();
            for (ClassificationResult result : results) {
                Map<String, Object> col = new LinkedHashMap<>();
                col.put("name", result.getColumnName());
                col.put("type", result.getType().name());
                col.put("categories", result.getCategories());
                columns.add(col);
            }
            output.put("columns", columns);

            return mapper.writeValueAsString(output);
        } catch (Exception e) {
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    private static String generateEsMapping(List<ClassificationResult> results, CliOptions options) {
        try {
            ElasticsearchMappingGenerator.MappingOptions mappingOptions =
                ElasticsearchMappingGenerator.MappingOptions.defaults()
                    .withShards(options.shards)
                    .withReplicas(options.replicas);

            ElasticsearchMappingGenerator generator = new ElasticsearchMappingGenerator(
                options.indexName,
                results,
                mappingOptions
            );

            return mapper.writeValueAsString(generator.generateMapping());
        } catch (Exception e) {
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
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
                case "-i", "--index" -> options.indexName = args[++i];
                case "--shards" -> options.shards = Integer.parseInt(args[++i]);
                case "--replicas" -> options.replicas = Integer.parseInt(args[++i]);
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

        // Derive index name from filename if not specified
        if (options.indexName == null) {
            String fileName = Path.of(options.inputFile).getFileName().toString();
            options.indexName = fileName.replaceAll("\\.csv$", "").toLowerCase().replaceAll("[^a-z0-9]", "_");
        }

        return options;
    }

    private static void printUsage() {
        System.out.println("""
            CSV Data Classifier - Analyze CSV columns and generate Elasticsearch mappings

            USAGE:
                csv-classifier <file.csv> [options]

            OPTIONS:
                -f, --format <json|es-mapping>   Output format (default: json)
                -o, --output <file>              Write to file instead of stdout
                -i, --index <name>               Elasticsearch index name
                --shards <n>                     Number of shards (default: 1)
                --replicas <n>                   Number of replicas (default: 1)
                -h, --help                       Show this help
                -v, --version                    Show version

            EXAMPLES:
                csv-classifier data.csv                     # Analyze and output JSON
                csv-classifier data.csv -f es-mapping       # Generate ES mapping
                csv-classifier data.csv -o result.json      # Save to file
                csv-classifier data.csv -f es-mapping -i my_index --shards 3

            COLUMN ANNOTATIONS:
                Column names can include type and category hints:
                  type[INTEGER]cat[index,id]MyColumn

                Supported types: TEXT, KEYWORD, INTEGER, LONG, FLOAT, DOUBLE,
                                 BOOLEAN, DATE, IP, GEO_POINT

                Supported categories: email-address, phone-number, ssn, credit-card,
                                      first-name, surname, city, state, zip-code,
                                      gender, job-title, index, id, hash, etc.
            """);
    }

    private static class CliOptions {
        String inputFile;
        String outputFile;
        String format = "json";
        String indexName;
        int shards = 1;
        int replicas = 1;
    }
}
