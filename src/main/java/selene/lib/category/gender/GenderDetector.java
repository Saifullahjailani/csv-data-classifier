package selene.lib.category.gender;

import java.util.regex.Pattern;

public class GenderDetector {
    private static final Pattern MALE_PATTERN = Pattern.compile("(?i)^(mr|mister|male|man|boy|he|him|m)$");
    private static final Pattern FEMALE_PATTERN = Pattern.compile("(?i)^(mrs|ms|miss|female|woman|girl|she|her|f)$");
    private static final Pattern NON_BINARY_PATTERN = Pattern.compile("(?i)^(nonbinary|non-binary|nb|they|them|enby|xem)$");


    public static boolean isGenderIdentifier(String string) {
        if (string == null || string.trim().isEmpty()) {
            return false;
        }

        String cleanString = string.trim();

        return MALE_PATTERN.matcher(cleanString).matches() ||
                FEMALE_PATTERN.matcher(cleanString).matches() ||
                NON_BINARY_PATTERN.matcher(cleanString).matches()
                ;
    }

    public static int genderCount(Iterable<String> strings){
        int gender = 0;

        for(var string : strings){
            if(isGenderIdentifier(string)){
                gender++;
            }
        }
        return gender;
    }
}
