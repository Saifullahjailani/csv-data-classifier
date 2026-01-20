package selene.lib;
import selene.lib.csv.classfiers.ClassifierFactory;
import selene.lib.csv.classfiers.base.CategoryClassifier;
import selene.lib.csv.statistics.Classifiers;
import lombok.extern.slf4j.Slf4j;
import selene.lib.csv.statistics.StatisticalProfile;
import selene.lib.csv.types.CategoryType;
import tech.tablesaw.api.ColumnType;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.csv.CsvReadOptions;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
@Slf4j
public class Main {
    static void main() {
        CsvReadOptions options = CsvReadOptions.builder("customers-100000.csv")
                .columnTypes(colName -> ColumnType.STRING)
                .build();

        Table table = Table.read().csv(options);

        StringColumn column = table.stringColumn("Email");
        StringColumn sample = column.sampleN(1000);
        StatisticalProfile profile = StatisticalProfile.getProfile(sample);

        CategoryClassifier nameClassifier=  ClassifierFactory.getClassifier(CategoryType.EMAIL);

        var d = nameClassifier.classify(sample, profile);
        System.out.println(d);

    }
}
