package BeamAco;

import java.util.Comparator;

public class BeamElementChildValueCompare implements Comparator<BeamElement> {
    @Override
    public int compare(BeamElement o1, BeamElement o2) {
        return Double.compare(o2.value, o1.value);
    }
}
