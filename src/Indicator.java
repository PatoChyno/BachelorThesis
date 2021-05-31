import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class Indicator {
    public static void main(String... args) {
        String[] filePathsList = Tools.getInputFilePathsList();
        String outputPathName = "./out/production/Indicators/out.csv";
        List<SortedMap<String, Integer>> classesList = new ArrayList<>();
        List<String> headerLine = new ArrayList<>();
        ConfigFile configFile = new ConfigFile();
        List<Float> thresholds = configFile.getThresholds();

        for (String filePath : filePathsList) {
            File inputFile = new File("./resources/" + filePath);
            headerLine.add(filePath.substring(0, filePath.indexOf('_')));
            SortedMap<String, Float> records = getDifferenceValues(inputFile);
            SortedMap<String, Integer> classValues = assignCategoriesToRecords(records, thresholds);
            classesList.add(classValues);
        }
        mergeClassValuesIntoFile(classesList, new File(outputPathName), headerLine);

        Tools.writeJenaConfig(headerLine);
        Tools.writeDLLearnerConfig(classesList, configFile.getDlLearnerRunLimitSeconds());
    }

    private static void mergeClassValuesIntoFile(List<SortedMap<String, Integer>> classesList, File file, List<String> headerLine) {
        BufferedWriter writer = null;
        CSVPrinter csvPrinter = null;
        try {
            writer = Files.newBufferedWriter(Paths.get(file.getPath()));
            csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT);
            csvPrinter.print("[DATETIME_KEY]");
            for (String caption : headerLine) {
                csvPrinter.print(caption);
            }
            csvPrinter.printRecord();
            for (String dateTimeKey : classesList.get(0).keySet()) {
                csvPrinter.print(dateTimeKey);
                for (SortedMap<String, Integer> classes : classesList) {
                    csvPrinter.print(classes.get(dateTimeKey));
                }
                csvPrinter.printRecord();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (csvPrinter != null) {
                    csvPrinter.flush();
                }
                if (writer != null) {
                    writer.flush();
                    writer.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static SortedMap<String, Integer> assignCategoriesToRecords(SortedMap<String, Float> records, List<Float> thresholds) {

//        Queue<Float> last10 = new LinkedList<>();

//        final int SAMPLE_SIZE = 10;
//        float currentSampleSum = 0;
        float lowest = Collections.min(records.values());
        float highest = Collections.max(records.values());
        float average = records.values().stream().reduce(0f, Float::sum) / records.size();
        float extremesDifference = highest - lowest;

        SortedMap<String, Integer> categoryValues = new TreeMap<>();
        for (Map.Entry<String, Float> currentRecord : records.entrySet()) {
            /*
            Float previousRecord = 0f;
            last10.add(currentRecord);
            if (last10.size() > SAMPLE_SIZE) {
                previousRecord = last10.remove();
            }
             */

            //float newAverage = calcNewAverage(currentSampleSum, currentRecord, previousRecord, last10.size());
            //currentSampleSum += currentRecord - previousRecord;

            //int hasAverageIncreased = Float.compare(newAverage, currentSampleSum);
            //int categoryValue = findClosestCategoryValue(thresholds, currentRecord.getValue(), average, extremesDifference);
            int categoryValue = findCategoryValue(thresholds, currentRecord.getValue(), average, extremesDifference);
            String categoryDate = currentRecord.getKey();
            categoryValues.put(categoryDate, categoryValue);
        }

        return categoryValues;
    }

    private static int findCategoryValue(List<Float> thresholds, float currentRecord, float average, float extremesDifference) {
        float currentToAverageRecordRatio = (currentRecord - average) / extremesDifference;
        if (currentToAverageRecordRatio == 0) {
            return 0;
        }

        int thresholdIndex = 0;
        boolean thresholdCrossed = false;
        while (thresholdIndex < thresholds.size() && !thresholdCrossed) {
            if (currentToAverageRecordRatio < thresholds.get(thresholdIndex)) {
                thresholdCrossed = true;
            } else {
                thresholdIndex++;
            }
        }
        thresholdIndex -= ((thresholds.size() + 1) / 2);
        if (thresholdIndex >= 0) {
            thresholdIndex++;
        }
        return thresholdIndex;
    }

    /*
    private static float calcNewAverage(float currentSum, float newElement, float oldElement, int elementCount) {
        return (currentSum + newElement - oldElement) / elementCount;
    }
     */

    private static SortedMap<String, Float> getDifferenceValues(File file) {
        SortedMap<String, Float> records = new TreeMap<>();
        try {
            CSVParser parser = CSVFormat.TDF.parse(new InputStreamReader(new FileInputStream(file)));
            boolean isFirstLine = true;
            for (CSVRecord record : parser.getRecords()) {
                // skip header
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }
                float open = valueAt(record, 2);
                float close = valueAt(record, 5);
                float difference = close - open;
                String dateTimeKey = record.get(0) + "T" + record.get(1);
                records.put(dateTimeKey, difference);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return records;
    }

    public static float valueAt(CSVRecord record, int index) {
        return Float.parseFloat(record.get(index));
    }
}
