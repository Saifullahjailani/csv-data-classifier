package selene.lib.category;

import lombok.AllArgsConstructor;
import lombok.Getter;
import selene.lib.type.ElasticTypes;

@AllArgsConstructor
@Getter
public enum CustomCategories {
    INDEX("index", ElasticTypes.INTEGER),
    JOB_TITLE("job-title", ElasticTypes.TEXT),
    GENDER("gender", ElasticTypes.KEYWORD),
    HASH("hash", ElasticTypes.KEYWORD),
    ID("id", ElasticTypes.KEYWORD);

    private final String type;
    private final ElasticTypes elasticTypes;

}
