package selene.lib.csv.statistics;

import selene.lib.csv.classfiers.base.ClassificationResult;
import selene.lib.csv.types.CategoryType;
import tech.tablesaw.api.StringColumn;

public class Classifiers {


    private static double matchRatio(StringColumn column, CategoryType type){
        long totalValues = column.size();
        long missingValues = column.countMissing();
        long totalValidValues = totalValues - missingValues;
        long matchCounter = 0;
        for(String str : column){
            if(str == null || str.isEmpty()) continue;
            if(type.matches(str)){
                matchCounter++;
            }
        }
        return (double) matchCounter / totalValidValues;
    }
    public static ClassificationResult classifyEmail(
            StatisticalProfile profile,
            StringColumn column) {

        // ===== TIER 1: CRITICAL GATES (Slightly relaxed) =====
        if (profile.getAtSymbolDensity() < 0.95 || profile.getAtSymbolDensity() > 1.05) {
            return ClassificationResult
                    .builder()
                    .isMatch(false)
                    .reason("@ symbol density must be ~1.0, found: " + profile.getAtSymbolDensity())
                    .build();
        }

        // Gate: Use CV but increase threshold to 0.6 for real-world variance
        if (profile.getAtPositionCoefficientOfVariation() > 0.6) {
            return ClassificationResult.builder()
                    .isMatch(false)
                    .reason("@ position CV too high: " + profile.getAtPositionCoefficientOfVariation()).build();
        }

        if (profile.getLengthStdDev() < 2.0) {
            return ClassificationResult.builder()
                    .isMatch(false)
                    .reason("Insufficient length variance: " + profile.getLengthStdDev())
                            .build();
        }

        if (profile.getSymbolPercentage() < 0.03 || profile.getSymbolPercentage() > 0.30) {
            return ClassificationResult.builder()
                    .isMatch(false)
                    .reason("Symbol percentage out of email range: " + profile.getSymbolPercentage())
                    .build();
        }

        // ===== TIER 2: PATTERN COMPLIANCE =====
        double patternMatchPct = matchRatio(column, CategoryType.EMAIL);
        if (patternMatchPct < 0.70) {  // Slightly lowered from 0.75
            return ClassificationResult.builder()
                    .isMatch(false)
                    .reason("Pattern compliance too low: " + patternMatchPct).build();
        }

        // ===== TIER 3: STATISTICAL SCORING (FIXED WEIGHTS & CALCULATIONS) =====
        double atScore = 1.0 - Math.min(Math.abs(profile.getAtSymbolDensity() - 1.0) * 5, 1.0);  // *5 instead of *2 = less penalty

        // Use CV directly: CV of 0.3 gives score 0.7, CV of 0.5 gives score 0.5
        double positionScore = 1.0 - Math.min(profile.getAtPositionCoefficientOfVariation(), 1.0);

        double lengthScore = Math.min(profile.getLengthStdDev() / 15.0, 1.0);

        double entropyScore;
        if (profile.getShannonEntropy() > 5.0) {
            entropyScore = 0.0;
        } else if (profile.getShannonEntropy() > 4.5) {
            entropyScore = patternMatchPct > 0.85 ? 0.7 : 0.3;  // Increased from 0.6/0.2
        } else {
            entropyScore = Math.max((4.5 - profile.getShannonEntropy()) / 4.5, 0.0);
        }

        // Symbol score: emails have ~10-15% symbols (@ and .). Relax tolerance.
        double symbolScore = 1.0 - Math.min(Math.abs(profile.getSymbolPercentage() - 0.12) * 8, 1.0);  // *8 instead of *10

        // INCREASED pattern weight (dominant signal), REDUCED penalty weights
        double weightedScore =
                (patternMatchPct * 0.55) +      // Increased from 0.50
                        (atScore * 0.23) +              // Decreased from 0.25
                        (positionScore * 0.12) +        // Increased from 0.10
                        (symbolScore * 0.10) +          // Increased from 0.08
                        (lengthScore * 0.05) +          // Same
                        (entropyScore * 0.05);          // Increased from 0.02

        // Apply bonuses (can now push above 0.95)
        if (profile.getDictionaryCoverage() > 0.30) weightedScore += 0.02;
        if (profile.getAverageWordCount() >= 1.5 && profile.getAverageWordCount() <= 2.5) weightedScore += 0.02;
        if (profile.getPatternOutlierPct() < 0.05) weightedScore += 0.01;  // Bonus for clean data

        if (profile.getPatternOutlierPct() > 0.15) weightedScore -= 0.03;  // Reduced penalty

        // NO HARD CAP - let perfect scores exceed 0.95, clamp only at 1.0
        weightedScore = Math.max(0.0, Math.min(weightedScore, 1.0));

        // ===== TIER 4: BOOTSTRAP VALIDATION =====


        return ClassificationResult.builder()
                .isMatch(true)
                .confidence(weightedScore)
                .category(CategoryType.EMAIL)
                .build();
    }

