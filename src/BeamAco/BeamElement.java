package BeamAco;

public class BeamElement extends Ant {
    public int node;
    public double value;
    public double greedyWeight;
    public double greedyRankSum;
    public BeamElement() {
        super();
        node = -1;
        value = 0.0;
        greedyWeight = 0.0;
        greedyRankSum = 0.0;
    }

    public BeamElement(BeamElement other) {
        super(other);
        this.node = other.node;
        this.value = other.value;
        this.greedyWeight = other.greedyWeight;
        this.greedyRankSum = other.greedyRankSum;
    }

    /**
     * Generiert eine Liste von Kindern für dieses BeamElement basierend auf dem maximal zulässigen Anzahl von Kindern.
     * @param maxChildren Die maximale Anzahl von Kindern, die erzeugt werden sollen.
     * @return Eine Liste von Kindern als Beam.
     */
    public Beam produceChildren(int maxChildren) {
        Beam childList = new Beam();
        // Erstelle neue partielle Lösung,
        // indem no unassigned nodes zur aktuellen Lösung hinzugefügt werden.
        for (int k = 0, inode = 0; k < nodesAvailable; k++) {
            while (nodeAssigned[++inode]);

            // Erzeugt ein Kind, indem die Lösung kopiert wird.
            BeamElement child = new BeamElement(this);
            child.node = inode;
            int last = child.permutation.getLast();

            child.greedyWeight = BeamElement.heuristicInfo[last][child.node];

            childList.add(child);
        }
        childList.sort (new BeamElementChildGreedyWeightComparator());
        Beam retBeam = new Beam();
        for (int i = 1; i <= maxChildren &&  i <= childList.size(); i++) {
            BeamElement child = childList.get(i-1);
            child.greedyRankSum = child.greedyRankSum + i;
            int last = child.permutation.getLast();
            child.value = BeamElement.pheromone[last][child.node];
            retBeam.add(child);
        }
        return retBeam;
    }

    /**
     * Fügt den Knoten dieses BeamElements der aktuellen Lösung hinzu.
     */
    public void commit() {
        int last = permutation.getLast();
        add(last, node);
    }
}
