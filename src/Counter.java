import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

public class Counter {
    public static void main(String... args) {
        String[] fileNames = Tools.getOutputFileNamesList();
        for (String fileName : fileNames) {
            try {
                BufferedReader reader = new BufferedReader(new FileReader(fileName));
                String line = reader.readLine();
                Map<Integer, Integer> statistics = new TreeMap<>();
                while (line != null) {
                    int value = Integer.parseInt(line);
                    Integer count = statistics.get(value);
                    if (count == null) {
                        statistics.put(value, 1);
                    } else {
                        statistics.put(value, ++count);
                    }
                    line = reader.readLine();
                }
                System.out.println(fileName);
                for (Map.Entry<Integer, Integer> e : statistics.entrySet()) {
                    System.out.println(e.getKey() + ": " + e.getValue() + "x");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
