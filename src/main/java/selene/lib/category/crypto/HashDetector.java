package selene.lib.category.crypto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;

public class HashDetector {

    private static double CONFIDENCE_THRESHOLD = 0.75;

    // Common hash lengths in characters
    private static final Map<Integer, String> HASH_TYPES = new HashMap<>();
    static {
        HASH_TYPES.put(32, "MD5");
        HASH_TYPES.put(40, "SHA-1 / RIPEMD-160");
        HASH_TYPES.put(56, "SHA-224");
        HASH_TYPES.put(64, "SHA-256 / Keccak-256 / Blake2s");
        HASH_TYPES.put(96, "SHA-384");
        HASH_TYPES.put(128, "SHA-512 / Blake2b");
        HASH_TYPES.put(34, "Bitcoin Address (P2PKH/P2SH)");
        HASH_TYPES.put(42, "Bitcoin Address (Bech32) | Ethereum Address");
        HASH_TYPES.put(62, "Bitcoin Address (Bech32m)");
        HASH_TYPES.put(66, "Ethereum Transaction/Block Hash");
    }

    @Getter
    @Setter
    @AllArgsConstructor
    public static class DetectionResult {
        private boolean isHash;
        private double confidence;
        private List<CryptoType> hashType;
    }

    @AllArgsConstructor
    @Getter
    @Setter
    public static class MultiDetectionResult{
        private final int numberOfHashes;
        private final Map<String, Integer> typeFrequency;

        public List<String> getTop(int n){
            return typeFrequency.entrySet()
                    .stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
                    .limit(n)
                    .map(Map.Entry::getKey)
                    .toList();
        }

    }

