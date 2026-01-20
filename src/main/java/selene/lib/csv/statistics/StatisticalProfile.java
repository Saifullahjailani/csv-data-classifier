package selene.lib.csv.statistics;

import lombok.Builder;
import lombok.Data;
import lombok.Value;
import tech.tablesaw.api.StringColumn;


@Value
@Builder
@Data
public class StatisticalProfile {

    /* ========== Cardinality & Distribution Metrics ========== */
    /** Ratio of unique values to total non-null values (0.0-1.0) */
    double uniqueRatio;

    /** Entropy of value frequency distribution (0.0-8.0) */
    double valueFrequencyEntropy;

    /** Ratio of distinct values seen vs. row count for numeric IDs (0.0-∞) */
    double populationCoverage;

    /** Coefficient of variation for duplicate frequencies (0.0-1.0) */
    double duplicateDistributionSkew;

    /* ========== Length & Structure Metrics ========== */
    /** Average string length of non-null values */
    double meanLength;

    /** Standard deviation of string lengths */
    double lengthStdDev;

    /** Most common string length */
    int lengthMode;

    /** Length variance after trimming top/bottom 5% outliers */
    double lengthVarianceTrimmed;

    /** Percentage of null-equivalent values ("", "N/A", "null") */
    double nullEquivalentRate;

    /* ========== Character Class Composition Metrics ========== */
    /** Percentage of digit characters (0-9) */
    double digitPercentage;

    /** Percentage of uppercase letters (A-Z) */
    double uppercasePercentage;

    /** Percentage of lowercase letters (a-z) */
    double lowercasePercentage;

    /** Percentage of symbol characters (!@#$%^&* etc.) */
    double symbolPercentage;

    /** Percentage of hex characters (a-fA-F0-9) */
    double hexCharacterPercentage;

    /** Percentage of whitespace characters */
    double whitespacePercentage;

    /** Average count of @ symbols per value */
    double atSymbolDensity;

    /** Average count of dash/hyphen symbols per value */
    double dashSymbolDensity;

    /** Average count of dot/period symbols per value */
    double dotSymbolDensity;

    /* ========== Positional Pattern Metrics ========== */
    /** Average entropy of characters at each position (0.0-6.0) */
    double symbolPositionEntropy;

    /** Frequency of the most common 3-character prefix */
    double topPrefixFrequency;

    /** Standard deviation of delimiter positions (0.0-∞) */
    double delimiterPositionVariance;

    /** Average number of digit-to-letter transitions per value */
    double digitLetterTransitionCount;

    /* ========== Entropy & Randomness Metrics ========== */
    /** Shannon entropy of character distribution (0.0-6.0) */
    double shannonEntropy;

    /** Shannon entropy normalized by alphabet size (0.0-1.0) */
    double normalizedEntropy;

    /** Percentage of unique bigrams/trigrams (0.0-1.0) */
    double ngramUniquenessRatio;

    /** Serial correlation between consecutive characters (-1.0 to 1.0) */
    double serialCorrelation;

    /** Compressibility ratio (uncompressed/compressed length) (1.0-∞) */
    double compressibilityRatio;

    /* ========== Pattern Compliance Metrics ========== */
    /** Percentage matching strict regex pattern */
    double strictPatternMatchPct;

    /** Percentage matching loose/variant regex pattern */
    double loosePatternMatchPct;

    /** Composite pattern compliance score (0.0-1.0) */
    double totalPatternCompliance;

    /** Standard deviation of pattern match confidence */
    double patternConsistency;

    /* ========== Semantic & Linguistic Metrics ========== */
    /** Percentage of words found in name/place dictionaries */
    double dictionaryCoverage;

    /** Percentage pronounceable via phonetic algorithm */
    double phoneticRegularity;

    /** Number of distinct Unicode blocks used */
    int unicodeDiversity;

    /** Percentage of values in Title Case */
    double titleCaseRate;

    /** Average number of space-separated tokens */
    double averageWordCount;

    /* ========== Outlier & Anomaly Metrics ========== */
    /** Percentage of values >2σ from mean length */
    double lengthOutlierPct;

