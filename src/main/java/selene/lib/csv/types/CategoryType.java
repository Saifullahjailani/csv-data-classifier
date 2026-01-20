package selene.lib.csv.types;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import tech.tablesaw.api.StringColumn;

@Getter
@AllArgsConstructor
public enum CategoryType {
    // ==================== Core Identifiers (Most Specific) ====================
    SSN("Social Security Number",
            "^(?!000|666|9\\d{2})\\d{3}-?(?!00)\\d{2}-?(?!0000)\\d{4}$",
            "US SSN with validation", "123-45-6789"),

    TAX_ID("Tax ID/EIN",
            "^(0[1-9]|[1-8]\\d|9[0-8])-?\\d{7}$",
            "US Employer ID with valid prefix", "12-3456789"),

    PASSPORT("Passport Number",
            "^[A-Z0-9]{6,12}$",
            "International passport format", "A123456789"),

    DRIVERS_LICENSE("Driver's License",
            "^[A-Z0-9]{6,20}$",
            "US driver's license format", "D1234567"),

    // ==================== Numeric Identifiers (Specific Ranges) ====================
    AGE("Age",
            "^(?:[1-9]|[1-9][0-9]|1[01][0-9])$",
            "Age 1-119 (no leading zeros)", "25"),

    YEAR("Year",
            "^(?:19\\d{2}|20[0-2]\\d|2030)$",
            "Year 1900-2030", "2024"),

    MONTH("Month",
            "^(?:0?[1-9]|1[0-2])$",
            "Month 1-12", "12"),

    DAY("Day",
            "^(?:0?[1-9]|[12]\\d|3[01])$",
            "Day 1-31", "15"),

    POSTAL_CODE_US("US ZIP Code",
            "^\\d{5}(?:-\\d{4})?$",
            "US postal code (5 or 9 digits)", "12345-6789"),

    POSTAL_CODE_CA("Canada Postal",
            "^[A-Z]\\d[A-Z]\\s?\\d[A-Z]\\d$",
            "Canadian postal code", "K1A 0B1"),

    POSTAL_CODE_UK("UK Postcode",
            "^[A-Z]{1,2}\\d{1,2}[A-Z]?\\s?\\d[A-Z]{2}$",
            "United Kingdom postcode", "SW1A 0AA"),

    COORDINATE("Coordinate",
            "^-?(?:90|[0-8]?\\d)(?:\\.\\d+)?$|^-?(?:180|1[0-7]\\d|\\d{1,2})(?:\\.\\d+)?$",
            "Latitude or longitude coordinate", "40.7128"),

    // ==================== Financial (Strict Patterns) ====================
    CREDIT_CARD_VISA("Visa Card",
            "^4\\d{3}[-\\s]?\\d{4}[-\\s]?\\d{4}[-\\s]?(?:\\d{4})?$",
            "Visa card number", "4111111111111111"),

    CREDIT_CARD_MC("MasterCard",
            "^(?:5[1-5]|222[1-9]|22[3-9]\\d|2[3-6]\\d{2}|27[01]\\d|2720)\\d{2}[-\\s]?\\d{4}[-\\s]?\\d{4}[-\\s]?\\d{4}$",
            "MasterCard number", "5555555555554444"),

    CREDIT_CARD_AMEX("American Express",
            "^3[47]\\d{2}[-\\s]?\\d{6}[-\\s]?\\d{5}$",
            "American Express card", "378282246310005"),

    BANK_ROUTING("Bank Routing",
            "^\\d{9}$",
            "US routing number (exactly 9 digits)", "021000021"),

    IBAN("International Bank Account",
            "^[A-Z]{2}\\d{2}[a-zA-Z0-9]{1,30}$",
            "IBAN format", "GB82WEST12345698765432"),

    SWIFT_BIC("SWIFT/BIC Code",
            "^[A-Z]{6}[A-Z0-9]{2}([A-Z0-9]{3})?$",
            "Bank identifier code", "BARCGB22"),

    ISIN("ISIN Code",
            "^[A-Z]{2}[A-Z0-9]{9}\\d$",
            "International Securities ID", "US0378331005"),

