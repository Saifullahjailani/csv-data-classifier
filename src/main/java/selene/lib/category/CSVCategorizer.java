package selene.lib.category;

import selene.lib.category.crypto.CryptoType;
import selene.lib.category.crypto.HashDetector;
import selene.lib.category.gender.GenderDetector;
import selene.lib.category.manual.ColumnNameProcessor;
import selene.lib.category.pii.PIIDetector;
import selene.lib.category.title.JobTitleDetector;
import selene.lib.category.type.ClassificationResult;
import selene.lib.type.ElasticTypes;
import selene.lib.type.TypeDetector;
import tech.tablesaw.api.*;
import tech.tablesaw.io.csv.CsvReadOptions;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class CSVCategorizer {
    private final Table csv;
    private static final ColumnNameProcessor columnNameProcessor = new ColumnNameProcessor();
    public static PIIDetector detector = new PIIDetector();
    private static final int SAMPLE_SIZE = 1000;
    private final Random random = new Random();
    public static double HASH_DETECTION_THRESHOLD = 0.75;
    public static double GENDER_COUNT_THRESHOLD = 0.75;
    public static double JOB_TITLE_COUNT_THRESHOLD = 0.75;

    // Maps original column name -> metadata (with clean name, type, categories)
    public final Map<String, ColumnNameProcessor.ColumnMetaData> columnsMetaData = new LinkedHashMap<>();


    public CSVCategorizer (String pathString) throws RuntimeException {
        Path path = Path.of(pathString);
        String fileName = path.getFileName().toString().toLowerCase();
        if(!fileName.endsWith(".csv")){
            throw new RuntimeException("Invalid file type: expected .csv file");
        }
        if(!Files.exists(path)){
            throw new RuntimeException("File not found: " + pathString);
        }
        this.csv = readCsv(pathString);

        // Parse column names for manual type/category annotations
        for(String originalColumnName : csv.columnNames()){
            ColumnNameProcessor.ColumnMetaData parsed = columnNameProcessor.getMetaData(originalColumnName);
            columnsMetaData.put(originalColumnName, parsed);
        }
    }

    public Map<String, ColumnNameProcessor.ColumnMetaData> categorize(){
        for(int i = 0 ; i < csv.columnCount(); i ++){
            StringColumn column = csv.stringColumn(i);
            var meta = columnsMetaData.get(column.name());

            // Check if column has manual annotations - skip detection if fully specified
            boolean hasManualType = meta.getElasticType() != null && meta.getElasticType() != ElasticTypes.UNKNOWN;
            boolean hasManualCategories = meta.getCategory() != null && !meta.getCategory().isEmpty();

            // If both type and categories are manually specified, skip detection entirely
            if (hasManualType && hasManualCategories) {
                continue;
            }

            // Only detect categories if not manually specified
            if (!hasManualCategories) {
                // Check if column is an index
                if(isIndex(column)){
                    meta.setElasticType(ElasticTypes.INTEGER);
                    meta.addCategory(CustomCategories.INDEX.getType());
                }

                StringColumn sampledColumn = sampleColumn(column);
                Set<String> detectedCategories = new HashSet<>(prePIIAnalysis(sampledColumn));

                if(detectedCategories.isEmpty()){
                    String maxKey = detector.getFrequency(sampledColumn).entrySet().stream()
                            .max(Map.Entry.comparingByValue())
                            .map(Map.Entry::getKey)
                            .orElse("");
                    if(!maxKey.isBlank()){
                        detectedCategories.add(maxKey.toLowerCase());
                    }
                }

                detectedCategories.forEach(meta::addCategory);

                // Check if all values are unique (potential ID column)
                if(sampledColumn.countMissing() == 0 && sampledColumn.countUnique() == sampledColumn.size()){
                    meta.addCategory(CustomCategories.ID.getType());
                }
            }
        }

        // Infer elastic types for columns without manual type
        inferElasticTypes();

        return columnsMetaData;
    }

    public void inferElasticTypes(){
        for(int i = 0; i < csv.columnCount(); i++){
            StringColumn column = csv.stringColumn(i);
            var meta = columnsMetaData.get(column.name());

            // Skip if type already set (manually or from index detection)
            if(meta.getElasticType() != null && meta.getElasticType() != ElasticTypes.UNKNOWN){
                continue;
            }

            // Get column values for type inference
            List<String> columnValues = StreamSupport.stream(column.spliterator(), false)
                    .limit(SAMPLE_SIZE)
                    .collect(Collectors.toList());

            ElasticTypes inferredType = TypeDetector.inferColumnType(
                    meta.getCategory(),
                    columnValues
            );

            meta.setElasticType(inferredType);
        }
    }

    public List<ClassificationResult> getClassificationResults(){
        return columnsMetaData.values().stream()
                .map(meta -> ClassificationResult.builder()
                        .columnName(meta.getColumnName())
                        .categories(new ArrayList<>(meta.getCategory()))
                        .type(meta.getElasticType())
                        .build())
                .collect(Collectors.toList());
    }

    public int getRowCount(){
        return csv.rowCount();
    }

    public int getColumnCount(){
        return csv.columnCount();
    }

    public List<String> getColumnNames(){
        return csv.columnNames();
    }

    private boolean isIndex(StringColumn column){
        column = getNotMissingRandomContiguousBlock(column);
        try {
            DoubleColumn col = column.parseDouble();
            double confidence = indexConfidence(col);
            return confidence >= 0.7;
        } catch (Exception _){
            return false;
        }
    }

    public static double indexConfidence(NumericColumn<?> column) {
        if (column.size() <= 1) return 0.0;

        double score = 0.0;

        // 1. Uniqueness (weight: 0.2)
        double uniqueRatio = (double) column.unique().size() / column.size();
        score += 0.2 * uniqueRatio;

        // 2. Monotonically increasing (weight: 0.25)
        int monotonicCount = 0;
        int sequentialCount = 0; // diff == 1
        int totalPairs = column.size() - 1;

        for (int i = 1; i < column.size(); i++) {
            double prev = column.getDouble(i - 1);
            double curr = column.getDouble(i);
            double diff = curr - prev;

            if (diff > 0) monotonicCount++;
            if (Math.abs(diff - 1.0) < 0.001) sequentialCount++;
        }

        double monotonicRatio = (double) monotonicCount / totalPairs;
        score += 0.25 * monotonicRatio;

        // 3. Sequential ratio — most diffs are 1 (weight: 0.35, heaviest)
        double sequentialRatio = (double) sequentialCount / totalPairs;
        score += 0.35 * sequentialRatio;

        // 4. Starts near 0 or 1 (weight: 0.1)
        double firstVal = column.getDouble(0);
        if (firstVal == 0 || firstVal == 1) {
            score += 0.1;
        } else if (firstVal < 10) {
            score += 0.05;
        }

        // 5. All values are whole numbers (weight: 0.1)
        long intCount = 0;
        for (int i = 0; i < column.size(); i++) {
            double val = column.getDouble(i);
            if (val == Math.floor(val)) intCount++;
        }
        double intRatio = (double) intCount / column.size();
        score += 0.1 * intRatio;

        return Math.min(1.0, Math.max(0.0, score));
    }

    private static Table readCsv(String path){
        return Table.read().csv(CsvReadOptions.builder(path).columnTypes(
                type ->
                        ColumnType.STRING
        ));
    }


    private StringColumn sampleColumn(StringColumn col){
        if(col.size() > SAMPLE_SIZE){
            return col.sampleN(SAMPLE_SIZE);
        }
        return col;
    }

    private StringColumn getNotMissingRandomContiguousBlock(StringColumn column){
        if(SAMPLE_SIZE > column.size()){
            return column.removeMissing();
        }
        int maxStart = column.size() - SAMPLE_SIZE;
        int start = random.nextInt(maxStart + 1);
        return column.inRange(start, start + SAMPLE_SIZE).removeMissing();
    }

    private static List<String> prePIIAnalysis(StringColumn column){
        List<String> categories = new ArrayList<>();
        int size = column.size();
        int isGender = 0;
        int isJobTitle = 0;
        int isHash = 0;
        Map<String, Integer> hashTypes = new HashMap<>();

        for(var val : column){
            if(GenderDetector.isGenderIdentifier(val)){
                isGender++;
            }
            if(JobTitleDetector.isJobTitle(val)){
                isJobTitle++;
            }
            var result = HashDetector.analyze(val);
            if(result.isHash()){
                isHash++;
                for (var type : result.getHashType().stream().map(CryptoType::getName).toList()){
                    var prev = hashTypes.getOrDefault(type, 0);
                    hashTypes.put(type, prev+1);
                }
            }
        }

        if((double) isGender / size > GENDER_COUNT_THRESHOLD){
            categories.add(CustomCategories.GENDER.getType());
        }
        if((double) isJobTitle / size > JOB_TITLE_COUNT_THRESHOLD){
            categories.add(CustomCategories.JOB_TITLE.getType());
        }
        if((double) isHash / size > HASH_DETECTION_THRESHOLD){
            categories.add(CustomCategories.HASH.getType());
            categories.addAll(
                    getTop(hashTypes, 5).stream().map(String::toLowerCase).toList()
            );
        }



        return categories;

    }
    private static List<String> getTop(Map<String, Integer> typeFrequency,int n){
        return typeFrequency.entrySet()
                .stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
                .limit(n)
                .map(Map.Entry::getKey)
                .toList();
    }
}
