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
        ConfigFile configFile = new ConfigFile();
        List<Float> thresholds = configFile.getThresholds();
        ConfigFile.CurrencyFrequency currencyFrequency = configFile.getCurrencyFrequency();
        String currencyFrequencyString = configFile.getCurrencyFrequencyAsString();

        String fullDirName = "./resources/" + currencyFrequencyString;
        String[] filePathsList = Tools.currencyInputDataFileNames(fullDirName);

        List<SortedMap<String, Integer>> listOfCategories = new ArrayList<>();
        List<String> headerLine = new ArrayList<>();
        Set<String> lastCurrencyDates = new HashSet<>();

        for (String filePath : filePathsList) {
            File inputFile = new File(fullDirName + "/" + filePath);
            headerLine.add(filePath.substring(0, filePath.indexOf('_')));
            SortedMap<String, Float> records = getDifferenceValues(inputFile, currencyFrequency == ConfigFile.CurrencyFrequency.DAILY);
            SortedMap<String, Integer> currencyCategories = assignCategoriesToCurrencyRecords(records, thresholds, lastCurrencyDates);
            lastCurrencyDates = records.keySet();
            listOfCategories.add(currencyCategories);
        }
        deleteIncompleteDateRecords(listOfCategories);

        String outputPathName = Tools.OUTPUT_DIRECTORY + Tools.OUTPUT_FILE;
        mergeClassValuesIntoFile(listOfCategories, new File(outputPathName), headerLine);

        Tools.writeJenaConfig(headerLine);
        Tools.writeDLLearnerConfig(listOfCategories, configFile.getDlLearnerRunLimitSeconds());
    }

    private static void deleteIncompleteDateRecords(List<SortedMap<String, Integer>> listOfCategories) {
        if (listOfCategories.size() > 1) {
            for (int i = listOfCategories.size() - 1; i > 0; i--) {
                Set<String> moreDates = listOfCategories.get(i - 1).keySet();
                Set<String> lessDates = listOfCategories.get(i).keySet();
                moreDates.removeIf(date -> !lessDates.contains(date));
            }
        }
    }

    private static void mergeClassValuesIntoFile(List<SortedMap<String, Integer>> classesList, File file, List<String> headerLine) {
        BufferedWriter writer = null;
        CSVPrinter csvPrinter = null;
        try {
            Files.createDirectory(Paths.get(file.getPath()).getParent());
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

    private static SortedMap<String, Integer> assignCategoriesToCurrencyRecords(SortedMap<String, Float> records, List<Float> thresholds, Set<String> lastCurrencyDates) {

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
            if (lastCurrencyDates.isEmpty()) {
                categoryValues.put(categoryDate, categoryValue);
            } else {
                if (lastCurrencyDates.contains(categoryDate)) {
                    categoryValues.put(categoryDate, categoryValue);
                }
            }
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

    private static SortedMap<String, Float> getDifferenceValues(File file, boolean isDaily) {
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
                float open, close;
                String dateTimeKey;
                if (isDaily) {
                    open = valueAt(record, 1);
                    close = valueAt(record, 4);
                    dateTimeKey = record.get(0);
                } else {
                    open = valueAt(record, 2);
                    close = valueAt(record, 5);
                    dateTimeKey = record.get(0) + "T" + record.get(1);
                }
                float difference = close - open;
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