    public static MultiDetectionResult analyze(Iterable<String> values){
        int isHash = 0;
        Map<String, Integer> types = new HashMap<>();
        for(String value : values){
            value = value.trim();
            DetectionResult result = analyze(value);
            if(result.isHash && result.getConfidence() > CONFIDENCE_THRESHOLD){
                isHash++;
                for(var type : result.getHashType()){
                    var prev = types.getOrDefault(type.getName(), 0);
                    types.put(type.getName(), prev+1);
                }
            }
        }
        return new MultiDetectionResult(isHash, types);
    }
        /**
         * Advanced statistical analysis of string to determine if it's a hash
         */
        public static DetectionResult analyze(String input) {
            if (input == null || input.trim().isEmpty()) {
                return new DetectionResult(false, 0.0, List.of());
            }

            String trimmed = input.trim();
            int length = trimmed.length();

            // === PATTERN DETECTION ===
            boolean hasSpaces = trimmed.contains(" ");
            boolean hexOnly = Pattern.compile("^[a-fA-F0-9]+$").matcher(trimmed).matches();
            boolean hex0xPrefix = Pattern.compile("^0x[a-fA-F0-9]+$").matcher(trimmed).matches();
            boolean base58 = Pattern.compile("^[123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz]+$").matcher(trimmed).matches();
            boolean base64 = Pattern.compile("^[A-Za-z0-9+/]+=*$").matcher(trimmed).matches();
            boolean bech32 = Pattern.compile("^(bc1|tb1)[a-z0-9]+$").matcher(trimmed).matches();


            // === STATISTICAL ANALYSIS ===

            // 1. Entropy Analysis (Shannon Entropy)
            double entropy = calculateShannonEntropy(trimmed);

            // 2. Character Distribution (Chi-Square Test)
            double chiSquare = calculateChiSquare(trimmed);

            // 3. N-gram Analysis (Bigram Randomness)
            double bigramEntropy = calculateBigramEntropy(trimmed);

            // 4. Frequency Analysis
            double uniformity = calculateUniformity(trimmed);

            // 5. Serial Correlation
            double serialCorrelation = calculateSerialCorrelation(trimmed);

            // 6. Runs Test (Randomness)
            double runsTestZ = calculateRunsTest(trimmed);

            // 7. Vowel Ratio Analysis
            double vowelRatio = calculateVowelRatio(trimmed);

            // 8. Digit Ratio
            double digitRatio = calculateDigitRatio(trimmed);

            // 9. Case Mixing Entropy
            double caseMixEntropy = calculateCaseMixingEntropy(trimmed);

            // 10. Compression Ratio (Kolmogorov Complexity approximation)
            double compressionRatio = estimateCompressionRatio(trimmed);

            // === SCORING SYSTEM ===
            double hashScore = 0.0;

            // High entropy indicates randomness (hash-like)
            if (entropy > 3.5) {
                hashScore += 15.0;
            } else if (entropy < 2.5) {
                hashScore -= 10.0;
            }

            // Chi-square: lower values mean more uniform distribution (hash-like)
            if (chiSquare < 50.0 && !hasSpaces) {
                hashScore += 12.0;
            }

            // High bigram entropy = unpredictable character pairs (hash-like)
            if (bigramEntropy > 4.5) {
                hashScore += 13.0;
            } else if (bigramEntropy < 3.0) {
                hashScore -= 8.0;
            }

            // Uniformity close to 1.0 = even distribution (hash-like)
            if (uniformity > 0.85 && length > 20) {
                hashScore += 10.0;
            }

            // Low serial correlation = random (hash-like)
            if (Math.abs(serialCorrelation) < 0.1 && length > 20) {
                hashScore += 10.0;
            }

            // Runs test near 0 = random sequence
            if (Math.abs(runsTestZ) < 2.0 && length > 20) {
                hashScore += 8.0;
            }

            // Low vowel ratio in letter-only strings suggests hash
            if (vowelRatio < 0.3 && vowelRatio > 0.0 && !hasSpaces) {
                hashScore += 8.0;
            } else if (vowelRatio > 0.35 && vowelRatio < 0.45) {
                hashScore -= 12.0;
            }

            // High digit ratio suggests hash
            if (digitRatio > 0.3 && !hasSpaces) {
                hashScore += 10.0;
            }

            // High case mixing without spaces suggests hash
            if (caseMixEntropy > 0.8 && !hasSpaces && length > 20) {
                hashScore += 7.0;
            }

            // Low compression ratio = already random/compressed (hash-like)
            if (compressionRatio > 0.9 && length > 30) {
                hashScore += 10.0;
            }

            // === ENCODING AND FORMAT CHECKS ===
            if (hexOnly || hex0xPrefix) {
                hashScore += 20.0;
            }

            if (hex0xPrefix) {
                hashScore += 15.0;
            }

            if (base58 && length >= 26 && length <= 35) {
                hashScore += 18.0;
            }

            if (bech32) {
                hashScore += 25.0;
            }

            if (HASH_TYPES.containsKey(length) && (hexOnly || hex0xPrefix)) {
                hashScore += 15.0;
            }

            if (hasSpaces) {
                hashScore -= 30.0;
            }

            if (length < 10 && Pattern.compile("^[a-zA-Z]+$").matcher(trimmed).matches()) {
                hashScore -= 20.0;
            }

            // === BLOCKCHAIN SPECIFIC CHECKS ===
            if (hex0xPrefix && length == 66) {
                hashScore += 20.0;
            }

            if (hex0xPrefix && length == 42) {
                hashScore += 20.0;
            }

            if (trimmed.startsWith("1") || trimmed.startsWith("3") || trimmed.startsWith("bc1")) {
                if (base58 || bech32) {
                    hashScore += 15.0;
                }
            }

            // === FINAL DETERMINATION ===
            // Normalize score to 0-100 confidence scale
            double confidence = Math.min(100.0, Math.max(0.0, hashScore * 1.2)) / 100;
            boolean isHash = hashScore >= 25.0;

            // Identify hash type
            var hashType = identifyHashType(trimmed);

            return new DetectionResult(isHash, confidence, hashType);
        }

        // === STATISTICAL METHODS ===

        private static double calculateShannonEntropy(String str) {
            Map<Character, Integer> freq = new HashMap<>();
            for (char c : str.toCharArray()) {
                freq.put(c, freq.getOrDefault(c, 0) + 1);
            }

            double entropy = 0.0;
            int length = str.length();
            for (int count : freq.values()) {
                double p = (double) count / length;
                entropy -= p * (Math.log(p) / Math.log(2));
            }
            return entropy;
        }

        private static double calculateChiSquare(String str) {
            Map<Character, Integer> observed = new HashMap<>();
            for (char c : str.toCharArray()) {
                observed.put(c, observed.getOrDefault(c, 0) + 1);
            }

            double expected = (double) str.length() / observed.size();
            double chiSquare = 0.0;

            for (int count : observed.values()) {
                chiSquare += Math.pow(count - expected, 2) / expected;
            }
            return chiSquare;
        }

