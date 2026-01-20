package selene.lib.csv.classfiers;

import selene.lib.csv.classfiers.base.CategoryClassifier;
import selene.lib.csv.classfiers.base.ClassifiersConfigs;
import selene.lib.csv.types.CategoryType;

import java.util.HashMap;
import java.util.Map;


public class ClassifierFactory {
    private static final Map<String, CategoryClassifier> classifiers = new HashMap<>();
    public static CategoryClassifier getClassifier(CategoryType type) {
        return classifiers.computeIfAbsent(type.name(), k -> createClassifier(type));
    }

    private static CategoryClassifier createClassifier(CategoryType type) {
        switch (type) {
            case NAME: return new NameClassifier(ClassifiersConfigs.name);
            case EMAIL: return new EmailClassifier(ClassifiersConfigs.email);
            default: throw new IllegalArgumentException("Unknown type: " + type);
        }
    }

}
