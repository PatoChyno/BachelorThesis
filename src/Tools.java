import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class Tools {
    private static final String CONF_PREFIX = "kb";
    public static final String OUTPUT_DIRECTORY = "./generated_output/";
    public static final String OUTPUT_FILE = "out.csv";
    public static final String DLLEARNER_CONFIG_FILENAME = "fx0.conf";
    public static final String JENA_JSON_FILENAME = "fx_desc0.json";

    public static String[] currencyInputDataFileNames(String directoryParent) {
        File directory = new File(directoryParent);
        return directory.list();
    }

    private static JSONObject generateJenaJSON(List<String> currencies) {
        JSONObject json = new JSONObject();
        json.put("csvdir", "src/test/forex");

        JSONObject config = new JSONObject();
        config.put("uri", "http://uim.bp/forex");
        config.put("generateDataProperties", false);
        config.put("generateBoolAs", "subclass");
        config.put("generateEnumAs", "subclass");
        json.put("config", config);

        JSONArray data = new JSONArray();
        JSONObject d0 = new JSONObject();
        d0.put("dataType", "ENTITY");
        d0.put("dataSource", OUTPUT_FILE);
        d0.put("keyColumn", 0);
        d0.put("concept", "EVENT");

        JSONArray attributes = new JSONArray();
        for (int i = 0; i < currencies.size(); i++) {
            JSONObject obj = new JSONObject();
            obj.put("column", i + 1);
            obj.put("type", "ENUM");
            obj.put("property", currencies.get(i));
            attributes.put(obj);
        }

        d0.put("attributes", attributes);
        data.put(d0);
        json.put("data", data);

        return json;
    }

    public static void writeJenaConfig(List<String> headerLine) {
        String filename = OUTPUT_DIRECTORY + JENA_JSON_FILENAME;
        JSONObject json = Tools.generateJenaJSON(headerLine);
        writeConfigFile(json.toString(), filename);
    }

    private static void writeConfigFile(String content, String filename) {
        try {
            FileWriter writer = new FileWriter(filename);
            BufferedWriter buffer = new BufferedWriter(writer);
            buffer.write(content);
            buffer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void writeDLLearnerConfig(List<SortedMap<String, Integer>> listOfCategories, int dlLearnerRunLimitSeconds, String testedCurrency, List<String> headerLine, int thresholdsSize) {
        String filename = OUTPUT_DIRECTORY + DLLEARNER_CONFIG_FILENAME;
        Properties properties = generateDLLearnerConfig(listOfCategories, dlLearnerRunLimitSeconds, testedCurrency, headerLine, thresholdsSize);
        String content = stringifyProperties(properties);
        writeConfigFile(content, filename);
    }

    private static String stringifyProperties(Properties properties) {
        StringBuilder stringBuilder = new StringBuilder();
        for (Map.Entry<Object, Object> property : properties.entrySet()) {
            stringBuilder
                    .append(property.getKey())
                    .append(" = ");
            Object propertyValue = property.getValue();
            if (propertyValue instanceof Map<?, ?>) {
                Map<?, ?> map = (Map<?, ?>) property.getValue();
                stringBuilder.append("[ ");
                int i = 0;
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    stringBuilder
                            .append("(\"")
                            .append(entry.getKey())
                            .append("\", \"")
                            .append(entry.getValue())
                            .append("\")");
                    if (i < map.size() - 1) {
                        stringBuilder.append(",\n");
                    }
                    i++;
                }
                stringBuilder.append(" ]");
            } else if (propertyValue instanceof Set<?>) {
                Set<?> set = (Set<?>) property.getValue();
                stringBuilder.append("{");
                int i = 0;
                for (Object entry : set) {
                    if (property.getKey() == "reasoner.sources") {
                        stringBuilder.append((String) entry);
                    } else {
                        stringBuilder
                                .append("\"")
                                .append(CONF_PREFIX)
                                .append(":")
                                .append((String) entry)
                                .append("\"");
                        if (i < set.size() - 1) {
                            stringBuilder.append(",\n");
                        }
                    }
                    i++;
                }
                stringBuilder.append("}");
            } else if (propertyValue instanceof Integer) {
                stringBuilder.append(propertyValue);
            } else {
                stringBuilder
                        .append("\"")
                        .append(propertyValue)
                        .append("\"");
            }
            stringBuilder.append("\n");
        }
        return stringBuilder.toString();
    }

    private static Properties generateDLLearnerConfig(List<SortedMap<String, Integer>> listOfCategories, int dlLearnerRunLimitSeconds, String testedCurrency, List<String> headerLine, int thresholdsSize) {
        Properties properties = new Properties();

        Map<String, String> prefixes = new HashMap<>();
        prefixes.put(CONF_PREFIX, "http://uim.bp/forex#");
        properties.put("prefixes", prefixes);

        properties.put("ks.type", "OWL File");
        properties.put("ks.fileName", "result.ttl");
        properties.put("reasoner.type", "closed world reasoner");

        Set<String> sources = new HashSet<>();
        sources.add("ks");
        properties.put("reasoner.sources", sources);

        properties.put("lp.type", "posNegStandard");

        int testedCurrencyIndex = testedCurrencyIndex(headerLine, testedCurrency);

        Set<String> positiveExamples = positiveExamples(listOfCategories, testedCurrencyIndex);
        properties.put("lp.positiveExamples", positiveExamples);

        Set<String> negativeExamples = negativeExamples(listOfCategories, testedCurrencyIndex);
        properties.put("lp.negativeExamples", negativeExamples);

        properties.put("alg.type", "celoe");
        properties.put("alg.maxExecutionTimeInSeconds", dlLearnerRunLimitSeconds);

        Set<String> ignoredConcepts = new HashSet<>();
        for (int i = 0; i <= thresholdsSize + 1; i++) {
            ignoredConcepts.add("S_EVENT_" + testedCurrency + "_" + (i - ((thresholdsSize + 1) / 2)));
        }
        properties.put("alg.ignoredConcepts", ignoredConcepts);

        return properties;
    }

    private static Set<String> positiveExamples(List<SortedMap<String, Integer>> classesList, int testedCurrencyIndex) {
        return findExamples(classesList, true, testedCurrencyIndex);
    }

    private static Set<String> negativeExamples(List<SortedMap<String, Integer>> classesList, int testedCurrencyIndex) {
        return findExamples(classesList, false, testedCurrencyIndex);
    }

    private static Set<String> findExamples(List<SortedMap<String, Integer>> currencyCategories, boolean isPositive, int testedCurrencyIndex) {
        Set<String> examples = new HashSet<>();

        SortedMap<String, Integer> map = currencyCategories.get(testedCurrencyIndex);
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            if (isPositive) {
                if (entry.getValue() > 0) {
                    examples.add("EVENT_" + entry.getKey());
                }
            } else {
                if (entry.getValue() <= 0) {
                    examples.add("EVENT_" + entry.getKey());
                }
            }
        }
        return examples;
    }

    public static void addCurrencyToHeader(String filePath, List<String> headerLine) {
        headerLine.add(filePath.substring(0, filePath.indexOf('_')));
    }

    private static int testedCurrencyIndex(List<String> headerLine, String testedCurrency) {
        int index = 0;
        boolean found = false;
        while (index < headerLine.size() && !found) {
            if (headerLine.get(index).equals(testedCurrency)) {
                found = true;
            } else {
                index++;
            }
        }
        return index;
    }
}
