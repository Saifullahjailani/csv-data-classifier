package selene.lib.category.type;

import lombok.Builder;
import lombok.Data;
import selene.lib.type.ElasticTypes;

import java.util.List;

@Builder
@Data
public class ClassificationResult {
    private final String columnName;
    private final List<String> categories;
    private final ElasticTypes type;
}
