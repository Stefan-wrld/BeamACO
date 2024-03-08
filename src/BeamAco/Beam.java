package BeamAco;

import java.util.*;

public class Beam extends ArrayList<BeamElement> {

    /**
     * Wählt ein zufälliges Element aus der Beam-Liste basierend auf einer gewichteten Zufallsauswahl aus.
     * @param basesum Die Summe der Gewichte aller Elemente in der Beam-Liste.
     * @return Ein zufällig ausgewähltes BeamElement.
     */
    public BeamElement randomWheel(double basesum) {
        Random rnd = new Random();
        double rand = rnd.nextDouble() * basesum;
        double wheel = 0;
        BeamElement retElement = null;
        for (BeamElement be : this) {
            wheel += be.value;
            if(wheel > rand) {
               retElement = be;
            }
        }
        assert Objects.requireNonNull(retElement).node >= 0;
        assert retElement != this.getLast();
        return retElement;
    }

    /**
     * Wählt eine bestimmte Anzahl von Elementen aus einer Liste von Kindern aus und fügt sie der aktuellen Beam-Liste hinzu.
     * @param children Die Liste von Kindern, aus der ausgewählt werden soll.
     * @param childrenBasesum Die Summe der Gewichte aller Elemente in der Liste von Kindern.
     * @param toChoose Die Anzahl der Elemente, die ausgewählt werden sollen.
     * @param detRate Die Determinismusrate, die angibt, wie deterministisch die Auswahl sein soll.
     */
    public void chooseFrom(Beam children, double childrenBasesum, int toChoose, double detRate) {
        Random rng = new Random();
        if (children.size() <= toChoose) {
            for (BeamElement be : children) {
                be.commit();
                this.add(be);
            }
        } else {
            children.sort (new BeamElementChildValueCompare());
            for (int i = 0; i < toChoose; i++) {
                BeamElement child;
                if (detRate >= 1.0 || (detRate > 0.0  &&  rng.nextDouble()< detRate)) {
                    child = children.getFirst();
                } else {
                    child = children.randomWheel(childrenBasesum);
                }
                child.commit();
                this.add(child);
                childrenBasesum = childrenBasesum - child.value;
                children.remove (child);
            }
        }
        children.clear();
    }
}
