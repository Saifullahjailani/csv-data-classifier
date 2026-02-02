package selene.lib.pii;

import ai.philterd.phileas.model.filtering.FilterType;
import selene.lib.category.pii.CsvDetect;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;

public class CsvDetectTest {

    @Test
    public void testCategorizeColumnEmails() {
        // Test data with emails
        StringColumn column = StringColumn.create("test",
                "john@example.com",
                "jane@company.org",
                "noemail",
                "",
                "alice@test.co.uk"
        );

        List<CsvDetect.CategoryScore> scores = CsvDetect.categorizeColumn(column);

        assertFalse(scores.isEmpty());
        assertTrue(scores.stream().anyMatch(s -> s.getType().name().contains("EMAIL")));
    }

    @Test
    public void testCategorizeColumnMixedPII() {
        StringColumn column = StringColumn.create("mixed",
                "Call 555-123-4567",
                "SSN: 123-45-6789",
                "0x4E5B2e1dc63F6b91cb6Cd759936495434C7e972F",
                "normal text",
                "credit: 4532015112830366"
        );

        List<CsvDetect.CategoryScore> scores = CsvDetect.categorizeColumn(column);

        // Should detect PHONE_NUMBER, SSN, HASH, CREDIT_CARD
        long piiCount = scores.stream()
                .filter(s -> !s.getType().name().equals("OTHER"))
                .count();
        assertTrue(piiCount >= 3, "Should detect multiple PII types");
    }

    @Test
    public void testCategorizeColumnEmpty() {
        StringColumn column = StringColumn.create("empty", "", " ", null, "");
        List<CsvDetect.CategoryScore> scores = CsvDetect.categorizeColumn(column);
        assertTrue(scores.isEmpty(), "Empty column should return empty list");
    }

    @Test
    public void testCategorizeTableMultipleColumns() {
        Table table = Table.create("test_table")
                .addColumns(
                        StringColumn.create("emails", "john@example.com", "jane@org.com"),
                        StringColumn.create("phones", "555-123-4567", "no phone"),
                        StringColumn.create("hashes", "0x123abc...", "normal")
                );

        Map<String, List<CsvDetect.CategoryScore>> results = CsvDetect.categorizeTable(table);

        assertEquals(3, results.size());
        assertTrue(results.containsKey("emails"));
        assertTrue(results.containsKey("phones"));
        assertTrue(results.containsKey("hashes"));

        // Emails column should have EMAIL_ADDRESS
        List<CsvDetect.CategoryScore> emailScores = results.get("emails");
        assertFalse(emailScores.isEmpty());
    }

    @Test
    public void testCategoryScoreGetters() {
        CsvDetect.CategoryScore score = new CsvDetect.CategoryScore(
                FilterType.AGE, 0.75
        );

        assertEquals(FilterType.AGE, score.getType());
        assertEquals(0.75, score.getRatio(), 0.001);
    }

    @Test
    public void testRealisticCsvScenario() {
        // Simulate CSV data with mixed PII
        StringColumn customerData = StringColumn.create("customer_data",
                "John Doe, john.doe@email.com, 555-987-6543",
                "Jane Smith, jane@work.com, DOB: 1985-03-15",
                "0x4E5B2e1dc63F6b91cb6Cd759936495434C7e972F",
                "Regular customer, no PII",
                "SSN 123-45-6789"
        );

        List<CsvDetect.CategoryScore> scores = CsvDetect.categorizeColumn(customerData);

        System.out.println("Realistic test results:");
        scores.forEach(s ->
                System.out.printf("  %s: %.1f%%%n", s.getType().name(), s.getRatio() * 100)
        );

        // Should find multiple PII types
        assertTrue(scores.size() >= 3);
    }

}
