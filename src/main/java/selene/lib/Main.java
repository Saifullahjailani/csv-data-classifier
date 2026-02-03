package selene.lib;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import selene.lib.service.ClassificationService;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.Callable;

@Command(
    name = "csv-classifier",
    mixinStandardHelpOptions = true,
    version = "1.0.0",
    description = "Analyze CSV columns and generate Elasticsearch mappings"
)
public class Main implements Callable<Integer> {

    private static final ObjectMapper mapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    @Parameters(index = "0", description = "CSV file to analyze")
    private Path inputFile;

    @Option(names = {"-f", "--format"}, description = "Output format: json (default), csv")
    private String format = "json";

    @Option(names = {"-o", "--output"}, description = "Output file (default: stdout)")
    private Path outputFile;

    @Override
    public Integer call() throws Exception {
        if (!Files.exists(inputFile)) {
            System.err.println("Error: File not found: " + inputFile);
            return 1;
        }

        // Run classification
        ClassificationService service = new ClassificationService(inputFile.toString()).classify();

        // Generate output
        String output = switch (format.toLowerCase()) {
            case "csv" -> service.toAnnotatedCsv();
            default -> mapper.writeValueAsString(service.toJsonOutput());
        };

        // Write output
        if (outputFile != null) {
            try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile.toFile()))) {
                writer.println(output);
            }
        } else {
            System.out.println(output);
        }

        return 0;
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }
}
