package selene.lib.csv.classfiers.base;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;
import selene.lib.csv.statistics.StatisticalProfile;
import selene.lib.csv.types.CategoryType;
import tech.tablesaw.api.StringColumn;

@AllArgsConstructor
@Setter
public abstract class AbstractCategoryClassifier implements CategoryClassifier{
    protected final ClassifiersConfigs.ClassifierConfig config;

    @Override
    public final ClassificationResult classify(StringColumn column, StatisticalProfile profile){
        if (!passesGates(profile)) {
            return ClassificationResult
                    .builder()
                    .isMatch(false)
                    .reason(getFailureReason(profile))
                    .build();
        }

        double patternMatch = calculatePatternMatch(column);
        if (patternMatch < config.getMinPatternMatch()) {
            return failResult("Pattern compliance too low: " + patternMatch);
        }
        double score = calculateScore(profile, patternMatch);

        return ClassificationResult.builder()
                .isMatch(true)
                .category(getCategory())
                .confidence(score)
                .build();
    }

    private ClassificationResult failResult(String reason){
        return ClassificationResult
                .builder()
                .isMatch(false)
                .reason(reason)
                .build();
    }

    private ClassificationResult failResult(StatisticalProfile profile){
        return ClassificationResult
                .builder()
                .isMatch(false)
                .reason(getFailureReason(profile))
                .build();
    }

    protected abstract boolean passesGates(StatisticalProfile profile);
    protected abstract double calculatePatternMatch(StringColumn column);
    protected abstract double calculateScore(StatisticalProfile profile, double patternMatch);
    protected abstract String getFailureReason(StatisticalProfile profile);
    protected abstract CategoryType getCategory();
}
