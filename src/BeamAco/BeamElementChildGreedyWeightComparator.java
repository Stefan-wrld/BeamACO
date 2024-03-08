package BeamAco;

import java.util.Comparator;

public class BeamElementChildGreedyWeightComparator implements Comparator<BeamElement> {
    @Override
    public int compare(BeamElement o1, BeamElement o2) {
        return Double.compare(o2.greedyWeight, o1.greedyWeight);
    }
}
