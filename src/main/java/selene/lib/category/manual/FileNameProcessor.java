package selene.lib.category.manual;

import lombok.AllArgsConstructor;
import lombok.Builder;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileNameProcessor {


    private static final String LOADER_IDENTIFIER = "loader";
    private static final String TOTAL_ROW_COUNT_IDENTIFIER = "total_rows";
    private static final String ID_COLUMN_NAME_IDENTIFIER = "id_column_name";
    private static final String ELASTIC_INDEX_NAME_IDENTIFIER = "elastic_index_name";
    private static final String ELASTIC_SHARD_COUNT_IDENTIFIER = "elastic_shards";
    private static final String ELASTIC_REPLICA_COUNT_IDENTIFIER = "elastic_replicas";
    private static final String CASE_ID_IDENTIFIER = "case_id";
    private static final String DISPLAY_NAME_IDENTIFIER = "display_name";
    private static final String MD5_HASH_IDENTIFIER = "md5";
    private static final String SECTION_IDENTIFIER = "display_section";


    private static final Pattern META_DATA_PATTERN = Pattern.compile(
            "\\[([^\\]]+)\\]",
            Pattern.CASE_INSENSITIVE
    );


    public static FileMetaData getMetaData(String fileName){
        if(fileName == null || fileName.isBlank()){
            return FileMetaData.builder().build();
        }
        Matcher matcher = META_DATA_PATTERN.matcher(fileName);
        if(!matcher.find()){
            return FileMetaData.builder().fileName(fileName).build();
        }
        var builder = FileMetaData.builder();

        String metaDataContent = matcher.group(1);
        for(String pair : metaDataContent.split(",")){
            String trimmedPair = pair.trim();
            String[] keyValue = trimmedPair.split("=", 2);
            if(keyValue.length == 2){
                FileMetaData.setField(
                        builder,
                        keyValue[0].trim().toLowerCase(),
                        keyValue[1].trim());
            }
         }

        String acctualFilename = matcher.replaceFirst("");
        builder.fileName(acctualFilename);

        return builder.build();
    }


}
