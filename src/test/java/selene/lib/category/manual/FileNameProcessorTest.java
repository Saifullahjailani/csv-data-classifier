package selene.lib.category.manual;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class FileNameProcessorTest {

    @Test
    @DisplayName("No metadata brackets returns empty metadata with filename")
    void testNoMetadata() {
        FileMetaData meta = FileNameProcessor.getMetaData("simplefile.txt");

        assertEquals("simplefile.txt", meta.getFileName());
        assertNull(meta.getLoader());
        assertNull(meta.getTotalRows());
    }

    @Test
    @DisplayName("Complete metadata parses all fields correctly")
    void testCompleteMetadata() {
        String filename = "data[loader=csv,total_rows=1000,id_column_name=uuid,elastic_index_name=myindex,elastic_shards=3,elastic_replicas=1,case_id=CASE123,md5=abc123,display_name=My Data,display_section=main].txt";

        FileMetaData meta = FileNameProcessor.getMetaData(filename);

        assertEquals("data.txt", meta.getFileName());
        assertEquals("csv", meta.getLoader());
        assertEquals("1000", meta.getTotalRows());
        assertEquals("uuid", meta.getIdColumnName());
        assertEquals("myindex", meta.getElasticIndexName());
        assertEquals(3, meta.getNumberOfShards());
        assertEquals(1, meta.getNumberOfReplicas());
        assertEquals("CASE123", meta.getCaseId());
        assertEquals("abc123", meta.getMd5());
        assertEquals("My Data", meta.getDisplayName());
        assertEquals("main", meta.getSection());
    }

    @ParameterizedTest
    @DisplayName("Case insensitive key matching")
    @ValueSource(strings = {
            "data[LOADER=parquet,TOTAL_ROWS=500].txt",
            "data[Loader=parquet,total_rows=500].txt"
    })
    void testCaseInsensitiveKeys(String filename) {
        FileMetaData meta = FileNameProcessor.getMetaData(filename);

        assertEquals("data.txt", meta.getFileName());
        assertEquals("parquet", meta.getLoader());
        assertEquals("500", meta.getTotalRows());
    }

    @ParameterizedTest
    @DisplayName("Whitespace handling in metadata")
    @ValueSource(strings = {
            "data[ loader = csv , total_rows = 1000 ].txt",
            "data[loader=csv,total_rows=1000 ].txt",
            "data[ loader=csv ,total_rows=1000].txt"
    })
    void testWhitespaceHandling(String filename) {
        FileMetaData meta = FileNameProcessor.getMetaData(filename);

        assertEquals("csv", meta.getLoader());
        assertEquals("1000", meta.getTotalRows());
    }

    @Test
    @DisplayName("Multiple brackets - only first one processed")
    void testMultipleBrackets() {
        String filename = "data[loader=csv][ignored].txt";

        FileMetaData meta = FileNameProcessor.getMetaData(filename);

        assertEquals("data[ignored].txt", meta.getFileName());
        assertEquals("csv", meta.getLoader());
    }

    @ParameterizedTest
    @DisplayName("Invalid key-value pairs are ignored")
    @MethodSource("invalidPairsProvider")
    void testInvalidPairs(String filename) {
        FileMetaData meta = FileNameProcessor.getMetaData(filename);

        assertNotNull(meta.getLoader());
        assertEquals("csv", meta.getLoader()); // Only valid pair should be set
    }

    static Stream<Arguments> invalidPairsProvider() {
        return Stream.of(
                Arguments.of("data[loader=csv,invalid,missing_value=abc,bad=pair].txt"),
                Arguments.of("data[loader=csv,keywithoutvalue,=valuewithoutkey].txt")
        );
    }

    @Test
    @DisplayName("Filename extraction removes brackets correctly")
    void testFilenameExtraction() {
        String[] testCases = {
                "data[loader=csv].txt",
                "file_name[total_rows=100].csv",
                "test[multiple,fields,here].json"
        };

        for (String testCase : testCases) {
            FileMetaData meta = FileNameProcessor.getMetaData(testCase);
            String expectedFilename = testCase.replaceAll("\\[([^\\]]+)\\]", "");
            assertEquals(expectedFilename, meta.getFileName());
        }
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("Null and empty filenames handled gracefully")
    void testNullEmptyFilenames(String filename) {
        FileMetaData meta = FileNameProcessor.getMetaData(filename);
        if(filename == null || filename.isBlank()){
            assertNull(meta.getFileName(), "The meta should be null for filename null");
        }
        else {
            assertEquals(filename, meta.getFileName());
        }
    }

    @Test
    @DisplayName("Numeric fields parse correctly")
    void testNumericParsing() {
        FileMetaData meta = FileNameProcessor.getMetaData("data[elastic_shards=5,elastic_replicas=2,total_rows=abc].txt");

        assertEquals(5, meta.getNumberOfShards());
        assertEquals(2, meta.getNumberOfReplicas());
        assertEquals("abc", meta.getTotalRows()); // total_rows remains string
    }

    @Test
    @DisplayName("Unmatched brackets handled")
    void testUnmatchedBracket() {
        FileMetaData meta = FileNameProcessor.getMetaData("data[loader=csv.txt");

        assertEquals("data[loader=csv.txt", meta.getFileName());
        assertNull(meta.getLoader()); // No valid metadata extracted
    }
}