        private static double calculateBigramEntropy(String str) {
            if (str.length() < 2) return 0.0;

            Map<String, Integer> bigrams = new HashMap<>();
            int total = 0;

            for (int i = 0; i < str.length() - 1; i++) {
                String bigram = str.substring(i, i + 2);
                bigrams.put(bigram, bigrams.getOrDefault(bigram, 0) + 1);
                total++;
            }

            double entropy = 0.0;
            for (int count : bigrams.values()) {
                double p = (double) count / total;
                entropy -= p * (Math.log(p) / Math.log(2));
            }
            return entropy;
        }

        private static double calculateUniformity(String str) {
            Map<Character, Integer> freq = new HashMap<>();
            for (char c : str.toCharArray()) {
                freq.put(c, freq.getOrDefault(c, 0) + 1);
            }

            double mean = (double) str.length() / freq.size();
            double variance = 0.0;

            for (int count : freq.values()) {
                variance += Math.pow(count - mean, 2);
            }
            variance /= freq.size();

            double stdDev = Math.sqrt(variance);
            return 1.0 / (1.0 + stdDev / mean);
        }

        private static double calculateSerialCorrelation(String str) {
            if (str.length() < 2) return 0.0;

            double[] values = new double[str.length()];
            for (int i = 0; i < str.length(); i++) {
                values[i] = str.charAt(i);
            }

            double mean = 0.0;
            for (double v : values) mean += v;
            mean /= values.length;

            double covariance = 0.0;
            double variance = 0.0;

            for (int i = 0; i < values.length - 1; i++) {
                covariance += (values[i] - mean) * (values[i + 1] - mean);
                variance += Math.pow(values[i] - mean, 2);
            }

            return variance > 0 ? covariance / variance : 0.0;
        }

        private static double calculateRunsTest(String str) {
            if (str.length() < 2) return 0.0;

            // Convert to binary: above/below median
            double median = str.chars().average().orElse(0);
            int runs = 1;
            int n1 = 0, n2 = 0;
            boolean prevAbove = str.charAt(0) > median;

            if (prevAbove) n1++;
            else n2++;

            for (int i = 1; i < str.length(); i++) {
                boolean above = str.charAt(i) > median;
                if (above) n1++;
                else n2++;
                if (above != prevAbove) runs++;
                prevAbove = above;
            }

            // Calculate Z-score
            double expectedRuns = (2.0 * n1 * n2) / (n1 + n2) + 1;
            double variance = (2.0 * n1 * n2 * (2.0 * n1 * n2 - n1 - n2)) /
                    (Math.pow(n1 + n2, 2) * (n1 + n2 - 1));
            double stdDev = Math.sqrt(variance);

            return stdDev > 0 ? (runs - expectedRuns) / stdDev : 0.0;
        }

        private static double calculateVowelRatio(String str) {
            int vowels = 0;
            int letters = 0;
            for (char c : str.toLowerCase().toCharArray()) {
                if (Character.isLetter(c)) {
                    letters++;
                    if ("aeiou".indexOf(c) >= 0) vowels++;
                }
            }
            return letters > 0 ? (double) vowels / letters : 0.0;
        }

        private static double calculateDigitRatio(String str) {
            int digits = 0;
            for (char c : str.toCharArray()) {
                if (Character.isDigit(c)) digits++;
            }
            return (double) digits / str.length();
        }

        private static double calculateCaseMixingEntropy(String str) {
            int transitions = 0;
            for (int i = 0; i < str.length() - 1; i++) {
                boolean curr = Character.isUpperCase(str.charAt(i));
                boolean next = Character.isUpperCase(str.charAt(i + 1));
                if (curr != next) transitions++;
            }
            return (double) transitions / Math.max(1, str.length() - 1);
        }

        private static double estimateCompressionRatio(String str) {
            // Simple run-length encoding simulation
            int compressed = 0;
            int count = 1;
            for (int i = 0; i < str.length() - 1; i++) {
                if (str.charAt(i) == str.charAt(i + 1)) {
                    count++;
                } else {
                    compressed += count > 1 ? 2 : 1;
                    count = 1;
                }
            }
            compressed += count > 1 ? 2 : 1;
            return (double) compressed / str.length();
        }


        private static List<CryptoType> identifyHashType(String str) {

            var types = CryptoType.getTypes(str);

            if (!types.isEmpty()) {
                return types;
            }
            return List.of();
        }

    }