    STOCK_TICKER("Stock Ticker",
            "^[A-Z]{1,5}(?:\\.[A-Z]{1,3})?$",
            "Stock symbol", "AAPL"),

    // ==================== Cryptocurrency (Strict) ====================
    BITCOIN_ADDRESS("Bitcoin Address",
            "^(?:1[a-km-zA-HJ-NP-Z1-9]{25,34}|3[a-km-zA-HJ-NP-Z1-9]{25,34}|bc1[ac-hj-np-zAC-HJ-NP-Z02-9]{8,87})$",
            "Bitcoin address", "1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa"),

    ETHEREUM_ADDRESS("Ethereum Address",
            "^0x[a-fA-F0-9]{40}$",
            "Ethereum address (exactly 40 hex chars)", "0x742d35Cc6634C0532925a3b8D4C0C1e9C7D4bF4B"),

    CRYPTO_TRANSACTION("Transaction Hash",
            "^0x[a-fA-F0-9]{64}$|^[a-fA-F0-9]{64}$",
            "Transaction hash (64 hex chars)", "0x5c504ed432cb51138bcf09aa5e8a410dd4a1e204ef84bfed1be16dfba1b22060"),

    // ==================== Contact Information ====================
    PHONE("Phone",
            "^(?:" +
                    // US formats (with optional country code)
                    "(?:\\+?1[-.\\s]?)?" +                          // Optional +1 prefix
                    "(?:\\(?[2-9]\\d{2}\\)?[-.\\s]?)?" +            // Optional area code
                    "[2-9]\\d{2}[-.\\s]?\\d{4}" +                   // Main number (3-4 digits)
                    "(?:\\s*(?:ext|x|#|extension)\\.?\\s*\\d{1,6})?" +  // Optional extension

                    "|" +

                    // International formats
                    "\\+(?:[1-9]\\d{0,2})" +                        // Country code (+1 to +999)
                    "(?:[-.\\s]?\\d{1,4}){1,5}" +                   // 1-5 groups of 1-4 digits
                    "(?:\\s*(?:ext|x|#|extension)\\.?\\s*\\d{1,6})?" +  // Optional extension

                    "|" +

                    // E.164 format (strict)
                    "\\+\\d{1,15}" +                                // Simple E.164 (+ followed by 1-15 digits)

                    "|" +

                    // Dot-separated format
                    "\\d{3}\\.\\d{3}\\.\\d{4}" +                    // 555.123.4567

                    "|" +

                    // No separators (10+ digits)
                    "\\d{10,}" +                                     // 10 or more digits
                    ")$",
            "Comprehensive phone number detection with extensions",
            "+1 (555) 123-4567 ext 123"
    ),

    EMAIL("Email Address",
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$",
            "Standard email format", "user@example.com"),

    URL("Web URL",
            "^https?://[\\w.-]+(?:/\\S*)?$",
            "URL with http/https", "https://example.com"),

    DOMAIN("Domain Name",
            "^[a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)*\\.[a-zA-Z]{2,}$",
            "Domain without protocol", "example.com"),

    NAME("Name",
            "^(?:" +
                    // Full names (First Last)
                    "[A-Z][a-z]{1,24}\\s+[A-Z][a-z]{1,24}" +

                    "|" +

                    // Names with hyphens, apostrophes, spaces
                    "[A-Z][a-z]{1,24}(?:[\\s\\'\\-][A-Z][a-z]{1,24}){1,3}" +

                    "|" +

                    // Single names (first or last)
                    "[A-Z][a-z]{1,29}" +

                    "|" +

                    // Names with middle initial
                    "[A-Z][a-z]{1,24}\\s+[A-Z]\\.?\\s*[A-Z][a-z]{1,24}" +
                    ")$",
            "Personal names including first, last, and full names",
            "John Smith"
    ),

    GENDER("Gender",
            "^(?i)(?:male|female|other|m|f|o|non.binary|transgender)$",
            "Gender identity", "Female"),

    DATE_ISO("Date ISO",
            "^\\d{4}-(?:0[1-9]|1[0-2])-(?:0[1-9]|[12]\\d|3[01])$",
            "YYYY-MM-DD format", "1990-01-01"),