    public static ClassificationResult classifyPhoneNumber(
            StatisticalProfile profile,
            StringColumn column) {

        // ===== TIER 1: CRITICAL GATES (Hard disqualifiers) =====

        // Gate 1: Must have high digit content (phones are numeric)
        if (profile.getDigitPercentage() < 0.70) {
            return ClassificationResult.builder()
                    .isMatch(false)
                    .reason("Digit percentage too low for phone: " + profile.getDigitPercentage())
                    .build();
        }

        // Gate 2: Must contain formatting symbols (dashes, dots, parens, spaces)
        if (profile.getSymbolPercentage() < 0.05 || profile.getSymbolPercentage() > 0.30) {
            return ClassificationResult.builder()
                    .isMatch(false)
                    .reason("Symbol percentage out of phone range: " + profile.getSymbolPercentage())
                    .build();
        }

        // Gate 3: Must have reasonable length variance (formats differ but not too much)
        if (profile.getLengthStdDev() < 1.5) {
            return ClassificationResult.builder()
                    .isMatch(false)
                    .reason("Insufficient length variance for phone: " + profile.getLengthStdDev())
                    .build();
        }

        // Gate 4: Cannot be cryptographic randomness (entropy too high)
        if (profile.getShannonEntropy() > 4.2) {
            return ClassificationResult.builder()
                    .isMatch(false)
                    .reason("Entropy too high for phone: " + profile.getShannonEntropy())
                    .build();
        }

        // ===== TIER 2: PATTERN COMPLIANCE =====
        double patternMatchPct = matchRatio(column, CategoryType.PHONE);

        if (patternMatchPct < 0.65) {
            return ClassificationResult.builder()
                    .isMatch(false)
                    .reason("Pattern compliance too low: " + patternMatchPct)
                    .build();
        }

        // ===== TIER 3: STATISTICAL SCORING =====

        // Digit content score: optimal 80-85% (25% weight)
        double digitScore = 1.0 - Math.min(Math.abs(profile.getDigitPercentage() - 0.80) * 5, 1.0);

        // Pattern match score: dominant signal (50% weight)
        double patternScore = patternMatchPct;

        // Symbol consistency score: formatting symbols should be present (10% weight)
        double symbolScore = 1.0 - Math.min(Math.abs(profile.getSymbolPercentage() - 0.15) * 8, 1.0);

        // Length variance score: reward moderate variance (10% weight)
        double lengthScore = Math.min(profile.getLengthStdDev() / 10.0, 1.0);

        // Entropy score: moderate entropy expected (5% weight)
        double entropyScore;
        if (profile.getShannonEntropy() > 4.0) {
            entropyScore = patternMatchPct > 0.85 ? 0.6 : 0.3;
        } else {
            entropyScore = Math.max((4.0 - profile.getShannonEntropy()) / 4.0, 0.0);
        }

        double weightedScore =
                (patternScore * 0.50) +
                        (digitScore * 0.25) +
                        (symbolScore * 0.10) +
                        (lengthScore * 0.10) +
                        (entropyScore * 0.05);

        // Bonuses for strong signals
        if (profile.getDictionaryCoverage() > 0.20) weightedScore += 0.02; // Area codes, common prefixes
        if (profile.getPatternOutlierPct() < 0.05) weightedScore += 0.02; // Clean data bonus

        // Penalty for excessive outliers
        if (profile.getPatternOutlierPct() > 0.20) weightedScore -= 0.03;

        weightedScore = Math.max(0.0, weightedScore); // No upper cap

        return ClassificationResult.builder()
                .isMatch(true)
                .confidence(weightedScore)
                .category(CategoryType.PHONE)
                .build();
    }


