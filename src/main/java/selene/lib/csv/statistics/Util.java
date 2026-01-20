package selene.lib.csv.statistics;

import tech.tablesaw.api.IntColumn;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public  class Util {

    public static double uniqueRatio(StringColumn column) {
        if (column == null || column.isEmpty()) return 0.0;
        return (double) column.countUnique() / column.size();
    }

    public static double valueFrequencyEntropy(StringColumn column) {
        if (column == null || column.isEmpty()) return 0.0;
        Table frequency = column.countByCategory();
        IntColumn counts = frequency.intColumn("Count");
        int totalValues = column.size();
        double entropy = 0.0;
        for (int count : counts) {
            double probability = (double) count / totalValues;
            if (probability > 0) {
                entropy -= probability * (Math.log(probability) / Math.log(2));
            }
        }
        return entropy;
    }

    public static double populationCoverage(StringColumn column) {
        if (column == null || column.isEmpty()) return 0.0;
        int totalRows = column.size();
        Set<String> distinctNumericValues = new HashSet<>();
        int nonNullCount = 0;
        int numericCount = 0;
        for (int i = 0; i < totalRows; i++) {
            String value = column.get(i);
            if (value == null || value.isEmpty()) continue;
            nonNullCount++;
            if (value.matches("-?\\d+(\\.\\d+)?")) {
                distinctNumericValues.add(value);
                numericCount++;
            }
        }
        if (nonNullCount == 0 || (double) numericCount / nonNullCount < 0.8) return 0.0;
        return (double) distinctNumericValues.size() / totalRows;
    }

    public static double duplicateDistributionSkew(StringColumn column) {
        if (column == null || column.isEmpty()) return 0.0;
        Table frequencyTable = column.countByCategory();
        IntColumn counts = frequencyTable.intColumn("Count");
        int n = counts.size();
        if (n <= 1) return 0.0;
        double sum = 0;
        for (int count : counts) sum += count;
        double mean = sum / n;
        if (mean == 0) return 0.0;
        double sumSquaredDiff = 0;
        for (int count : counts) {
            double diff = count - mean;
            sumSquaredDiff += diff * diff;
        }
        double variance = sumSquaredDiff / n;
        double stdDev = Math.sqrt(variance);
        return stdDev / mean;
    }

    public static double meanLength(StringColumn column) {
        if (column == null || column.isEmpty()) return 0.0;
        return column.length().mean();
    }

    public static double lengthStdDev(StringColumn column) {
        if (column == null || column.isEmpty()) return 0.0;
        return column.length().standardDeviation();
    }

    public static int lengthMode(StringColumn column) {
        if (column == null || column.isEmpty()) return 0;
        Map<Integer, Integer> lengthFreq = new HashMap<>();
        int maxCount = 0;
        int mode = 0;
        for (int i = 0; i < column.size(); i++) {
            String value = column.get(i);
            int len = value == null ? 0 : value.length();
            int count = lengthFreq.getOrDefault(len, 0) + 1;
            lengthFreq.put(len, count);
            if (count > maxCount) {
                maxCount = count;
                mode = len;
            }
        }
        return mode;
    }

    public static double lengthVarianceTrimmed(StringColumn column) {
        if (column == null || column.isEmpty()) return 0.0;
        List<Integer> lengths = new ArrayList<>();
        for (int i = 0; i < column.size(); i++) {
            String value = column.get(i);
            lengths.add(value == null ? 0 : value.length());
        }
        Collections.sort(lengths);
        int trimSize = (int) Math.ceil(lengths.size() * 0.05);
        if (lengths.size() <= 2 * trimSize) return 0.0;
        List<Integer> trimmed = lengths.subList(trimSize, lengths.size() - trimSize);
        double mean = trimmed.stream().mapToInt(Integer::intValue).average().orElse(0.0);
        double variance = trimmed.stream().mapToInt(Integer::intValue).mapToDouble(v -> Math.pow(v - mean, 2)).sum() / trimmed.size();
        return variance;
    }

    public static double nullEquivalentRate(StringColumn column) {
        if (column == null || column.isEmpty()) return 0.0;
        Set<String> nullEquivalents = new HashSet<>(Arrays.asList("", "N/A", "n/a", "null", "NULL", "Null", "None", "NA", "NaN", "-"));
        int count = 0;
        for (int i = 0; i < column.size(); i++) {
            String value = column.get(i);
            if (value == null || nullEquivalents.contains(value)) count++;
        }
        return (double) count / column.size();
    }

    public static double digitPercentage(StringColumn column) {
        if (column == null || column.isEmpty()) return 0.0;
        long totalChars = 0;
        long digitChars = 0;
        for (int i = 0; i < column.size(); i++) {
            String value = column.get(i);
            if (value == null) continue;
            totalChars += value.length();
            for (char c : value.toCharArray()) if (Character.isDigit(c)) digitChars++;
        }
        return totalChars == 0 ? 0.0 : (double) digitChars / totalChars;
    }

    public static double uppercasePercentage(StringColumn column) {
        if (column == null || column.isEmpty()) return 0.0;
        long totalChars = 0;
        long upperChars = 0;
        for (int i = 0; i < column.size(); i++) {
            String value = column.get(i);
            if (value == null) continue;
            totalChars += value.length();
            for (char c : value.toCharArray()) if (Character.isUpperCase(c)) upperChars++;
        }
        return totalChars == 0 ? 0.0 : (double) upperChars / totalChars;
    }

    public static double lowercasePercentage(StringColumn column) {
        if (column == null || column.isEmpty()) return 0.0;
        long totalChars = 0;
        long lowerChars = 0;
        for (int i = 0; i < column.size(); i++) {
            String value = column.get(i);
            if (value == null) continue;
            totalChars += value.length();
            for (char c : value.toCharArray()) if (Character.isLowerCase(c)) lowerChars++;
        }
        return totalChars == 0 ? 0.0 : (double) lowerChars / totalChars;
    }

    public static double symbolPercentage(StringColumn column) {
        if (column == null || column.isEmpty()) return 0.0;
        long totalChars = 0;
        long symbolChars = 0;
        for (int i = 0; i < column.size(); i++) {
            String value = column.get(i);
            if (value == null) continue;
            totalChars += value.length();
            for (char c : value.toCharArray()) {
                if (!(Character.isLetterOrDigit(c) || Character.isWhitespace(c))) symbolChars++;
            }
        }
        return totalChars == 0 ? 0.0 : (double) symbolChars / totalChars;
    }

    public static double hexCharacterPercentage(StringColumn column) {
        if (column == null || column.isEmpty()) return 0.0;
        long totalChars = 0;
        long hexChars = 0;
        for (int i = 0; i < column.size(); i++) {
            String value = column.get(i);
            if (value == null) continue;
            totalChars += value.length();
            for (char c : value.toCharArray()) {
                if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F')) hexChars++;
            }
        }
        return totalChars == 0 ? 0.0 : (double) hexChars / totalChars;
    }

    public static double whitespacePercentage(StringColumn column) {
        if (column == null || column.isEmpty()) return 0.0;
        long totalChars = 0;
        long wsChars = 0;
        for (int i = 0; i < column.size(); i++) {
            String value = column.get(i);
            if (value == null) continue;
            totalChars += value.length();
            for (char c : value.toCharArray()) if (Character.isWhitespace(c)) wsChars++;
        }
        return totalChars == 0 ? 0.0 : (double) wsChars / totalChars;
    }

    /**
     * Calculates coefficient of variation for @ symbol position
     * CV = stdDev / mean (relative dispersion, normalized for length)
     */
    public static double atPositionCoefficientOfVariation(StringColumn column) {
        double[] positions = column.asList().stream()
                .filter(v -> v != null && v.contains("@"))
                .mapToDouble(v -> v.indexOf('@'))
                .toArray();

        if (positions.length < 2) return 0.0;

        double mean = 0.0;
        for (double pos : positions) mean += pos;
        mean /= positions.length;

        double variance = 0.0;
        for (double pos : positions) {
            variance += Math.pow(pos - mean, 2);
        }
        variance /= positions.length;

        double stdDev = Math.sqrt(variance);

        // Coefficient of variation: low CV = consistent relative position
        return mean > 0 ? stdDev / mean : 0.0;
    }


    public static double atSymbolDensity(StringColumn column) {
        if (column == null || column.isEmpty()) return 0.0;
        double totalCount = 0;
        for (int i = 0; i < column.size(); i++) {
            String value = column.get(i);
            if (value == null) continue;
            for (char c : value.toCharArray()) if (c == '@') totalCount++;
        }
        return totalCount / column.size();
    }

    public static double dashSymbolDensity(StringColumn column) {
        if (column == null || column.isEmpty()) return 0.0;
        double totalCount = 0;
        for (int i = 0; i < column.size(); i++) {
            String value = column.get(i);
            if (value == null) continue;
            for (char c : value.toCharArray()) if (c == '-' || c == '_') totalCount++;
        }
        return totalCount / column.size();
    }

    public static double dotSymbolDensity(StringColumn column) {
        if (column == null || column.isEmpty()) return 0.0;
        double totalCount = 0;
        for (int i = 0; i < column.size(); i++) {
            String value = column.get(i);
            if (value == null) continue;
            for (char c : value.toCharArray()) if (c == '.') totalCount++;
        }
        return totalCount / column.size();
    }

    public static double symbolPositionEntropy(StringColumn column) {
        if (column == null || column.isEmpty()) return 0.0;
        int maxLen = 0;
        for (int i = 0; i < column.size(); i++) {
            String value = column.get(i);
            if (value != null) maxLen = Math.max(maxLen, value.length());
        }
        if (maxLen == 0) return 0.0;
        double[] entropies = new double[maxLen];
        for (int pos = 0; pos < maxLen; pos++) {
            Map<Character, Integer> charFreq = new HashMap<>();
            int total = 0;
            for (int i = 0; i < column.size(); i++) {
                String value = column.get(i);
                if (value != null && pos < value.length()) {
                    char c = value.charAt(pos);
                    charFreq.put(c, charFreq.getOrDefault(c, 0) + 1);
                    total++;
                }
            }
            double entropy = 0.0;
            if (total > 0) {
                for (int count : charFreq.values()) {
                    double p = (double) count / total;
                    entropy -= p * (Math.log(p) / Math.log(2));
                }
            }
            entropies[pos] = entropy;
        }
        return Arrays.stream(entropies).average().orElse(0.0);
    }

    public static double topPrefixFrequency(StringColumn column) {
        if (column == null || column.isEmpty()) return 0.0;
        Map<String, Integer> prefixFreq = new HashMap<>();
        for (int i = 0; i < column.size(); i++) {
            String value = column.get(i);
            if (value != null && value.length() >= 3) {
                String prefix = value.substring(0, 3);
                prefixFreq.put(prefix, prefixFreq.getOrDefault(prefix, 0) + 1);
            }
        }
        if (prefixFreq.isEmpty()) return 0.0;
        int maxCount = prefixFreq.values().stream().max(Integer::compare).orElse(0);
        return (double) maxCount / column.size();
    }

    public static double delimiterPositionVariance(StringColumn column) {
        if (column == null || column.isEmpty()) return 0.0;
        List<Integer> positions = new ArrayList<>();
        for (int i = 0; i < column.size(); i++) {
            String value = column.get(i);
            if (value == null) continue;
            for (int j = 0; j < value.length(); j++) {
                char c = value.charAt(j);
                if (c == '-' || c == '_' || c == '.' || c == '/' || c == ',' || c == ';' || c == '|') {
                    positions.add(j);
                }
            }
        }
        if (positions.isEmpty()) return 0.0;
        double mean = positions.stream().mapToInt(Integer::intValue).average().orElse(0.0);
        double variance = positions.stream().mapToInt(Integer::intValue).mapToDouble(p -> Math.pow(p - mean, 2)).sum() / positions.size();
        return variance;
    }

    public static double digitLetterTransitionCount(StringColumn column) {
        if (column == null || column.isEmpty()) return 0.0;
        double totalTransitions = 0;
        for (int i = 0; i < column.size(); i++) {
            String value = column.get(i);
            if (value == null || value.length() < 2) continue;
            int transitions = 0;
            for (int j = 1; j < value.length(); j++) {
                boolean prevDigit = Character.isDigit(value.charAt(j - 1));
                boolean currLetter = Character.isLetter(value.charAt(j));
                if (prevDigit && currLetter) transitions++;
            }
            totalTransitions += transitions;
        }
        return totalTransitions / column.size();
    }

    public static double shannonEntropy(StringColumn column) {
        if (column == null || column.isEmpty()) return 0.0;
        Map<Character, Integer> charFreq = new HashMap<>();
        int totalChars = 0;
        for (int i = 0; i < column.size(); i++) {
            String value = column.get(i);
            if (value == null) continue;
            for (char c : value.toCharArray()) {
                charFreq.put(c, charFreq.getOrDefault(c, 0) + 1);
                totalChars++;
            }
        }
        if (totalChars == 0) return 0.0;
        double entropy = 0.0;
        for (int count : charFreq.values()) {
            double p = (double) count / totalChars;
            entropy -= p * (Math.log(p) / Math.log(2));
        }
        return entropy;
    }

    public static double normalizedEntropy(StringColumn column) {
        if (column == null || column.isEmpty()) return 0.0;
        Set<Character> alphabet = new HashSet<>();
        int totalChars = 0;
        for (int i = 0; i < column.size(); i++) {
            String value = column.get(i);
            if (value == null) continue;
            for (char c : value.toCharArray()) {
                alphabet.add(c);
                totalChars++;
            }
        }
        if (totalChars == 0) return 0.0;
        double shannonEntropy = shannonEntropy(column);
        double maxEntropy = Math.log(alphabet.size()) / Math.log(2);
        return maxEntropy == 0 ? 0.0 : shannonEntropy / maxEntropy;
    }

    public static double ngramUniquenessRatio(StringColumn column) {
        if (column == null || column.isEmpty()) return 0.0;
        Set<String> uniqueNgrams = new HashSet<>();
        int totalNgrams = 0;
        for (int i = 0; i < column.size(); i++) {
            String value = column.get(i);
            if (value == null) continue;
            for (int j = 0; j < value.length() - 1; j++) {
                if (j < value.length() - 2) {
                    uniqueNgrams.add(value.substring(j, j + 3));
                    totalNgrams++;
                }
                uniqueNgrams.add(value.substring(j, j + 2));
                totalNgrams++;
            }
        }
        return totalNgrams == 0 ? 0.0 : (double) uniqueNgrams.size() / totalNgrams;
    }

    public static double serialCorrelation(StringColumn column) {
        if (column == null || column.isEmpty()) return 0.0;
        List<Double> chars = new ArrayList<>();
        for (int i = 0; i < column.size(); i++) {
            String value = column.get(i);
            if (value == null) continue;
            for (char c : value.toCharArray()) chars.add((double) c);
        }
        if (chars.size() < 2) return 0.0;
        double sum = 0;
        for (double d : chars) sum += d;
        double mean = sum / chars.size();
        double cov = 0;
        double var = 0;
        for (int i = 0; i < chars.size() - 1; i++) {
            cov += (chars.get(i) - mean) * (chars.get(i + 1) - mean);
            var += Math.pow(chars.get(i) - mean, 2);
        }
        return var == 0 ? 0.0 : cov / var;
    }

    public static double compressibilityRatio(StringColumn column) {
        if (column == null || column.isEmpty()) return 1.0;
        StringBuilder all = new StringBuilder();
        for (int i = 0; i < column.size(); i++) {
            String value = column.get(i);
            if (value != null) all.append(value);
        }
        byte[] data = all.toString().getBytes();
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            GZIPOutputStream gzip = new GZIPOutputStream(baos);
            gzip.write(data);
            gzip.close();
            double compressed = baos.toByteArray().length;
            return data.length / compressed;
        } catch (IOException e) {
            return 1.0;
        }
    }

    public static double strictPatternMatchPct(StringColumn column) {
        if (column == null || column.isEmpty()) return 0.0;
        String[] patterns = {"^[A-Z]{2,4}$", "^\\d{4}-\\d{2}-\\d{2}$", "^[a-f0-9]{32}$", "^[A-Z0-9]{6,12}$"};
        int totalMatches = 0;
        for (int i = 0; i < column.size(); i++) {
            String value = column.get(i);
            if (value == null) continue;
            for (String pattern : patterns) {
                if (value.matches(pattern)) {
                    totalMatches++;
                    break;
                }
            }
        }
        return (double) totalMatches / column.size();
    }

    public static double loosePatternMatchPct(StringColumn column) {
        if (column == null || column.isEmpty()) return 0.0;
        String[] patterns = {"^.*\\d.*$", "^.*[A-Z].*$", "^.*[a-z].*$", "^.*[-_.@].*$"};
        int totalMatches = 0;
        for (int i = 0; i < column.size(); i++) {
            String value = column.get(i);
            if (value == null) continue;
            int matchCount = 0;
            for (String pattern : patterns) {
                if (value.matches(pattern)) matchCount++;
            }
            if (matchCount >= 2) totalMatches++;
        }
        return (double) totalMatches / column.size();
    }

    public static double totalPatternCompliance(StringColumn column) {
        double strict = strictPatternMatchPct(column);
        double loose = loosePatternMatchPct(column);
        return (strict + loose * 0.5) / 1.5;
    }

    public static double patternConsistency(StringColumn column) {
        if (column == null || column.isEmpty()) return 0.0;
        List<Double> confidences = new ArrayList<>();
        String[] patterns = {"^\\d+$", "^[A-Z]+$", "^[a-z]+$", "^[A-Za-z]+$", "^[A-Za-z0-9]+$"};
        for (int i = 0; i < column.size(); i++) {
            String value = column.get(i);
            if (value == null) continue;
            int matches = 0;
            for (String pattern : patterns) if (value.matches(pattern)) matches++;
            confidences.add((double) matches / patterns.length);
        }
        if (confidences.isEmpty()) return 0.0;
        double mean = confidences.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = confidences.stream().mapToDouble(d -> Math.pow(d - mean, 2)).sum() / confidences.size();
        return Math.sqrt(variance);
    }

    public static double dictionaryCoverage(StringColumn column) {
        if (column == null || column.isEmpty()) return 0.0;
        Set<String> dictionary = new HashSet<>(Arrays.asList("the", "and", "for", "are", "but", "not", "with", "you", "this", "have"));
        int totalWords = 0;
        int matchedWords = 0;
        for (int i = 0; i < column.size(); i++) {
            String value = column.get(i);
            if (value == null) continue;
            String[] words = value.split("\\s+");
            for (String word : words) {
                if (!word.isEmpty()) {
                    totalWords++;
                    if (dictionary.contains(word.toLowerCase())) matchedWords++;
                }
            }
        }
        return totalWords == 0 ? 0.0 : (double) matchedWords / totalWords;
    }

    public static double phoneticRegularity(StringColumn column) {
        if (column == null || column.isEmpty()) return 0.0;
        int pronounceable = 0;
        for (int i = 0; i < column.size(); i++) {
            String value = column.get(i);
            if (value == null || value.isEmpty()) continue;
            boolean hasVowel = value.toLowerCase().matches(".*[aeiouy].*");
            boolean noTriple = !value.matches(".*[bcdfghjklmnpqrstvwxz]{3}.*");
            if (hasVowel && noTriple && value.length() >= 3) pronounceable++;
        }
        return (double) pronounceable / column.size();
    }

    public static int unicodeDiversity(StringColumn column) {
        if (column == null || column.isEmpty()) return 0;
        Set<Character.UnicodeBlock> blocks = new HashSet<>();
        for (int i = 0; i < column.size(); i++) {
            String value = column.get(i);
            if (value == null) continue;
            for (char c : value.toCharArray()) {
                blocks.add(Character.UnicodeBlock.of(c));
            }
        }
        return blocks.size();
    }

    public static double titleCaseRate(StringColumn column) {
        if (column == null || column.isEmpty()) return 0.0;
        int titleCaseCount = 0;
        String titleCasePattern = "^[A-Z][a-z]*(?:[\\s\\u00A0\\u2007\\u202F\\-'][A-Z][a-z]*)*$";
        for (int i = 0; i < column.size(); i++) {
            String value = column.get(i);
            if (value == null || value.isEmpty()) continue;
            boolean isTitleCase = value.matches(titleCasePattern);
            if (isTitleCase) titleCaseCount++;
        }
        return (double) titleCaseCount / column.size();
    }

    public static double averageWordCount(StringColumn column) {
        if (column == null || column.isEmpty()) return 0.0;
        int totalWords = 0;
        int nonNullCount = 0;
        for (int i = 0; i < column.size(); i++) {
            String value = column.get(i);
            if (value == null || value.isEmpty()) continue;
            nonNullCount++;
            totalWords += value.trim().split("\\s+").length;
        }
        return nonNullCount == 0 ? 0.0 : (double) totalWords / nonNullCount;
    }

    public static double lengthOutlierPct(StringColumn column) {
        if (column == null || column.isEmpty()) return 0.0;
        List<Integer> lengths = new ArrayList<>();
        for (int i = 0; i < column.size(); i++) {
            String value = column.get(i);
            lengths.add(value == null ? 0 : value.length());
        }
        double mean = lengths.stream().mapToInt(Integer::intValue).average().orElse(0.0);
        double stdDev = Math.sqrt(lengths.stream().mapToInt(Integer::intValue).mapToDouble(l -> Math.pow(l - mean, 2)).sum() / lengths.size());
        int outlierCount = 0;
        for (int len : lengths) if (Math.abs(len - mean) > 2 * stdDev) outlierCount++;
        return (double) outlierCount / column.size();
    }

    public static double patternOutlierPct(StringColumn column) {
        if (column == null || column.isEmpty()) return 0.0;
        String[] patterns = {"^\\d+$", "^[A-Za-z]+$", "^[A-Za-z0-9]+$", "^.*[.@_-].*$", "^[A-Z][a-z]+$"};
        int outlierCount = 0;
        for (int i = 0; i < column.size(); i++) {
            String value = column.get(i);
            if (value == null || value.isEmpty()) continue;
            boolean anyMatch = false;
            for (String pattern : patterns) {
                if (value.matches(pattern)) {
                    anyMatch = true;
                    break;
                }
            }
            if (!anyMatch) outlierCount++;
        }
        return (double) outlierCount / column.size();
    }

    public static double averageEditDistance(StringColumn column) {
        if (column == null || column.size() < 2) return 0.0;
        List<String> values = new ArrayList<>();
        for (int i = 0; i < column.size(); i++) {
            String value = column.get(i);
            if (value != null && !value.isEmpty()) values.add(value);
        }
        if (values.size() < 2) return 0.0;
        int totalPairs = 0;
        double totalDistance = 0;
        for (int i = 0; i < Math.min(values.size(), 10); i++) {
            for (int j = i + 1; j < Math.min(values.size(), 10); j++) {
                totalDistance += levenshtein(values.get(i), values.get(j));
                totalPairs++;
            }
        }
        return totalPairs == 0 ? 0.0 : totalDistance / totalPairs;
    }

    public static boolean hasNullMasking(StringColumn column) {
        if (column == null || column.isEmpty()) return false;
        List<Boolean> nullMask = new ArrayList<>();
        for (int i = 0; i < column.size(); i++) nullMask.add(column.get(i) == null || column.get(i).isEmpty());
        int runs = 0;
        for (int i = 1; i < nullMask.size(); i++) {
            if (!nullMask.get(i - 1) && nullMask.get(i)) runs++;
        }
        return runs > 5;
    }

    public static double longestCommonPrefix(StringColumn column) {
        if (column == null || column.isEmpty()) return 0.0;
        List<String> values = new ArrayList<>();
        for (int i = 0; i < column.size(); i++) {
            String value = column.get(i);
            if (value != null && !value.isEmpty()) values.add(value);
        }
        if (values.isEmpty()) return 0.0;
        String prefix = values.get(0);
        for (String s : values) {
            while (!s.startsWith(prefix)) prefix = prefix.substring(0, prefix.length() - 1);
        }
        return prefix.length();
    }

    public static double clusteringCoefficient(StringColumn column) {
        if (column == null || column.isEmpty()) return 0.0;
        List<String> values = new ArrayList<>();
        for (int i = 0; i < column.size(); i++) {
            String value = column.get(i);
            if (value != null && !value.isEmpty()) values.add(value);
        }
        if (values.size() < 3) return 0.0;
        int triangles = 0;
        int triplets = 0;
        for (int i = 0; i < Math.min(values.size(), 10); i++) {
            for (int j = i + 1; j < Math.min(values.size(), 10); j++) {
                for (int k = j + 1; k < Math.min(values.size(), 10); k++) {
                    double d1 = levenshtein(values.get(i), values.get(j));
                    double d2 = levenshtein(values.get(j), values.get(k));
                    double d3 = levenshtein(values.get(i), values.get(k));
                    if (d1 < 3 && d2 < 3 && d3 < 3) triangles++;
                    if (d1 < 3 && d2 < 3) triplets++;
                }
            }
        }
        return triplets == 0 ? 0.0 : (double) triangles / triplets;
    }

    public static boolean isSparse(StringColumn column) {
        if (column == null || column.isEmpty()) return true;
        int nullCount = 0;
        for (int i = 0; i < column.size(); i++) {
            String value = column.get(i);
            if (value == null || value.isEmpty()) nullCount++;
        }
        return (double) nullCount / column.size() > 0.5;
    }

    public static boolean isMixedType(StringColumn column) {
        if (column == null || column.isEmpty()) return false;
        int numeric = 0;
        int alphabetic = 0;
        int alphaNumeric = 0;
        int symbol = 0;
        for (int i = 0; i < column.size(); i++) {
            String value = column.get(i);
            if (value == null || value.isEmpty()) continue;
            if (value.matches("^\\d+$")) numeric++;
            else if (value.matches("^[A-Za-z]+$")) alphabetic++;
            else if (value.matches("^[A-Za-z0-9]+$")) alphaNumeric++;
            else symbol++;
        }
        int total = numeric + alphabetic + alphaNumeric + symbol;
        return total > 0 ? (double) Math.max(numeric, Math.max(alphabetic, Math.max(alphaNumeric, symbol))) / total < 0.8 : false;
    }

    public static boolean isTruncated(StringColumn column) {
        return false;
    }

    private static int levenshtein(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        for (int i = 0; i <= s1.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= s2.length(); j++) dp[0][j] = j;
        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
            }
        }
        return dp[s1.length()][s2.length()];
    }
}