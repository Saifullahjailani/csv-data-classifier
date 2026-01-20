package selene.lib.csv.sampling;

import tech.tablesaw.api.StringColumn;

public class Sample {
    public static StringColumn Sample(StringColumn column, int n){
        if(column.size() < n){
            return column;
        }
        return column.sampleN(n);
    }
}
