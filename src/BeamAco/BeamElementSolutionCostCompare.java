package BeamAco;

import java.util.Comparator;

public class BeamElementSolutionCostCompare implements Comparator<BeamElement> {
    @Override
    public int compare(BeamElement o1, BeamElement o2) {
        if(o1.constraintViolations < o2.constraintViolations) {
            return -1;
        } else if (o1.constraintViolations == o2.constraintViolations) {
            return Double.compare(o1.cost(), o1.cost());
        } else {
            return 1;
        }
    }
}