    public static ClassificationResult classifyName(
            StatisticalProfile profile,
            StringColumn column) {

        // ===== TIER 1: CRITICAL GATES (Hard disqualifiers) =====

        if (profile.getAtSymbolDensity() > 0.01) {
            return ClassificationResult.builder()
                    .isMatch(false)
                    .reason("Name cannot contain @ symbols: " + profile.getAtSymbolDensity())
                    .build();
        }

        // ADDED: High dictionary coverage indicates locations/countries, not personal names
        if (profile.getDictionaryCoverage() > 0.70) {
            return ClassificationResult.builder()
                    .isMatch(false)
                    .reason("High dictionary coverage indicates location, not personal name: " + profile.getDictionaryCoverage())
                    .build();
        }

        // ADDED: Low uniqueness indicates categories (countries, states), not personal names
        if (profile.getUniqueRatio() < 0.50) {
            return ClassificationResult.builder()
                    .isMatch(false)
                    .reason("Low uniqueness indicates categories/locations, not personal names: " + profile.getUniqueRatio())
                    .build();
        }

        if (profile.getLetterPercentage() < 0.70) {
            return ClassificationResult.builder()
                    .isMatch(false)
                    .reason("Letter percentage too low for name: " + profile.getLetterPercentage())
                    .build();
        }

        if (profile.getSymbolPercentage() > 0.15) {
            return ClassificationResult.builder()
                    .isMatch(false)
                    .reason("Symbol percentage too high for name: " + profile.getSymbolPercentage())
                    .build();
        }

        if (profile.getMeanLength() < 3.0) {
            return ClassificationResult.builder()
                    .isMatch(false)
                    .reason("Mean length too short for name: " + profile.getMeanLength())
                    .build();
        }

        if (profile.getShannonEntropy() > 4.8) {
            return ClassificationResult.builder()
                    .isMatch(false)
                    .reason("Entropy too high for name: " + profile.getShannonEntropy())
                    .build();
        }

        // ===== TIER 2: PATTERN COMPLIANCE =====
        if (profile.getTitleCaseRate() < 0.60) {
            return ClassificationResult.builder()
                    .isMatch(false)
                    .reason("Title case rate too low for name: " + profile.getTitleCaseRate())
                    .build();
        }

        // ===== TIER 3: STATISTICAL SCORING =====

        double patternScore = profile.getTitleCaseRate();

        double letterScore = profile.getLetterPercentage();

        double symbolScore = profile.getSymbolPercentage() < 0.001 ?
                1.0 :
                1.0 - Math.min(profile.getSymbolPercentage() * 6, 1.0);

        double wordCountScore;
        double avgWords = profile.getAverageWordCount();
        if (avgWords >= 1.0 && avgWords <= 3.5) {
            wordCountScore = 1.0;
        } else {
            wordCountScore = Math.max(1.0 - Math.abs(avgWords - 2.0) * 0.4, 0.0);
        }

        double lengthScore = Math.min(profile.getLengthStdDev() / 6.0, 1.0);

        double entropyScore;
        if (profile.getShannonEntropy() > 4.0) {
            entropyScore = profile.getTitleCaseRate() > 0.85 ? 0.8 : 0.5;
        } else {
            entropyScore = Math.max((4.0 - profile.getShannonEntropy()) / 4.0, 0.0);
        }

        double dictionaryBonus = profile.getDictionaryCoverage() > 0.40 ? 0.03 : 0.0;

        double weightedScore =
                (patternScore * 0.50) +
                        (letterScore * 0.25) +
                        (wordCountScore * 0.10) +
                        (symbolScore * 0.08) +
                        (lengthScore * 0.05) +
                        (entropyScore * 0.02) +
                        dictionaryBonus;

        if (profile.getLetterPercentage() > 0.95) weightedScore += 0.02;

        if (profile.getPatternOutlierPct() < 0.05) weightedScore += 0.02;

        weightedScore = Math.min(weightedScore, 1.0);

        return ClassificationResult.builder()
                .isMatch(true)
                .confidence(weightedScore)
                .category(CategoryType.NAME)
                .build();
    }

}
