package BeamAco;

import java.io.PrintStream;
import java.util.*;

public class Solution {
    public static String instance;
    public static int n = 0;
    public static int evaluations;
    public static double[][] heuristicInfo;
    public List<Integer> permutation;
    public boolean[] nodeAssigned;
    public int nodesAvailable;
    public int constraintViolations;
    public double infeasibility;
    public double lowerBound;
    public int lowerBoundConstraintViolations;
    private final double[] makespan;
    private double tourcost;
    private static double[] windowStart;
    // time-window start
    private static double windowStartMin = Double.MAX_VALUE;
    private static double windowStartMax = Double.MIN_VALUE;

    // time-window end
    private static double[] windowEnd;
    private static double windowEndMin = Double.MAX_VALUE;
    private static double windowEndMax = Double.MIN_VALUE;

    // travel time/distance
    private static double[][] distance;
    private static double distanceMin = Double.MAX_VALUE;
    private static double distanceMax = Double.MIN_VALUE;

    private static boolean[][] twInfeasible;
    private static int numTwInfeasible = 0;

    public Solution() {
        // Start am depot
        permutation = new ArrayList<>(n);
        permutation.add(0);
        nodeAssigned = new boolean[n];
        nodesAvailable = n;
        constraintViolations = 0;
        infeasibility = 0;
        lowerBound = -1;
        lowerBoundConstraintViolations = -1;
        makespan = new double[n+1];
        tourcost = 0;
        nodeAssigned[0] = true;
        nodesAvailable--;
    }

    public Solution(Solution other) {
        this.permutation = new ArrayList<>(other.permutation);
        this.nodeAssigned = Arrays.copyOf(other.nodeAssigned, other.nodeAssigned.length);
        this.nodesAvailable = other.nodesAvailable;
        this.constraintViolations = other.constraintViolations;
        this.infeasibility = other.infeasibility;
        this.lowerBound = other.lowerBound;
        this.lowerBoundConstraintViolations = other.lowerBoundConstraintViolations;
        this.makespan = Arrays.copyOf(other.makespan, other.makespan.length);
        this.tourcost = other.tourcost;
    }

    public static boolean fequals(double left, double right) {
        double epsilon = 1e-6;
        return Math.abs(left - right) < epsilon;
    }

