import java.io.FileInputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public class ConfigFile {
    private List<Float> thresholds;
    private int dlLearnerRunLimitSeconds;
    private final Properties properties;
    private CurrencyFrequency currencyFrequency;
    private String testedCurrency;

    public enum CurrencyFrequency {
        MINUTES, HOURLY, DAILY
    }

    public ConfigFile() {
        thresholds = null;
        currencyFrequency = null;
        dlLearnerRunLimitSeconds = -1;
        testedCurrency = "";

        properties = new Properties();
        try {
            properties.load(new FileInputStream("./src/config.properties"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<Float> getThresholds() {
        if (thresholds == null) {
            initThresholds();
        }
        return thresholds;
    }

    public int getDlLearnerRunLimitSeconds() {
        dlLearnerRunLimitSeconds = Integer.parseInt(retrieveProperty("dl_run_seconds"));
        return dlLearnerRunLimitSeconds;
    }

    private void initThresholds() {
        String[] thresholdsStrings = retrieveProperty("thresholds").split(",");
        if (thresholdsStrings.length % 2 == 0) {
            try {
                throw new Exception("The number of thresholds has to be odd!");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        thresholds = Arrays.stream(thresholdsStrings).map(Float::parseFloat).sorted().collect(Collectors.toList());
    }

    public String getCurrencyFrequencyAsString() {
        if (currencyFrequency == null) {
            setCurrencyFrequency();
        }
        String returnValue = "";
        switch (currencyFrequency) {
            case MINUTES -> returnValue = "minutes";
            case HOURLY -> returnValue = "hourly";
            case DAILY -> returnValue = "daily";
        }
        return returnValue;
    }

    public CurrencyFrequency getCurrencyFrequency() {
        if (currencyFrequency == null) {
            setCurrencyFrequency();
        }
        return currencyFrequency;
    }

    private void setCurrencyFrequency() {
        switch (retrieveProperty("currency_frequency").toLowerCase()) {
            case "minutes" -> currencyFrequency = CurrencyFrequency.MINUTES;
            case "hourly" -> currencyFrequency = CurrencyFrequency.HOURLY;
            case "daily" -> currencyFrequency = CurrencyFrequency.DAILY;
            default -> {
                try {
                    throw new Exception("The currency frequency in the config file isn't a recognized value (please choose one of: 'minutes', 'hourly', 'daily')");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public String getTestedCurrency() {
        testedCurrency = retrieveProperty("tested_currency");
        return testedCurrency;
    }

    private String retrieveProperty(String propertyName) {
        String property = properties.getProperty(propertyName);
        if (property == null) {
            try {
                throw new Exception("Property " + propertyName + " is not defined in the properties config file.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return property;
    }
}
