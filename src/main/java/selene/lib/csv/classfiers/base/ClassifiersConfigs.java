package selene.lib.csv.classfiers.base;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
public class ClassifiersConfigs {

    // Email classifier configuration
    public static final ClassifierConfig email = ClassifierConfig.builder()
            // === Scoring Weights (must sum to 1.0) ===
            .patternWeight(0.55)      // Dominant: regex pattern match
            .atSymbolWeight(0.23)     // @ symbol density
            .positionWeight(0.12)     // @ position consistency
            .symbolWeight(0.10)       // Overall symbol percentage
            .lengthWeight(0.05)       // Length variance
            .entropyWeight(0.05)      // Randomness check

            // === Gate Thresholds (hard disqualifiers) ===
            .minAtSymbolDensity(0.95)        // Must have @
            .maxAtSymbolDensity(1.05)        // Exactly one @
            .maxAtPositionCV(0.6)            // @ position can't vary too much
            .minSymbolPercentage(0.03)       // Must have some symbols (., @)
            .maxSymbolPercentage(0.30)       // But not too many
            .minPatternMatch(0.70)           // At least 70% regex match

            // Name-specific fields (not used but need defaults)
            .letterWeight(0.0)
            .wordCountWeight(0.0)
            .minLetterPercentage(0.0)
            .minMeanLength(0.0)
            .minTitleCaseRate(0.0)
            .minUniqueRatio(0.0)
            .maxDictionaryCoverage(1.0)
            .build();

    // Name classifier configuration
    public static final ClassifierConfig name = ClassifierConfig.builder()
            // === Scoring Weights (must sum to 1.0) ===
            .patternWeight(0.50)      // Title case rate
            .letterWeight(0.25)       // Letter percentage
            .symbolWeight(0.08)       // Symbol percentage (spaces, hyphens)
            .wordCountWeight(0.10)    // Average word count (1-3 words)
            .lengthWeight(0.05)       // Length variance
            .entropyWeight(0.02)      // Randomness check
            .atSymbolWeight(0.0)      // Not used
            .positionWeight(0.0)      // Not used

            // === Gate Thresholds (hard disqualifiers) ===
            .maxAtSymbolDensity(0.01)          // Cannot contain @
            .minLetterPercentage(0.70)         // Must be mostly letters
            .maxSymbolPercentage(0.15)         // Limited symbols (spaces/hyphens only)
            .minMeanLength(3.0)                // At least 3 chars average
            .maxEntropy(4.8)                   // Not too random
            .minTitleCaseRate(0.60)            // Must be Title Case
            .minUniqueRatio(0.50)              // Reject categories (countries/states)
            .maxDictionaryCoverage(0.70)       // Reject locations (cities/countries)
            .minPatternMatch(0.70)             // Title case compliance

            // Email-specific fields (not used but need defaults
            .minAtSymbolDensity(0.0)
            .maxAtSymbolDensity(999.0)
            .maxAtPositionCV(999.0)
            .minSymbolPercentage(0.0)
            .build();

    // Add SSN config
    public static final ClassifierConfig ssn = ClassifierConfig.builder()
            // === Gates ===
            .minDigitPercentage(0.70)       // High digit content
            .maxLengthStdDev(1.0)           // Allow 0 std dev for single values
            .minSymbolPercentage(0.00)      // ← FIX: Allow 0% symbols (unformatted)
            .maxSymbolPercentage(0.25)      // Not too many symbols
            .maxAtSymbolDensity(0.01)       // No @ symbols
            .minUniqueRatio(0.80)           // High uniqueness
            .maxEntropy(4.5)                // Moderate entropy

            // === Weights ===
            .patternWeight(0.45)
            .letterWeight(0.25)             // Reused for digit weight
            .symbolWeight(0.15)             // Symbol density
            .lengthWeight(0.10)             // Length consistency
            .wordCountWeight(0.10)          // Reused for uniqueness
            .entropyWeight(0.05)            // Randomness check

            // === Unused but needed ===
            .atSymbolWeight(0.0)
            .positionWeight(0.0)
            .minAtSymbolDensity(0.0)
            .maxAtSymbolDensity(999.0)
            .maxAtPositionCV(999.0)
            .build();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ClassifierConfig {
        // === Common Scoring Weights ===
        private double
                patternWeight,
                symbolWeight,
                lengthWeight,
                entropyWeight,
                atSymbolWeight,
                positionWeight,
                letterWeight,
                wordCountWeight,
                minPatternMatch;

        private double
                minAtSymbolDensity,
                maxAtSymbolDensity,
                maxAtPositionCV,
                minSymbolPercentage,
                maxSymbolPercentage,
                maxLengthStdDev;


        // === Name-Specific Gates ===
        private double
                minLetterPercentage,
                minMeanLength,
                maxEntropy,
                minTitleCaseRate,
                minUniqueRatio ,
                maxDictionaryCoverage,
                minDigitPercentage;

    }
}