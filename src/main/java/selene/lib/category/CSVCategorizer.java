package selene.lib.category;

import selene.lib.category.crypto.CryptoType;
import selene.lib.category.crypto.HashDetector;
import selene.lib.category.gender.GenderDetector;
import selene.lib.category.manual.ColumnNameProcessor;
import selene.lib.category.pii.PIIDetector;
import selene.lib.category.title.JobTitleDetector;
import selene.lib.type.ElasticTypes;
import tech.tablesaw.api.*;
import tech.tablesaw.io.csv.CsvReadOptions;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class CSVCategorizer {
    private final Table csv;
    public static PIIDetector detector = new PIIDetector();
    private static final int SAMPLE_SIZE = 1000;
    private final Random random = new Random();
    public static double HASH_DETECTION_THRESHOLD = 0.75;
    public static double GENDER_COUNT_THRESHOLD = 0.75;
    public static double JOB_TITLE_COUNT_THRESHOLD = 0.75;

    public final Map<String, ColumnNameProcessor.ColumnMetaData> columnsMetaData = new HashMap<>();



    public CSVCategorizer (String pathString) throws RuntimeException {
        Path path = Path.of(pathString);
        if(!path.endsWith("csv")){
            throw new RuntimeException("Invalid file type");
        }
        if(!Files.exists(path)){
            throw new RuntimeException("Invalid file type");
        }
        this.csv = readCsv(pathString);

        for(String columnName : csv.columnNames()){
            columnsMetaData.put(columnName,
                    ColumnNameProcessor.ColumnMetaData
                            .builder()
                            .columnName(columnName)
                            .category(new ArrayList<>())
                            .elasticType(ElasticTypes.UNKNOWN)
                            .build());
        }

    }

    public void categorize(){
        // Check if any of the columns is index
        for(int i = 0 ; i < csv.columnCount(); i ++){
            StringColumn column = csv.stringColumn(i);
            if(isIndex(column)){
                var obj = columnsMetaData.get(column.name());
                obj.setElasticType(ElasticTypes.INTEGER);
                obj.addCategory(CustomCategories.INDEX.getType());

            }
            column = sampleColumn(column);
            Set<String> preCategories = new HashSet<>(prePIIAnalysis(column));
            if(preCategories.isEmpty()){
                String maxKey = detector.getFrequency(column).entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .map(Map.Entry::getKey)
                        .orElse("");
                if(!maxKey.isBlank()){
                    preCategories.add(maxKey.toLowerCase());
                }
            }
            var obj = columnsMetaData.get(column.name());
            preCategories.forEach(obj::addCategory);
            if(column.countMissing() == 0 && column.countUnique() == column.size()){
                preCategories.add(CustomCategories.ID.getType());
            }

        }
    }

    public void inferElasticTypes(){
        // fixme infer the ElasticType based on the following things
        // Category
        // Table Name if exists
        // The data in that column
    }

    private boolean isIndex(StringColumn column){
        column = getNotMisingRandomContiguousBlock(column);
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

    private StringColumn getNotMisingRandomContiguousBlock(StringColumn column){

        if(SAMPLE_SIZE > column.size()){
            return column;
        }
        int maxStart = column.size() - SAMPLE_SIZE;
        int start = random.nextInt(maxStart);
        return column.inRange(start, start + 1).removeMissing();
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
