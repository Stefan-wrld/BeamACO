package BeamAco;

import java.util.Comparator;

public class BeamElementLowerBoundCompare implements Comparator<BeamElement> {
    @Override
    public int compare(BeamElement c1, BeamElement c2) {
        if (c1.lowerBoundConstraintViolations < c2.lowerBoundConstraintViolations) {
            return -1;
        } else if (c1.lowerBoundConstraintViolations == c2.lowerBoundConstraintViolations) {
            if (c1.lowerBound < c2.lowerBound) {
                return -1;
            } else if (c1.lowerBound == c2.lowerBound) {
                return 0;
            }
        }
        return 1;
    }
}
