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
    private int movingAverageUnits;

    public int getMovingAverageUnits() {
        try {
            movingAverageUnits = Integer.parseInt(retrieveProperty("moving_average"));
            if (movingAverageUnits < 0) {
                throw new Exception("The value of moving average has to be at least 0.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return movingAverageUnits;
    }

    public enum CurrencyFrequency {
        MINUTES, HOURLY, DAILY
    }

    public ConfigFile() {
        thresholds = null;
        currencyFrequency = null;
        dlLearnerRunLimitSeconds = -1;
        testedCurrency = "";
        movingAverageUnits = 0;

        properties = new Properties();
        try {
            properties.load(new FileInputStream("./src/config.properties"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<Float> getThresholds() throws Exception {
        if (thresholds == null) {
            initThresholds();
        }
        return thresholds;
    }

    public int getDlLearnerRunLimitSeconds() throws Exception {
        dlLearnerRunLimitSeconds = Integer.parseInt(retrieveProperty("dl_run_seconds"));
        return dlLearnerRunLimitSeconds;
    }

    private void initThresholds() throws Exception {
        String[] thresholdsStrings = retrieveProperty("thresholds").split(",");
        if (thresholdsStrings.length % 2 == 0) {
            throw new Exception("The number of thresholds has to be odd!");
        }
        thresholds = Arrays.stream(thresholdsStrings).map(Float::parseFloat).sorted().collect(Collectors.toList());
    }

    public String getCurrencyFrequencyAsString() throws Exception {
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

    public CurrencyFrequency getCurrencyFrequency() throws Exception {
        if (currencyFrequency == null) {
            setCurrencyFrequency();
        }
        return currencyFrequency;
    }

    private void setCurrencyFrequency() throws Exception {
        String currencyFrequency = retrieveProperty("currency_frequency").toLowerCase();
        switch (currencyFrequency) {
            case "minutes" -> this.currencyFrequency = CurrencyFrequency.MINUTES;
            case "hourly" -> this.currencyFrequency = CurrencyFrequency.HOURLY;
            case "daily" -> this.currencyFrequency = CurrencyFrequency.DAILY;
            default -> {
                throw new Exception("The currency frequency '" + currencyFrequency + "' in the config file isn't a recognized value (please choose one of: 'minutes', 'hourly', 'daily')");
            }
        }
    }

    public String getTestedCurrency() throws Exception {
        testedCurrency = retrieveProperty("tested_currency");
        return testedCurrency;
    }

    private String retrieveProperty(String propertyName) throws Exception {
        String property = properties.getProperty(propertyName);
        if (property == null) {
            throw new Exception("Property " + propertyName + " is not defined in the properties config file.");
        }
        return property;
    }
}
