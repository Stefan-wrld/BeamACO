package BeamAco;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class TSPInstanceLoader {
    private static double[][] costMatrix;
    private static List<Double> startTimes;
    private static List<Double> endTimes;
    private static int numberNodes;

    public static void loadInstance(String fileName) {
        List<List<String>> records = new ArrayList<>();
        startTimes = new LinkedList<>();
        endTimes = new LinkedList<>();
        int index = 0;
        int n = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split("\\s+");
                if (index == 0) {
                    n = Integer.parseInt(values[0]);
                }
                if(index > 0 && index <= n){
                    records.add(Arrays.asList(values));
                }
                if(index > n && index <= 2 * n ) {
                    double startTime = Double.parseDouble(values[0]);
                    double endTime = Double.parseDouble(values[1]);
                    startTimes.add(startTime);
                    endTimes.add(endTime);
                }
                index++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        costMatrix = GraphGenerator.toDoubleMatrix(records);
        numberNodes = costMatrix.length;
    }

    public static double[][] getCostMatrix() {
        return costMatrix;
    }

    public static double[] getStartTimes() {
        return startTimes.stream().mapToDouble(Double::doubleValue).toArray();
    }

    public static double[] getEndTimes() {
        return endTimes.stream().mapToDouble(Double::doubleValue).toArray();
    }

    public static int getNumberNodes() {
        return numberNodes;
    }
}
