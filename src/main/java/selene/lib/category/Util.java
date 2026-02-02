package selene.lib.category;

import ai.philterd.phileas.model.filtering.FilterType;
import selene.lib.category.crypto.CryptoType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Util {

    public static List<String> getAllValidCategories(){
        List<String> cateories = new ArrayList<>();
       cateories.addAll(Arrays.stream(FilterType.values()).map(FilterType::getType).toList());

        cateories.addAll(Arrays.stream(CryptoType.values()).map(CryptoType::getName).toList());
        cateories.addAll(Arrays.stream(CustomCategories.values()).map(CustomCategories::getType).toList());
        return cateories;
    }

    public static boolean isValidCategory(String str){
        for(var cat : getAllValidCategories()){
            if (str.equalsIgnoreCase(cat)){
                return true;
            }
        }
        return false;
    }
}
