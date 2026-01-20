package selene.lib.csv.classfiers;

import selene.lib.csv.classfiers.base.AbstractCategoryClassifier;
import selene.lib.csv.classfiers.base.ClassifiersConfigs;
import selene.lib.csv.statistics.StatisticalProfile;
import selene.lib.csv.types.CategoryType;
import tech.tablesaw.api.StringColumn;

public class EmailClassifier extends AbstractCategoryClassifier {

    public EmailClassifier(ClassifiersConfigs.ClassifierConfig config) {
        super(config);
    }

    @Override
    protected boolean passesGates(StatisticalProfile profile) {
        // Gate 1: @ density between 0.95-1.05
        if (profile.getAtSymbolDensity() < config.getMinAtSymbolDensity() ||
                profile.getAtSymbolDensity() > config.getMaxAtSymbolDensity()) {
            return false;
        }

        // Gate 2: CV of @ position < 0.6
        if (profile.getAtPositionCoefficientOfVariation() > config.getMaxAtPositionCV()) {
            return false;
        }

        // Gate 3: Length std dev > 2.0
        if (profile.getLengthStdDev() < 2.0) {
            return false;
        }

        // Gate 4: Symbol percentage between 0.03-0.30
        if (profile.getSymbolPercentage() < config.getMinSymbolPercentage() ||
                profile.getSymbolPercentage() > config.getMaxSymbolPercentage()) {
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
        // atScore: 1.0 - Math.min(Math.abs(profile.getAtSymbolDensity() - 1.0) * 5, 1.0)
        double atScore = 1.0 - Math.min(Math.abs(profile.getAtSymbolDensity() - 1.0) * 5, 1.0);

        // positionScore: 1.0 - Math.min(profile.getAtPositionCoefficientOfVariation(), 1.0)
        double positionScore = 1.0 - Math.min(profile.getAtPositionCoefficientOfVariation(), 1.0);

        // symbolScore: 1.0 - Math.min(Math.abs(profile.getSymbolPercentage() - 0.12) * 8, 1.0)
        double symbolScore = 1.0 - Math.min(Math.abs(profile.getSymbolPercentage() - 0.12) * 8, 1.0);

        // lengthScore: Math.min(profile.getLengthStdDev() / 15.0, 1.0)
        double lengthScore = Math.min(profile.getLengthStdDev() / 15.0, 1.0);

        // entropyScore (same as static method)
        double entropyScore;
        if (profile.getShannonEntropy() > 5.0) {
            entropyScore = 0.0;
        } else if (profile.getShannonEntropy() > 4.5) {
            entropyScore = patternMatch > 0.85 ? 0.7 : 0.3;
        } else {
            entropyScore = Math.max((4.5 - profile.getShannonEntropy()) / 4.5, 0.0);
        }

        // Weighted scoring (using config weights that match static method)
        double weightedScore =
                (patternMatch * config.getPatternWeight()) +      // 0.55
                        (atScore * config.getAtSymbolWeight()) +         // 0.23
                        (positionScore * config.getPositionWeight()) +   // 0.12
                        (symbolScore * config.getSymbolWeight()) +       // 0.10
                        (lengthScore * config.getLengthWeight()) +       // 0.05
                        (entropyScore * config.getEntropyWeight());      // 0.05

        // Bonuses (same as static method)
        if (profile.getDictionaryCoverage() > 0.30) weightedScore += 0.02;
        if (profile.getAverageWordCount() >= 1.5 && profile.getAverageWordCount() <= 2.5) weightedScore += 0.02;
        if (profile.getPatternOutlierPct() < 0.05) weightedScore += 0.01;

        // Penalty (same as static method)
        if (profile.getPatternOutlierPct() > 0.15) weightedScore -= 0.03;

        // Clamp to [0.0, 1.0] (same as static method)
        return Math.max(0.0, Math.min(weightedScore, 1.0));
    }

    @Override
    protected String getFailureReason(StatisticalProfile profile) {
        if (profile.getAtSymbolDensity() < config.getMinAtSymbolDensity() ||
                profile.getAtSymbolDensity() > config.getMaxAtSymbolDensity()) {
            return "@ symbol density out of range: " + profile.getAtSymbolDensity();
        }
        if (profile.getAtPositionCoefficientOfVariation() > config.getMaxAtPositionCV()) {
            return "@ position CV too high: " + profile.getAtPositionCoefficientOfVariation();
        }
        if (profile.getLengthStdDev() < 2.0) {
            return "Insufficient length variance: " + profile.getLengthStdDev();
        }
        if (profile.getSymbolPercentage() < config.getMinSymbolPercentage() ||
                profile.getSymbolPercentage() > config.getMaxSymbolPercentage()) {
            return "Symbol percentage out of range: " + profile.getSymbolPercentage();
        }
        return "Failed email classification gates";
    }

    @Override
    protected CategoryType getCategory() {
        return CategoryType.EMAIL;
    }
}