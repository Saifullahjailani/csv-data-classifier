package selene.lib.csv.classfiers;

import selene.lib.csv.classfiers.base.AbstractCategoryClassifier;
import selene.lib.csv.classfiers.base.ClassifiersConfigs;
import selene.lib.csv.statistics.StatisticalProfile;
import selene.lib.csv.types.CategoryType;
import tech.tablesaw.api.StringColumn;

public class SSNClassifier extends AbstractCategoryClassifier {

    public SSNClassifier(ClassifiersConfigs.ClassifierConfig config) {
        super(config);
    }

    @Override
    protected boolean passesGates(StatisticalProfile profile) {
        // Gate 1: High digit percentage (allow 70-100% for both formatted and unformatted)
        if (profile.getDigitPercentage() < config.getMinDigitPercentage()) {
            return false;
        }

        // Gate 2: Very low length variance (single value has 0 std dev - that's perfect!)
        if (profile.getLengthStdDev() > config.getMaxLengthStdDev()) {
            return false;
        }

        // Gate 3: Symbol percentage flexibly low (allow 0% symbols for unformatted)
        if (profile.getSymbolPercentage() < config.getMinSymbolPercentage() ||
                profile.getSymbolPercentage() > config.getMaxSymbolPercentage()) {
            return false;
        }

        // Gate 4: No @ symbols (strict)
        if (profile.getAtSymbolDensity() > config.getMaxAtSymbolDensity()) {
            return false;
        }

        // Gate 5: High uniqueness (single value has 1.0 uniqueness - perfect!)
        if (profile.getUniqueRatio() < config.getMinUniqueRatio()) {
            return false;
        }

        // Gate 6: Moderate entropy (not too random)
        if (profile.getShannonEntropy() > config.getMaxEntropy()) {
            return false;
        }

        return true;
    }

    @Override
    protected double calculatePatternMatch(StringColumn column) {
        long valid = column.size() - column.countMissing();
        long match_count = getCategory().matchCount(column);
        return (double) match_count / valid;
    }

    @Override
    protected double calculateScore(StatisticalProfile profile, double patternMatch) {
        // Length consistency: reward very low variance (0 is perfect for single SSN)
        double lengthScore = 1.0 - Math.min(profile.getLengthStdDev(), 1.0);

        // Digit percentage: should be very high (formatted SSN has 9 digits out of 11 chars)
        double digitScore = profile.getDigitPercentage();

        // Symbol density: moderate (dashes) but not too high
        double symbolScore = 1.0 - Math.min(Math.abs(profile.getSymbolPercentage() - 0.15) * 10, 1.0);

        // Uniqueness: should be near 1.0 (real SSNs are unique)
        double uniquenessScore = profile.getUniqueRatio();

        // Entropy: moderate (structured, not random)
        double entropyScore;
        if (profile.getShannonEntropy() > 4.5) {
            entropyScore = patternMatch > 0.85 ? 0.6 : 0.3;
        } else {
            entropyScore = Math.max((4.5 - profile.getShannonEntropy()) / 4.5, 0.0);
        }

        // Apply weighted scoring
        double weightedScore =
                (patternMatch * config.getPatternWeight()) +
                        (lengthScore * config.getLengthWeight()) +
                        (digitScore * config.getLetterWeight()) +      // Reused: letterWeight stores digit weight
                        (symbolScore * config.getSymbolWeight()) +
                        (uniquenessScore * config.getWordCountWeight()) + // Reused: wordCountWeight stores uniqueness weight
                        (entropyScore * config.getEntropyWeight());

        // Clamp to [0.0, 1.0]
        return Math.max(0.0, Math.min(weightedScore, 1.0));
    }

    @Override
    protected String getFailureReason(StatisticalProfile profile) {
        if (profile.getDigitPercentage() < config.getMinDigitPercentage())
            return "Too few digits: " + profile.getDigitPercentage();
        if (profile.getLengthStdDev() > config.getMaxLengthStdDev())
            return "Inconsistent length: " + profile.getLengthStdDev();
        if (profile.getSymbolPercentage() < config.getMinSymbolPercentage() ||
                profile.getSymbolPercentage() > config.getMaxSymbolPercentage())
            return "Symbol percentage out of range: " + profile.getSymbolPercentage();
        if (profile.getAtSymbolDensity() > config.getMaxAtSymbolDensity())
            return "Contains @ symbols";
        if (profile.getUniqueRatio() < config.getMinUniqueRatio())
            return "Too many duplicates (SSNs should be unique): " + profile.getUniqueRatio();
        if (profile.getShannonEntropy() > config.getMaxEntropy())
            return "Entropy too high: " + profile.getShannonEntropy();
        return "Failed SSN classification gates";
    }

    @Override
    protected CategoryType getCategory() {
        return CategoryType.SSN;
    }
}