    // ==================== Location (Specific) ====================
    STREET_ADDRESS("Street Address",
            "^\\d+\\s+[\\p{L}]+(?:\\s+[\\p{L}]+){1,6}(?:\\s+(?:Apt|Suite|Unit|#)\\s*\\d+)?$",
            "Street address line", "123 Main Street Apt 4B"),

    CITY_NAME("City",
            "^[\\p{L}]+(?:[\\s-][\\p{L}]+){0,2}$",
            "City name", "New York"),

    STATE_FULL("State Full",
            "^(?i)(?:Alabama|Alaska|Arizona|Arkansas|California|Colorado|Connecticut|Delaware|Florida|Georgia|Hawaii|Idaho|Illinois|Indiana|Iowa|Kansas|Kentucky|Louisiana|Maine|Maryland|Massachusetts|Michigan|Minnesota|Mississippi|Missouri|Montana|Nebraska|Nevada|New Hampshire|New Jersey|New Mexico|New York|North Carolina|North Dakota|Ohio|Oklahoma|Oregon|Pennsylvania|Rhode Island|South Carolina|South Dakota|Tennessee|Texas|Utah|Vermont|Virginia|Washington|West Virginia|Wisconsin|Wyoming)$",
            "US state full name", "California"),

    COUNTRY_NAME("Country",
            "^[\\p{L}]+(?:[\\s'.-][\\p{L}]+){0,3}$",
            "Country name", "United States"),

    // ==================== Employment (Specific) ====================
    EMPLOYEE_ID("Employee ID",
            "^[A-Z0-9-]{5,20}$",
            "Employee identifier", "EMP-12345"),

    SALARY_AMOUNT("Salary",
            "^\\$?\\d{1,3}(?:,\\d{3})*(?:\\.\\d{2})?$",
            "Salary amount", "$75,000.00"),

    JOB_TITLE("Job Title",
            "^[\\p{L}\\s'&,-]{3,80}$",
            "Position title", "Senior Software Engineer"),

    PROFESSION("Profession",
            "^(?i)(?:doctor|engineer|teacher|lawyer|nurse|dentist|pharmacist|software.developer|data.scientist|manager|consultant|analyst|designer|scientist)$",
            "Standardized profession", "Engineer"),

    // ==================== Web & Technical ====================
    IPV4_ADDRESS("IPv4 Address",
            "^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$",
            "IPv4 format", "192.168.1.1"),

    IPV6_ADDRESS("IPv6 Address",
            "^(?:[0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$",
            "IPv6 full format", "2001:0db8:85a3:0000:0000:8a2e:0370:7334"),

    MAC_ADDRESS("MAC Address",
            "^[0-9a-fA-F]{2}[:-][0-9a-fA-F]{2}[:-][0-9a-fA-F]{2}[:-][0-9a-fA-F]{2}[:-][0-9a-fA-F]{2}[:-][0-9a-fA-F]{2}$",
            "MAC address", "00:1B:44:11:3A:B7"),

    // ==================== Generic (Last Resort) ====================
    GENERIC_NUMBER("Generic Number",
            "^(?:[1-9]\\d*|0)$",
            "Any positive integer or zero", "12345"),

    GENERIC_STRING("Generic String",
            "^[\\p{L}0-9\\s'.,_-]+$",
            "Any alphanumeric string", "some text 123"),

    UNDEFINED("Undefined", "^.*$",
            "Catch-all for unknown types", "any value");

    private final String categoryName;
    private final String categoryRegex;
    private final String categoryDescription;
    private final String categoryExample;

    /**
     * Checks if the value matches this category
     */
    public boolean matches(String value) {
        if (value == null || value.isBlank()) return false;
        return value.matches(categoryRegex);
    }

    public long matchCount(StringColumn column) {
        if (column == null || column.isEmpty()) {
            return 0;
        }

        long count = 0;
        // Direct iteration is more memory-efficient than streaming for large datasets
        for (int i = 0; i < column.size(); i++) {
            if (!column.isMissing(i)) {
                String value = column.get(i);
                if (matches(value)) { // Reuse existing validation logic
                    count++;
                }
            }
        }
        return count;
    }



}