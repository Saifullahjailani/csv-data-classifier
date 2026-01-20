package selene.lib.csv.classfiers.base;

import selene.lib.csv.statistics.StatisticalProfile;
import tech.tablesaw.api.StringColumn;

public interface CategoryClassifier {
    ClassificationResult classify(StringColumn column, StatisticalProfile profile);
}
