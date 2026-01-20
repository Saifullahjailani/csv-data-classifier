package selene.lib.csv.types;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@AllArgsConstructor
public enum ElasticType {
    TEXT("text"),
    KEYWORD("keyword"),
    LONG("long"),
    INTEGER("integer"),
    DOUBLE("double"),
    BOOLEAN("boolean"),
    DATE("date"),
    BINARY("binary"),
    IP("ip"),
    NESTED("nested"),
    OBJECT("object"),
    GEO_POINT("geo_point"),
    UNDEFINED("undefined");

    private final String type;

}