    /** Percentage failing all known patterns */
    double patternOutlierPct;

    /** Average Levenshtein distance between random value pairs */
    double averageEditDistance;

    /** Whether null masking reveals hidden patterns (true/false) */
    boolean hasNullMasking;

    /* ========== Cross-Value Similarity Metrics ========== */
    /** Average length of longest common prefix */
    double longestCommonPrefix;

    /** Clustering coefficient for value similarity (0.0-1.0) */
    double clusteringCoefficient;

    /* ========== Quality Flags ========== */
    /** True if column has high null rate (>50%) */
    boolean isSparse;

    /** True if mixed data types detected */
    boolean isMixedType;

    /** True if analysis truncated due to memory constraints */
    boolean isTruncated;

    double atPositionCoefficientOfVariation;

    double letterPercentage;
    public static StatisticalProfile getProfile(StringColumn column) {
        return StatisticalProfile.builder()
                .letterPercentage(Util.lowercasePercentage(column) + Util.uppercasePercentage(column))
                .uniqueRatio(Util.uniqueRatio(column))
                .valueFrequencyEntropy(Util.valueFrequencyEntropy(column))
                .populationCoverage(Util.populationCoverage(column))
                .duplicateDistributionSkew(Util.duplicateDistributionSkew(column))
                .meanLength(Util.meanLength(column))
                .lengthStdDev(Util.lengthStdDev(column))
                .atPositionCoefficientOfVariation(Util.atPositionCoefficientOfVariation(column))
                .lengthMode(Util.lengthMode(column))
                .lengthVarianceTrimmed(Util.lengthVarianceTrimmed(column))
                .nullEquivalentRate(Util.nullEquivalentRate(column))
                .digitPercentage(Util.digitPercentage(column))
                .uppercasePercentage(Util.uppercasePercentage(column))
                .lowercasePercentage(Util.lowercasePercentage(column))
                .symbolPercentage(Util.symbolPercentage(column))
                .hexCharacterPercentage(Util.hexCharacterPercentage(column))
                .whitespacePercentage(Util.whitespacePercentage(column))
                .atSymbolDensity(Util.atSymbolDensity(column))
                .dashSymbolDensity(Util.dashSymbolDensity(column))
                .dotSymbolDensity(Util.dotSymbolDensity(column))
                .symbolPositionEntropy(Util.symbolPositionEntropy(column))
                .topPrefixFrequency(Util.topPrefixFrequency(column))
                .delimiterPositionVariance(Util.delimiterPositionVariance(column))
                .digitLetterTransitionCount(Util.digitLetterTransitionCount(column))
                .shannonEntropy(Util.shannonEntropy(column))
                .normalizedEntropy(Util.normalizedEntropy(column))
                .ngramUniquenessRatio(Util.ngramUniquenessRatio(column))
                .serialCorrelation(Util.serialCorrelation(column))
                .compressibilityRatio(Util.compressibilityRatio(column))
                .strictPatternMatchPct(Util.strictPatternMatchPct(column))
                .loosePatternMatchPct(Util.loosePatternMatchPct(column))
                .totalPatternCompliance(Util.totalPatternCompliance(column))
                .patternConsistency(Util.patternConsistency(column))
                .dictionaryCoverage(Util.dictionaryCoverage(column))
                .phoneticRegularity(Util.phoneticRegularity(column))
                .unicodeDiversity(Util.unicodeDiversity(column))
                .titleCaseRate(Util.titleCaseRate(column))
                .averageWordCount(Util.averageWordCount(column))
                .lengthOutlierPct(Util.lengthOutlierPct(column))
                .patternOutlierPct(Util.patternOutlierPct(column))
                .averageEditDistance(Util.averageEditDistance(column))
                .hasNullMasking(Util.hasNullMasking(column))
                .longestCommonPrefix(Util.longestCommonPrefix(column))
                .clusteringCoefficient(Util.clusteringCoefficient(column))
                .isSparse(Util.isSparse(column))
                .isMixedType(Util.isMixedType(column))
                .isTruncated(Util.isTruncated(column))
                .build();
    }
}