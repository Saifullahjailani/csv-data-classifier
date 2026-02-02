package selene.lib;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import selene.lib.category.CSVCategorizer;
import selene.lib.category.manual.ColumnNameProcessor;
import selene.lib.category.type.ClassificationResult;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class Main {

    private static final String VERSION = "1.0.0";

    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            System.exit(1);
        }

        // Parse command line arguments
        CliOptions options = parseArguments(args);

        if (options.showHelp) {
            printUsage();
            return;
        }

        if (options.showVersion) {
            System.out.println("CSV Data Classifier v" + VERSION);
            return;
        }

        if (options.inputFile == null) {
            System.err.println("Error: No input file specified");
            printUsage();
            System.exit(1);
        }

        try {
            processFile(options);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            if (options.verbose) {
                e.printStackTrace();
            }
            System.exit(1);
        }
    }

    private static void processFile(CliOptions options) throws IOException {
        Path inputPath = Path.of(options.inputFile);
        if (!Files.exists(inputPath)) {
            throw new IOException("File not found: " + options.inputFile);
        }

        if (options.verbose) {
            System.out.println("Processing file: " + options.inputFile);
        }

        // Create categorizer and process
        CSVCategorizer categorizer = new CSVCategorizer(options.inputFile);
        Map<String, ColumnNameProcessor.ColumnMetaData> results = categorizer.categorize();
        List<ClassificationResult> classificationResults = categorizer.getClassificationResults();

        // Output results based on format
        String output;
        switch (options.outputFormat.toLowerCase()) {
            case "json":
                output = formatAsJson(classificationResults, categorizer, options.verbose);
                break;
            case "csv":
                output = formatAsCsv(classificationResults);
                break;
            case "table":
            default:
                output = formatAsTable(classificationResults, categorizer, options.verbose);
                break;
        }

        // Write to file or stdout
        if (options.outputFile != null) {
            try (PrintWriter writer = new PrintWriter(new FileWriter(options.outputFile))) {
                writer.print(output);
            }
            if (options.verbose) {
                System.out.println("Results written to: " + options.outputFile);
            }
        } else {
            System.out.print(output);
        }
    }

    private static String formatAsJson(List<ClassificationResult> results, CSVCategorizer categorizer, boolean verbose) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);

            Map<String, Object> output = new LinkedHashMap<>();
            output.put("version", VERSION);
            output.put("summary", Map.of(
                    "rowCount", categorizer.getRowCount(),
                    "columnCount", categorizer.getColumnCount()
            ));

            List<Map<String, Object>> columns = new ArrayList<>();
            for (ClassificationResult result : results) {
                Map<String, Object> col = new LinkedHashMap<>();
                col.put("name", result.getColumnName());
                col.put("elasticType", result.getType().name());
                col.put("categories", result.getCategories());
                columns.add(col);
            }
            output.put("columns", columns);

            return mapper.writeValueAsString(output) + "\n";
        } catch (Exception e) {
            return "{\"error\": \"" + e.getMessage() + "\"}\n";
        }
    }

    private static String formatAsCsv(List<ClassificationResult> results) {
        StringBuilder sb = new StringBuilder();
        sb.append("column_name,elastic_type,categories\n");

        for (ClassificationResult result : results) {
            sb.append(escapeCsv(result.getColumnName())).append(",");
            sb.append(result.getType().name()).append(",");
            sb.append(escapeCsv(String.join(";", result.getCategories()))).append("\n");
        }

        return sb.toString();
    }

    private static String formatAsTable(List<ClassificationResult> results, CSVCategorizer categorizer, boolean verbose) {
        StringBuilder sb = new StringBuilder();

        // Header
        sb.append("╔══════════════════════════════════════════════════════════════════════════════╗\n");
        sb.append("║                         CSV DATA CLASSIFIER RESULTS                          ║\n");
        sb.append("╠══════════════════════════════════════════════════════════════════════════════╣\n");

        // Summary
        sb.append(String.format("║  Rows: %-10d  Columns: %-10d                                    ║\n",
                categorizer.getRowCount(), categorizer.getColumnCount()));
        sb.append("╠══════════════════════════════════════════════════════════════════════════════╣\n");

        // Column details header
        sb.append("║  COLUMN NAME              │  ELASTIC TYPE       │  CATEGORIES                ║\n");
        sb.append("╠══════════════════════════════════════════════════════════════════════════════╣\n");

        for (ClassificationResult result : results) {
            String name = truncate(result.getColumnName(), 23);
            String type = truncate(result.getType().name(), 17);
            String categories = truncate(String.join(", ", result.getCategories()), 25);

            sb.append(String.format("║  %-23s │  %-17s │  %-25s ║\n", name, type, categories));
        }

        sb.append("╚══════════════════════════════════════════════════════════════════════════════╝\n");

        return sb.toString();
    }

    private static String truncate(String str, int maxLen) {
        if (str == null) return "";
        if (str.length() <= maxLen) return str;
        return str.substring(0, maxLen - 3) + "...";
    }

    private static String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private static CliOptions parseArguments(String[] args) {
        CliOptions options = new CliOptions();

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];

            switch (arg) {
                case "-h":
                case "--help":
                    options.showHelp = true;
                    break;
                case "-v":
                case "--version":
                    options.showVersion = true;
                    break;
                case "--verbose":
                    options.verbose = true;
                    break;
                case "-f":
                case "--format":
                    if (i + 1 < args.length) {
                        options.outputFormat = args[++i];
                    }
                    break;
                case "-o":
                case "--output":
                    if (i + 1 < args.length) {
                        options.outputFile = args[++i];
                    }
                    break;
                default:
                    if (!arg.startsWith("-") && options.inputFile == null) {
                        options.inputFile = arg;
                    }
                    break;
            }
        }

        return options;
    }

    private static void printUsage() {
        System.out.println("""
                CSV Data Classifier - Automatic CSV Column Classification Tool

                USAGE:
                    csv-classifier [OPTIONS] <input-file>

                ARGUMENTS:
                    <input-file>            Path to the CSV file to analyze

                OPTIONS:
                    -h, --help              Show this help message
                    -v, --version           Show version information
                    --verbose               Enable verbose output
                    -f, --format <FORMAT>   Output format: table (default), json, csv
                    -o, --output <FILE>     Write output to file instead of stdout

                EXAMPLES:
                    csv-classifier data.csv
                    csv-classifier -f json data.csv
                    csv-classifier -f json -o results.json data.csv
                    csv-classifier --verbose -f csv data.csv > report.csv

                OUTPUT FORMATS:
                    table   - Human-readable ASCII table (default)
                    json    - JSON format with full metadata
                    csv     - CSV format for further processing

                DETECTED CATEGORIES:
                    - PII: email, phone, ssn, credit-card, name, address, etc.
                    - Crypto: hash, bitcoin, ethereum, and 50+ blockchain addresses
                    - Custom: gender, job-title, index, id

                ELASTICSEARCH TYPES:
                    TEXT, KEYWORD, INTEGER, LONG, FLOAT, DOUBLE, BOOLEAN,
                    DATE, IP, GEO_POINT, and more

                For more information, visit: https://github.com/selene/csv-data-classifier
                """);
    }

    private static class CliOptions {
        String inputFile;
        String outputFile;
        String outputFormat = "table";
        boolean showHelp = false;
        boolean showVersion = false;
        boolean verbose = false;
    }
}
