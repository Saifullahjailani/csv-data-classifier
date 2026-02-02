package selene.lib.category.pii;

import ai.philterd.phileas.model.filtering.FilterType;
import ai.philterd.phileas.model.filtering.Span;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CsvDetect {

    private static final PIIDetector detector = new PIIDetector();

    public static List<CategoryScore> categorizeColumn(StringColumn column) {
        Map<FilterType, Integer> frequency = new HashMap<>();
        double total = column.size();

        for (String value : column) {
            String candidate = value.trim().toLowerCase();
            if (candidate.isBlank()) {
                continue;
            }

            List<Span> spans = detector.detect(candidate);
            for (Span span : spans) {
                FilterType type = span.getFilterType();
                frequency.merge(type, 1, Integer::sum);
            }
        }

        List<CategoryScore> scores = new ArrayList<>();
        for (Map.Entry<FilterType, Integer> entry : frequency.entrySet()) {
            scores.add(new CategoryScore(entry.getKey(), entry.getValue() / total));
        }

        return scores;
    }


    public static Map<String,List<CategoryScore>> categorizeTable(Table table){
        Map<String,List<CategoryScore>> scores = new HashMap<>();
        for(var col : table.columnNames()){
            scores.put(
                    col,
                    categorizeColumn(
                            table.stringColumn(col)
                    )
            );
        }
        return scores;
    }



    @Getter
    @Setter
    @AllArgsConstructor
    public static final class CategoryScore {
        public final FilterType type;
        public final double ratio;

    }
}
