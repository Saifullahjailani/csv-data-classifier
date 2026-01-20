package selene.lib.csv.classifiers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import selene.lib.csv.classfiers.SSNClassifier;
import selene.lib.csv.classfiers.base.ClassificationResult;
import selene.lib.csv.classfiers.base.ClassifiersConfigs;
import selene.lib.csv.statistics.StatisticalProfile;
import selene.lib.csv.statistics.Util;
import selene.lib.csv.types.CategoryType;
import tech.tablesaw.api.StringColumn;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SSNClassifierTest {

    private SSNClassifier classifier;
    private ClassifiersConfigs.ClassifierConfig config;

    @BeforeEach
    void setUp() {
        config = ClassifiersConfigs.ssn; // Use the static SSN config
        classifier = new SSNClassifier(config);
    }

    @Test
    void testValidSSN_HighConfidence() {
        // Valid SSNs that should pass all gates
        List<String> ssnValues = Arrays.asList(
                "123-45-6789", "987-65-4321", "456-78-9012",
                "789-12-3456", "321-54-9876", "654-32-1098"
        );
        StringColumn column = StringColumn.create("ssn", ssnValues);

        StatisticalProfile profile = StatisticalProfile.getProfile(column);

        ClassificationResult result = classifier.classify(column, profile);

        assertTrue(result.isMatch(), "Should match SSN pattern");
        assertEquals(CategoryType.SSN, result.getCategory());
        assertTrue(result.getConfidence() > 0.85,
                "Valid SSN should have high confidence, got: " + result.getConfidence());
        assertNull(result.getReason()); // No failure reason on success
    }

    @Test
    void testInvalidSSN_FailsGate_TooManyLetters() {
        // Too many letters, not enough digits
        List<String> values = Arrays.asList("ABC-DEF-GHIJ", "XYZ-ABC-1234");
        StringColumn column = StringColumn.create("ssn", values);

        StatisticalProfile profile = StatisticalProfile.getProfile(column);

        ClassificationResult result = classifier.classify(column, profile);

        assertFalse(result.isMatch(), "Should fail digit percentage gate");
        assertNotNull(result.getReason());
        assertTrue(result.getReason().contains("Too few digits"));
    }

    @Test
    void testInvalidSSN_FailsGate_ContainsAtSymbol() {
        // Contains @ symbols (looks like email, not SSN)
        List<String> values = Arrays.asList("123-45-6789@example.com");
        StringColumn column = StringColumn.create("ssn", values);

        StatisticalProfile profile = StatisticalProfile.getProfile(column);
        ClassificationResult result = classifier.classify(column, profile);

        assertFalse(result.isMatch(), "Should fail @ symbol gate");
        assertNotNull(result.getReason());
        assertTrue(result.getReason().contains("@ symbols"));
    }

    @Test
    void testMixedFormatsSSN_ModerateConfidence() {
        // Mix of formatted (with dashes) and unformatted (without dashes)
        List<String> values = Arrays.asList(
                "123-45-6789", "987654321", "456-78-9012",
                "789-12-3456", "321-54-9876", "654321098"
        );
        StringColumn column = StringColumn.create("ssn", values);

        StatisticalProfile profile = StatisticalProfile.getProfile(column);

        ClassificationResult result = classifier.classify(column, profile);

        assertTrue(result.isMatch(), "Should still match despite format variation");
        assertTrue(result.getConfidence() > 0.60 && result.getConfidence() < 0.90,
                "Mixed formats should reduce confidence but still be acceptable. Got: " + result.getConfidence());
    }

    @Test
    void testSSN_WithNullsAndBlanks() {
        // Should handle nulls and empty strings gracefully
        List<String> values = Arrays.asList(
                "123-45-6789", null, "", "987-65-4321",
                "456-78-9012", null, "789-12-3456"
        );
        StringColumn column = StringColumn.create("ssn", values);

        StatisticalProfile profile = StatisticalProfile.getProfile(column);

        ClassificationResult result = classifier.classify(column, profile);

        // Should still work as nulls/blanks are excluded from calculations
        assertTrue(result.isMatch(), "Should handle nulls and blanks gracefully");
        assertTrue(result.getConfidence() > 0.80, "Should maintain high confidence. Got: " + result.getConfidence());
    }

    @Test
    void testSSN_DuplicateValues_FailsUniquenessGate() {
        // All values are the same - SSNs should be unique
        List<String> values = Collections.nCopies(6, "123-45-6789");
        StringColumn column = StringColumn.create("ssn", values);

        StatisticalProfile profile = StatisticalProfile.getProfile(column);

        ClassificationResult result = classifier.classify(column, profile);

        assertFalse(result.isMatch(), "Should fail uniqueness gate");
        assertNotNull(result.getReason());
        assertTrue(result.getReason().contains("Too many duplicates"));
    }

    @Test
    void testSSN_TooRandom_HighEntropy() {
        // Random characters, not structured SSN format
        List<String> values = Arrays.asList(
                "ABC-DEF-GHIJ", "JKL-MNO-PQRS", "TUV-WXY-Z123",
                "456-78-90ABC", "XYZ-12-3456", "789-ABC-1234"
        );
        StringColumn column = StringColumn.create("ssn", values);

        StatisticalProfile profile = StatisticalProfile.getProfile(column);

        ClassificationResult result = classifier.classify(column, profile);

        assertFalse(result.isMatch(), "Should fail entropy gate");
        assertNotNull(result.getReason());
        assertTrue(result.getReason().contains("Entropy"));
    }

    @Test
    void testSSN_BlankColumn() {
        StringColumn column = StringColumn.create("ssn", new String[0]);

        StatisticalProfile profile = StatisticalProfile.getProfile(column);

        ClassificationResult result = classifier.classify(column, profile);

        assertFalse(result.isMatch(), "Empty column should not match");
        assertNotNull(result.getReason());
    }


    @ParameterizedTest
    @CsvSource({
            "123-45-6789, true, 0.95",   // Valid formatted
            "987654321, true, 0.92",     // Valid unformatted
            "000-00-0000, true, 0.85",   // Valid format but placeholder
            "123-AB-CDEF, false, 0.0",   // Invalid letters in SSN
            "ABC-DEF-GHIJ, false, 0.0", // All letters
            "1234567890, false, 0.0"    // Too many digits
    })
    void testIndividualSSN_Validation(String ssn, boolean expectedMatch, double minExpectedConfidence) {
        StringColumn column = StringColumn.create("ssn", ssn);
        StatisticalProfile profile = StatisticalProfile.getProfile(column);

        ClassificationResult result = classifier.classify(column, profile);

        assertEquals(expectedMatch, result.isMatch(),
                "SSN '" + ssn + "' expected match: " + expectedMatch);

        if (expectedMatch) {
            assertTrue(result.getConfidence() >= minExpectedConfidence,
                    "SSN '" + ssn + "' should have confidence >= " + minExpectedConfidence +
                            ", got: " + result.getConfidence());
        } else {
            assertNotNull(result.getReason(), "Failed SSN should have failure reason");
        }
    }

    @Test
    void testSSN_PlaceholderValues_LowConfidence() {
        // Common placeholder/test SSNs that are technically valid format
        List<String> values = Arrays.asList(
                "000-00-0000", "111-11-1111", "999-99-9999",
                "123-45-6789", "987-65-4321"
        );
        StringColumn column = StringColumn.create("ssn", values);

        StatisticalProfile profile = StatisticalProfile.getProfile(column);

        ClassificationResult result = classifier.classify(column, profile);

        assertTrue(result.isMatch(), "Should match pattern");
        // Placeholders reduce confidence but should still pass
        assertTrue(result.getConfidence() > 0.60 && result.getConfidence() < 0.90,
                "Placeholders should reduce confidence. Got: " + result.getConfidence());
    }
}