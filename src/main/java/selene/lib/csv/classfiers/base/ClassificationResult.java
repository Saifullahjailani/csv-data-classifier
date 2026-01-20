package selene.lib.csv.classfiers.base;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import selene.lib.csv.types.CategoryType;

@Data
@Builder
@AllArgsConstructor
public class ClassificationResult {
    private final boolean isMatch;
    private final CategoryType category;
    private final double confidence;
    private final String reason;
}