    private static void calculateStaticHinfo() {
        windowStartMax = Arrays.stream(windowStart).max().getAsDouble();
        windowStartMin = Arrays.stream(windowStart).min().getAsDouble();
        windowEndMax = Arrays.stream(windowEnd).max().getAsDouble();
        windowEndMin = Arrays.stream(windowEnd).min().getAsDouble();
        for (int i = 0 ; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i == j) continue;
                if (distance[i][j] > distanceMax)
                    distanceMax = distance[i][j];
                if (distance[i][j] < distanceMin)
                    distanceMin = distance[i][j];
            }
        }
    }

    /**
     * Berechnet die gewichtete Heuristik-Information.
     * @param prev Der Index des vorherigen Knotens.
     * @param next Der Index des nächsten Knotens.
     * @param distW Das Gewicht für die Entfernung.
     * @param winstartW Das Gewicht für den Start des Zeitfensters.
     * @param winendW Das Gewicht für das Ende des Zeitfensters.
     * @return Die gewichtete Heuristik-Information.
     */
    private static double weightedHinfo(int prev, int next,
                                        double distW,
                                        double winstartW,
                                        double winendW) {
        if(prev==next) return 0.0;
        if(distW < 1e-6
        && winstartW < 1e-6
        && winendW < 1e-6) return 1.0;

        double hDist = ((distanceMax) - (distance[prev][next])) / (distanceMax - distanceMin);
        double hWinStart = ((windowStartMax) - (windowStart[next])) / (windowStartMax - windowStartMin);
        double hWinEnd = ((windowEndMax) - (windowEnd[next])) / (windowEndMax - windowEndMin);

        return distW * hDist
                + winstartW * hWinStart
                + winendW * hWinEnd;
    }

    /**
     * Erzeugt zufällige Gewichte für die Heuristik-Information und aktualisiert
     * die Heuristik-Informationen basierend auf diesen Gewichten.
     */
    public static void randomizeHinfo() {
        Random rng = new Random();
        double distdistWw = rng.nextDouble();
        double winstartW = rng.nextDouble();
        double winendW = rng.nextDouble();
        double total = distdistWw + winstartW + winendW;

        double distHeuristicWeight;
        double winstartHeuristicWeight;
        double winendHeuristicWeight;
        if (total < 1e-6) {
            distHeuristicWeight = 0.0;
            winstartHeuristicWeight = 0.0;
            winendHeuristicWeight = 0.0;
        } else  {
            distHeuristicWeight = distdistWw / total;
            winstartHeuristicWeight = winstartW / total;
            winendHeuristicWeight = winendW / total;
        }
        // vorberechnung der heuristicInfo
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                heuristicInfo[i][j] = weightedHinfo(i, j,
                        distHeuristicWeight,
                        winstartHeuristicWeight,
                        winendHeuristicWeight);
            }
        }
    }

    public static void loadInstance(String filename) {
        instance = filename;
        TSPInstanceLoader.loadInstance(filename);
        n = TSPInstanceLoader.getNumberNodes();
        windowStart = TSPInstanceLoader.getStartTimes();
        windowEnd = TSPInstanceLoader.getEndTimes();
        distance = TSPInstanceLoader.getCostMatrix();
        strongTimeWindowInfeasibility();
        heuristicInfo = new double[n][n];
        calculateStaticHinfo();
    }

    /**
     * Berechnet die starke Zeitfenster-Infeasibility für die geladene Instanz.
     * Diese Methode aktualisiert die statischen Variablen twInfeasible und numTwInfeasible.
     */
    private static void strongTimeWindowInfeasibility() {
        twInfeasible = new boolean[n][n];
        numTwInfeasible = 0;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                twInfeasible[i][j] = (windowStart[i] + distance[i][j] > windowEnd[j]) && (++numTwInfeasible > 0);
            }
        }
    }

    public static void printCompileParameters(PrintStream channel) {
        channel.print("TSPTW minimise tourcost [double values]\n");
    }

    public static void printParameters(String prefix, PrintStream channel) {
        channel.printf("%s Problem: ", prefix);
        printCompileParameters(channel);
        channel.println();
        channel.printf("%s instance : %s\n", prefix, instance);
        channel.printf("%s n. customers + depot: %d\n", prefix, n);
        channel.printf("%s distances   : [%g, %g]\n", prefix, distanceMin, distanceMax);
        channel.printf("%s windowStart: [%g, %g]\n", prefix, windowStartMin, windowStartMax);
        channel.printf("%s windowEnd  : [%g, %g]\n", prefix, windowEndMin, windowEndMax);
        double print = 100.0* numTwInfeasible /(n*n);
        channel.printf("%s n tw infeasible : %d (%g%%)\n", prefix, numTwInfeasible, print);
        channel.println();
    }

    public void printOneLine() {
        printOneLine(System.out);
    }

    public void printOneLine(PrintStream channel) {
        channel.printf ("%.2f\t%.2f\t%d\t%16g\t",
           makespan(), tourcost, constraintViolations, infeasibility);
        channel.println();
    }

    public boolean betterThan(Solution other) {
        return (this.constraintViolations < other.constraintViolations
                || (this.constraintViolations == other.constraintViolations
                && fless (this.cost(), other.cost())));
    }

    public static boolean fless(double left, double right) {
        double epsilon = 1e-6;
        if (Math.abs(left - right) < epsilon) {
            return false;
        }
        return left < right;
    }

    public double makespan() {
        return makespan[permutation.size()-1];
    }

    public double cost() {
        return tourcost;
    }

    public int constraintViolations() {
        return constraintViolations;
    }

    /**
     * Fügt einen Knoten zu der Toure hinzu, die von der aktuellen Position (current) zu dem angegebenen Knoten (node) führt.
     * Diese Methode aktualisiert die Route, die Kosten, die Infeasibility und die Constraint-Verletzungen.
     * @param current Der aktuelle Knoten, von dem aus der neue Knoten hinzugefügt wird.
     * @param node Der Knoten, der hinzugefügt wird.
     */
    public void add(int current, int node) {
        assert (node > 0);
        assert (node != current);
        assert (!nodeAssigned[node]);

        permutation.add(node);
        int j = permutation.size() - 1;

        makespan[j] =Math.max(makespan[j-1] + distance[current][node],
                windowStart[node]);

        if (makespan[j] > windowEnd[node]) {
            infeasibility += makespan[j] - windowEnd[node];
            constraintViolations++;
        }

        tourcost += distance[current][node];

        nodeAssigned[node] = true;
        nodesAvailable--;

        evaluations++;

        if (nodesAvailable == 1) {
            current = node;
            node = 0;
            while (nodeAssigned[++node]);
            add(current, node);
            // Letzter Knoten -> Verbindung mit Knoten.
            permutation.add(0);
            makespan[n] = makespan[n - 1] + distance[node][0];
            tourcost +=  distance[node][0];
            if (makespan[n] > windowEnd[0]) {
                constraintViolations++;
                infeasibility += makespan[n] - windowEnd[0];
            }
            evaluations++;
        }
    }

    /**
     * Fügt eine Liste von Knoten zu der Tour hinzu, beginnend mit dem letzten Knoten in der aktuellen Route.
     * Diese Methode fügt die Knoten in der Reihenfolge hinzu, wie sie in der übergebenen Liste (p) angegeben sind.
     * @param p Eine Liste von Knoten, die zur Route hinzugefügt werden sollen.
     */
    public void add(int[] p) {
        int last = permutation.getLast();
        int k = 0;
        for (int i = 0; i < nodesAvailable; i++) {
            int node = p[k];
            add (last, node);
            last = node;
            ++k;
        }
    }

    public boolean checkSolution() {
        double mkspan = 0;
        double cost = 0;
        int prev = 0; // starts at the depot
        int cviols = 0;
        int cviolsUnsure = 0;
        double infeas = 0;

        if (permutation.size() - 1 != n) {
            System.err.printf ("invalid: (permutation.size() == %d) != (n == %d)\n",
               permutation.size() - 1, n);
            return false;
        }

        for (int i = 1; i < n; i++) {
            int node = permutation.get(i);

            cost += distance[prev][node];
            mkspan = Math.max (mkspan + distance[prev][node], windowStart[node]);
            if (!fequals (mkspan, makespan[i])) {
                System.err.printf ("invalid: makespan = %g !=  makespan[%d] = %g!\n",
              mkspan, i, makespan[i]);
                return false;
            }

            if (makespan[i] > windowEnd[node]) {
                cviols++;
                infeas += makespan[i] - windowEnd[node];
            }
            if (fequals (makespan[i], windowEnd[node]))
                cviolsUnsure++;

            prev = node;
        }

        // finish at the depot
        cost += distance[prev][0];
        if (!fequals (cost, tourcost)) {
            System.err.printf ("invalid: real cost = %g !=  tourcost = %g!\n",
            cost, tourcost);
            return false;
        }

        mkspan = Math.max (mkspan + distance[prev][0], windowStart[0]);
        if (!fequals (mkspan, makespan[n])) {
            System.err.printf ("invalid: makespan = %g !=  makespan[n] = %g!\n",
            mkspan, makespan[n]);
            return false;
        }

        if (makespan[n] > windowEnd[0]) {
            cviols++;
            infeas += makespan[n] - windowEnd[0];
        }
        if (fequals (makespan[n], windowEnd[0]))
            cviolsUnsure++;

        if (Math.abs (cviols - constraintViolations) > cviolsUnsure) {
            System.err.printf ("invalid: real cviols = %d !=  constraintViolations = %d  (unsure = %d) !\n",
                    cviols, constraintViolations, cviolsUnsure);
            return false;
        } else if (constraintViolations < 0) {
            System.err.printf ("invalid: constraintViolations = %d  < 0 !\n",
                    constraintViolations);
            return false;
        }

        if (infeasibility != infeas) {
            System.err.printf ("invalid: infeasibility (%g) != real infeasibility (%g) !\n",
                    infeasibility, infeas);
            return false;
        }
        return true;
    }

    /**
     * Tauscht die Positionen der Knoten an den Indizes k und k+1 in der Tour.
     * Aktualisiert die Constraint-Verletzungen und die Infeasibility basierend auf dem neuen Zustand der Route.
     * @param k Der Index des ersten zu tauschenden Knotens in der Route.
     */
    private void swap(int k) {
        assert (k < n - 1);
        assert (k > 0);
        int a = permutation.get(k - 1);
        int b = permutation.get(k);
        int c = permutation.get(k + 1);

        doSwap(k);

        if (makespan[k] > windowEnd[b]) {
            constraintViolations--;
            infeasibility -= (makespan[k] - windowEnd[b]);
        }
        if (makespan[k+1] > windowEnd[c]) {
            constraintViolations--;
            infeasibility -= (makespan[k+1] - windowEnd[c]);
        }
        makespan[k] = Math.max(makespan[k-1] + distance[a][c], windowStart[c]);
        makespan[k+1] = Math.max(makespan[k] + distance[c][b], windowStart[b]);

        if (makespan[k] > windowEnd[c]) {
            constraintViolations++;
            infeasibility += (makespan[k] - windowEnd[c]);
        }
        if (makespan[k+1] > windowEnd[b]) {
            constraintViolations++;
            infeasibility += (makespan[k+1] - windowEnd[b]);
        }

        double mkspan = makespan[k+1];
        int i, current, prev = b; // permutation[k+1]
        for (i = k + 2; i < n + 1; i++, prev = current) {
            current = permutation.get(i);
            mkspan += distance[prev][current];

            if (makespan[i] > windowStart[current]) {
                // Es musste nicht gewartet werden ...
                if (makespan[i] > windowEnd[current]) {
                    // ... eine Nebenbedingung wurde zuvor gebrochen
                    constraintViolations--;
                    infeasibility -= (makespan[i] - windowEnd[current]);
                }
                if (mkspan <= windowStart[current]) {
                    // ... jetzt muss gewartet werden
                    makespan[i] = windowStart[current];
                    mkspan = windowStart[current];
                    continue;
                }
            } else {
                // Es musste zuvor gewartet werden ...
                if (mkspan <= windowStart[current]) {
                    // ... es muss immer noch gewartet werden.
                    makespan[i] = windowStart[current];
                    break;
                }
            }
            if (mkspan > windowEnd[current]) {
                // ... es wird nicht gewartet aber eine Nebenbedingung wird gebrochen.
                constraintViolations++;
                infeasibility += (mkspan - windowEnd[current]);
            }
            makespan[i] = mkspan;
        }
        evaluations += i - (k + 2);
    }

    private void insertionMove(int k, int i, int d) {
        swap(k);
    }

    private boolean infeasibleMove(int initial, int last) {
        return twInfeasible[permutation.get(last)][permutation.get(initial)];
    }
    Solution localSearch() {
        return localSearchInsertion();
    }

    /**
     * Führt die in der Bachelorarbeit beschriebene lokale Suche durch.
     * Es werden verschiedene Insertion Moves ausprobiert, um eine bessere Lösung zu finden.
     * @return Die beste gefundene Lösung nach der lokalen Suche.
     */
    private Solution localSearchInsertion() {
        Solution best = new Solution(this);
        Solution sol;
        for (int i = 1; i < n - 1; i++) {
            boolean moveP = this.infeasibleMove(i, i+1);
            if (moveP) {
                continue;// zum nächsten;
            }
            sol = new Solution(this);
            sol.insertionMove(i, i, i+1);
            if (sol.betterThan(best)) {
                return new Solution(sol);
            }
            Solution orb1 = new Solution(sol);
            for (int d = i + 1; d < n - 1; d++) {
                moveP = sol.infeasibleMove(d, d + 1);
                if (moveP) {
                    break;
                }
                sol.insertionMove(d, i, d+1);
                if (sol.betterThan(best)) {
                    return new Solution(sol);
                }
            }
            sol = orb1;
            for (int d = i - 1; d > 0; d--) {
                moveP = sol.infeasibleMove(d, d + 1);
                if (moveP) {
                    break;
                }
                sol.insertionMove(d, i + 1, d);
                if (sol.betterThan(best)) {
                    return new Solution(sol);
                }
            }
        }
        return new Solution(best);
    }

    /**
     * Führt einen Swap-Vorgang in der Permutation durch und aktualisiert den Tourkostenwert entsprechend.
     * @param k Der Index, an dem der Swap-Vorgang ausgeführt werden soll.
     * @return Die Änderung der Tourkosten durch den Swap.
     */
    private double doSwap(int k) {
        List<Integer> p = this.permutation;
        double gain = deltaSwap(k);
        tourcost += gain;
        int b = p.get(k);
        p.set(k, p.get(k + 1));
        p.set(k + 1, b);
        return gain;
    }

    /**
     * Berechnet die Änderung der Tourkosten, die durch einen Swap an einem bestimmten Index k entsteht.
     * @param k Der Index, an dem der Swap-Vorgang stattfinden würde.
     * @return Die Änderung der Tourkosten durch den Swap.
     */
    private double deltaSwap(int k) {
        final List<Integer> p = this.permutation;
        int a = p.get(k - 1);
        int b = p.get(k);
        int c = p.get(k + 1);
        int d = p.get(k + 2);
        double delta = (distance[a][c] + distance[c][b] + distance[b][d])
                - (distance[a][b] + distance[b][c] + distance[c][d]);
        evaluations += 6;
        return delta;
    }
}
