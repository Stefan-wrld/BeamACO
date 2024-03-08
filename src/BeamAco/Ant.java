package BeamAco;

import java.util.*;

public class Ant extends Solution{
    public static double[][] pheromone;
    public static Random rng = new Random();
    public static double timeSampling;
    private static double[][] total;
    private final double[] probs;
    private double basesum;

    /**
     * Initialisiert die Ameise mit einer Instanz.
     * @param instance Der Pfad zur Instanzdatei.
     */
    public static void init(String instance) {
        Solution.loadInstance(instance);
        pheromone = new double[Solution.n][Solution.n];
        total = new double[n][n];
        timeSampling = 0;
    }

    public Ant() {
        super();
        probs = new double[n];
    }
    public Ant(Ant other) {
        super(other);
        this.probs = Arrays.copyOf(other.probs, other.probs.length);
        this.basesum = other.basesum;
    }

    /**
     * Füllt eine Matrix mit einem bestimmten Wert.
     * @param m Die Matrix, die gefüllt werden soll.
     * @param n Die Größe der Matrix.
     * @param value Der Wert, mit dem die Matrix gefüllt werden soll.
     */
    private static void matrixFill(double[][] m, int n, double value) {
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                m[i][j] = value;
    }

    public static void initUniformPheromoneValues()
    {
        matrixFill(pheromone, n, 0.5);
    }

    public static void resetUniformPheromoneValues()
    {
        matrixFill(pheromone, n, 0.5);
    }

    /**
     * Aktualisiert die Wahrscheinlichkeiten für die nächste Knotenauswahl.
     * @param added Der zuletzt hinzugefügte Knoten.
     */
    private void updateProbs(int added) {
        basesum = 0.0;
        probs[added] = 0.0;
        for (int k = 0, inode = 0; k < nodesAvailable; k++) {
            while (nodeAssigned[++inode]);
            probs[inode] = total[added][inode];
            basesum += probs[inode];
        }
        assert (probs[added]== 0.0);
        assert (Double.isFinite(basesum));
    }

    /**
     * Ermittelt den Knoten mit der maximalen Wahrscheinlichkeit für die nächste Auswahl.
     * @return Der Index des Knotens mit der maximalen Wahrscheinlichkeit.
     */
    private int maximumProb() {
        int maxNode = -1;
        double maxProb = 0.0;
        for (int i = 1; i < n; i++) {
            if (maxProb < probs[i]) {
                maxProb = probs[i];
                maxNode = i;
            }
        }
        return maxNode;
    }

    /**
     * Führt einen Konstruktionsschritt für die Ameise durch.
     * @param last Der Index des zuletzt hinzugefügten Knotens.
     * @param detRate Die Determinismusrate für die Knotenauswahl.
     * @return Der Index des neu hinzugefügten Knotens.
     */
    private int constructionStep(int last, double detRate) {
        int node;
        updateProbs(last);
        double random = rng.nextDouble();
        if (detRate >= 1.0 || (detRate > 0.0  && random < detRate)) {
            node = maximumProb();
        } else {
            node = randomWheel();
        }
        assert (node >= 0); // Negativ heist nicht gefunden.
        add (last, node);
        return node;
    }

    private void precomputeTotal() {
        for (int k = 0; k < n; k++) {
            for (int j = 0; j < n; j++) {
                total[k][j] = pheromone[k][j] * heuristicInfo[k][j];
            }
        }
    }

    /**
     * Konstruiert eine Lösung mit der Ameise.
     * @param detRate Die Determinismusrate für die Knotenauswahl.
     * @return Eine Lösung, die von der Ameise erstellt wurde.
     */
    public Solution construct(double detRate) {
        randomizeHinfo();
        precomputeTotal();
        int last = 0;

        do {
            last = constructionStep(last, detRate);
        } while (nodesAvailable > 0);

        Solution solution = this;
        return new Solution(solution);
    }

    /**
     * Führt eine zufällige Auswahl basierend auf den Wahrscheinlichkeiten durch.
     * @return Der Index des ausgewählten Knotens.
     */
    private int randomWheel() {
        double rand = rng.nextDouble() * basesum;
        int i = 1;
        double wheel = probs[i];
        while (wheel <= rand) {
            i++;
            wheel += probs[i];
        }
        assert (i > 0);
        assert (i < n);
        assert (!nodeAssigned[i]);
        assert (probs[i] > 0.0);
        return i;
    }

    /**
     * Vervollständigt die Konstruktion einer Lösung mit der Ameise.
     * @param detRate Die Determinismusrate für die Knotenauswahl.
     */
    public void complete(double detRate) {
        int last = permutation.getLast();
        while (nodesAvailable > 0) {
            last = constructionStep(last, detRate);
        }
        assert (checkSolution());
    }

    // Da java keine nextPermutation methode bereitstellt, wurde sie von hier entnommen
    // https://stackoverflow.com/questions/62786114/is-there-a-next-permutation-in-java
    private static boolean findNextPermutation(int[] p) {
        for (int a = p.length - 2; a >= 0; --a)
            if (p[a] < p[a + 1])
                for (int b = p.length - 1;; --b)
                    if (p[b] > p[a]) {
                        int t = p[a];
                        p[a] = p[b];
                        p[b] = t;
                        for (++a, b = p.length - 1; a < b; ++a, --b) {
                            t = p[a];
                            p[a] = p[b];
                            p[b] = t;
                        }
                        return true;
                    }
        return false;
    }

