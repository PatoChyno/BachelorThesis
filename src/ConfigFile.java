import java.io.FileInputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public class ConfigFile {
    private List<Float> thresholds;
    private int dlLearnerRunLimitSeconds;
    private final Properties properties;

    public ConfigFile() {
        thresholds = null;
        dlLearnerRunLimitSeconds = -1;

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
        return this.thresholds;
    }

    public int getDlLearnerRunLimitSeconds() {
        dlLearnerRunLimitSeconds = Integer.parseInt(properties.getProperty("dl_run_seconds"));
        return dlLearnerRunLimitSeconds;
    }

    private void initThresholds() {
        String[] thresholdsStrings = properties.getProperty("thresholds").split(",");
        if (thresholdsStrings.length % 2 == 0) {
            try {
                throw new Exception("The number of thresholds has to be odd!");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        thresholds = Arrays.stream(thresholdsStrings).map(Float::parseFloat).sorted().collect(Collectors.toList());
    }
}
