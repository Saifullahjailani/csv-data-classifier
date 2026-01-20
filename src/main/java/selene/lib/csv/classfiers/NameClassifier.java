package selene.lib.csv.classfiers;

import selene.lib.csv.classfiers.base.AbstractCategoryClassifier;
import selene.lib.csv.classfiers.base.ClassifiersConfigs;
import selene.lib.csv.statistics.StatisticalProfile;
import selene.lib.csv.types.CategoryType;
import tech.tablesaw.api.StringColumn;

public class NameClassifier extends AbstractCategoryClassifier {

    public NameClassifier(ClassifiersConfigs.ClassifierConfig config) {
        super(config);
    }

    @Override
    protected boolean passesGates(StatisticalProfile profile) {
        return profile.getAtSymbolDensity() <= config.getMaxAtSymbolDensity() &&
                profile.getLetterPercentage() >= config.getMinLetterPercentage() &&
                profile.getSymbolPercentage() <= config.getMaxSymbolPercentage() &&
                profile.getMeanLength() >= config.getMinMeanLength() &&
                profile.getShannonEntropy() <= config.getMaxEntropy() &&
                profile.getTitleCaseRate() >= config.getMinTitleCaseRate() &&
                profile.getUniqueRatio() > config.getMinUniqueRatio() &&  // Reject low uniqueness (categories)
                profile.getDictionaryCoverage() < config.getMaxDictionaryCoverage(); // Reject high coverage (locations)
    }

    @Override
    protected double calculatePatternMatch(StringColumn column) {
        long valid = column.size() - column.countMissing();
        long match_count = getCategory().matchCount(column);
        return (double) match_count / valid;
    }

    @Override
    protected double calculateScore(StatisticalProfile profile, double patternMatch) {
        // Direct letter percentage = perfect score (0.70-1.00)
        double letterScore = profile.getLetterPercentage();

        // Zero symbols is perfect for single-word names
        double symbolScore = profile.getSymbolPercentage() < 0.001 ?
                1.0 :
                1.0 - Math.min(profile.getSymbolPercentage() * 8, 1.0);

        // Word count: 1-3 words optimal
        double wordCountScore;
        double avgWords = profile.getAverageWordCount();
        if (avgWords >= 1.0 && avgWords <= 3.5) {
            wordCountScore = 1.0;
        } else {
            wordCountScore = Math.max(1.0 - Math.abs(avgWords - 2.0) * 0.4, 0.0);
        }

        // Length variance
        double lengthScore = Math.min(profile.getLengthStdDev() / 8.0, 1.0);

        // Entropy score
        double entropyScore;
        if (profile.getShannonEntropy() > 4.0) {
            entropyScore = profile.getTitleCaseRate() > 0.85 ? 0.8 : 0.5;
        } else {
            entropyScore = Math.max((4.0 - profile.getShannonEntropy()) / 4.0, 0.0);
        }

        // Dictionary coverage bonus/penalty
        double dictionaryScore = 0.0;
        double coverage = profile.getDictionaryCoverage();
        if (coverage > 0.20 && coverage < 0.60) {
            dictionaryScore = config.getMaxDictionaryCoverage(); // Bonus for moderate name coverage
        } else if (coverage > 0.70) {
            dictionaryScore = -0.05; // Penalty for location names
        }

        return (patternMatch * config.getPatternWeight()) +
                (letterScore * config.getLetterWeight()) +
                (wordCountScore * config.getWordCountWeight()) +
                (symbolScore * config.getSymbolWeight()) +
                (lengthScore * config.getLengthWeight()) +
                (entropyScore * config.getEntropyWeight()) +
                dictionaryScore;
    }

    @Override
    protected String getFailureReason(StatisticalProfile profile) {
        if (profile.getAtSymbolDensity() > config.getMaxAtSymbolDensity())
            return "Contains @ symbols";
        if (profile.getLetterPercentage() < config.getMinLetterPercentage())
            return "Too few letters: " + profile.getLetterPercentage();
        if (profile.getSymbolPercentage() > config.getMaxSymbolPercentage())
            return "Too many symbols: " + profile.getSymbolPercentage();
        if (profile.getMeanLength() < config.getMinMeanLength())
            return "Too short: " + profile.getMeanLength();
        if (profile.getShannonEntropy() > config.getMaxEntropy())
            return "Too random/high entropy: " + profile.getShannonEntropy();
        if (profile.getTitleCaseRate() < config.getMinTitleCaseRate())
            return "Not Title Case: " + profile.getTitleCaseRate();
        if (profile.getUniqueRatio() <= config.getMinUniqueRatio())
            return "Low uniqueness (likely categories): " + profile.getUniqueRatio();
        if (profile.getDictionaryCoverage() >= config.getMaxDictionaryCoverage())
            return "High dictionary coverage (likely locations): " + profile.getDictionaryCoverage();
        return "Failed name classification gates";
    }

    @Override
    protected CategoryType getCategory() {
        return CategoryType.NAME;
    }
}