    /**
     * Führt das stochastic sampling durch.
     * @param nSamples Die Anzahl der stochastic samplings.
     * @param detRate Die Determinismusrate für die Knotenauswahl.
     * @return Die beste Lösung, die durch stochastic sampling gefunden wurde.
     */
    public Ant stochasticSampling(int nSamples, double detRate) {
        Stopwatch timerSampling = new Stopwatch();
        lowerBound = -1;
        lowerBoundConstraintViolations = -1;
        int cviols = lowerBoundConstraintViolations;

        Ant best = new Ant();

        if (nodesAvailable <= 3) {
            int k;
            int inode = 0;
            int[] p = new int[nodesAvailable];
            for (k = 0; k < nodesAvailable; k++) {
                while (nodeAssigned[++inode]);
                p[k] = inode;
            } do {
                Ant sol = new Ant(this);
                sol.add (p);
                if (lowerBound == -1) {
                    lowerBound = sol.cost();
                    cviols = sol.constraintViolations();
                    best = sol;
                } else if (sol.constraintViolations() < cviols
                || (sol.constraintViolations() == cviols
                        && sol.cost() < lowerBound)) {
                    lowerBound = sol.cost();
                    cviols = sol.constraintViolations();
                    best = sol;
                }
            } while (findNextPermutation(p));
            return new Ant(best);
        }

        for (int i = 0; i < nSamples; i++) {
            Ant sol = new Ant(this);
            sol.complete (detRate);
            if (lowerBound == -1) {
                lowerBound = sol.cost();
                cviols = sol.constraintViolations();
                best = sol;
            }
            else if (sol.constraintViolations() < cviols
                    || (sol.constraintViolations() == cviols
                    && sol.cost() < lowerBound)) {
                lowerBound = sol.cost();
                cviols = sol.constraintViolations();
                best = sol;
            }
        }
        timeSampling += timerSampling.getElapsedTime();
        return new Ant(best);
    }

    /**
     * Führt die Konstruktion von Lösungen mit Beam Search durch.
     * @param detRate Die Determinismusrate für die Knotenauswahl.
     * @param beamWidth Die Breite des Beams für die Beam Search.
     * @param maxChildren Die maximale Anzahl von Kindknoten für jedes Beam Element.
     * @param toChoose Die Anzahl der zu wählenden Elemente aus den Kindern.
     * @param nSamples Die Anzahl der für die stochastic sampling zu erstellenden Lösungen.
     * @param sampleRate Die Rate, bei der stochastische Abtastung durchgeführt wird.
     * @return Die beste gefundene Lösung durch Beam Search.
     */
    public Solution beamConstruct(double detRate, int beamWidth, int maxChildren, int toChoose, int nSamples, int sampleRate) {
        Beam beam = new Beam();
        Ant best = null;
        int beamDepth = 0;
        randomizeHinfo();
        precomputeTotal();
        BeamElement beamRoot = new BeamElement();
        beam.add(beamRoot);
        while (true) {
            Beam children = new Beam();
            for (BeamElement be : beam) {
               Beam tmpChildren = be.produceChildren(maxChildren);
               children.addAll(tmpChildren);
            }
            double greedyRankBasesum = 0.0;
            for (BeamElement be : children) {
                greedyRankBasesum += 1.0 / be.greedyRankSum;
            }
            double childrenBasesum = 0.0;
            for (BeamElement child : children) {
                double val = child.value * ((1.0 / child.greedyRankSum) / greedyRankBasesum);
                childrenBasesum += val;
                child.value = val;
            }
            Beam newBeam = new Beam();
            newBeam.chooseFrom(children, childrenBasesum, toChoose, detRate);
            beamDepth++;
            if(!newBeam.isEmpty()) {
                beam.clear();
                if (newBeam.size() > beamWidth) {
                    if (n - beamDepth <= sampleRate) {
                        for (int i = 0; i < newBeam.size(); i++) {
                            BeamElement beamNode = newBeam.get(i);
                            if (best == null || beamNode.betterThan(best)) {
                                Ant bestOfSampling = beamNode.stochasticSampling(nSamples, detRate);
                                if (best == null) {
                                    best = bestOfSampling;
                                } else if (bestOfSampling.betterThan(best)) {
                                    best = bestOfSampling;
                                }
                                i++;
                            } else {
                                newBeam.remove(beamNode);
                            }
                        }
                        newBeam.sort(new BeamElementLowerBoundCompare());
                    } else { // beamDepth > sampleRate
                        Collections.shuffle(newBeam);
                    }
                }

                int count = 0;
                BeamElement beamNode;
                while (count < beamWidth && count < newBeam.size()) {
                    beamNode = newBeam.get(count);
                    beam.add(beamNode);
                    count++;
                }
                if (newBeam.isEmpty()) {
                    assert (best != null);
                    Solution s = best;
                    s = new Solution(s);
                    return s;
                }
            }else {
              beam.sort(new BeamElementSolutionCostCompare());
              Ant bestOfBeam = beam.getFirst();
              beam.removeFirst();
              beam.clear();
              if(best == null) {
                  best = bestOfBeam;
              } else if (bestOfBeam.betterThan(best)) {
                  best = bestOfBeam;
              }
              Solution s = best;
              s = new Solution(s);
              return s;
            }
        }
    }
}
