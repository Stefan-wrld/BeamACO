package BeamAco;

import java.util.List;

public class GraphGenerator {
    public static double[][] toDoubleMatrix(List<List<String>> matrix) {

        double[][] result = new double[matrix.size()][];

        for (int i = 0; i < matrix.size(); i++) {

            result[i] = new double[matrix.get(i).size()];

            for (int j = 0; j < matrix.get(i).size(); j++) {

                if (matrix.get(i).get(j) == null || matrix.get(i).get(j).isEmpty()) {
                    result[i][j] = 0.0;
                } else {
                    result[i][j] = Double.parseDouble(matrix.get(i).get(j).trim());
                }
            }
        }
        return result;
    }
}
