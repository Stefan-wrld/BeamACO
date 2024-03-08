package BeamAco;

import java.io.PrintStream;
import java.util.List;

public class Aco {
    static String instance = "src/instances/AFG/rbg233.tw";
    static int nOfIter = 300;
    static int timeLimit = 15;
    static int maxLocalSearchPerIter = Integer.MAX_VALUE;
    static double timeTaken;
    static double timeLocalSearch;
     static double timeInit;
     static double timeBeam;

    static int nOfAnts = 1;

    static double lRate = 0.1;

    static double tauMin = 0.001;
    static double tauMax = 0.999;

    static double detRate = 0.9;

    static int nOfTrials = 5;

    // Parameter für PBS
    static int beamWidth = 2;
    static double mu = 2.0;
    static int maxChildren = 100;

    // Parameter für SS
    static int nSamples = 10;
    static int samplePercent = 100;
    static int sampleRate = -1;

    static PrintStream stream = System.out;


    static Solution bestSoFar = null;
    static Solution restartBest = null;
    static Solution iterationBest = null;

    public static void printTraceHeader(){
        stream.printf ("# Trial Iteration     Cost  Cviols     Time  %8s  %8s\n", "TimeLS", "TimeSampling");
    }
    public static void printTrace(Solution s, int trialCounter, int iter, double timeTaken) {
        stream.printf("%7d %9d %8.2f  %6d  %8.1f  %8.1f  %8.1f\n", trialCounter, iter,s.cost(), s.constraintViolations(), timeTaken, timeLocalSearch, Ant.timeSampling);
    }
    public static void trialBegin(int trialCounter)
    {
        stream.printf ("# begin try %d\n", trialCounter);
        printTraceHeader();
    }

    static void
    updatePheromoneValues(boolean bsUpdate, double cf) {
        double iWeight;
        double rWeight;
        double gWeight;

        if (bsUpdate) {
            iWeight = 0.0;
            rWeight = 0.0;
            gWeight = 1.0;
        } else {
            if (cf < 0.4) {
                iWeight = 1.0;
                rWeight = 0.0;
                gWeight = 0.0;
            } else if (cf < 0.6) {
                iWeight = 2.0 / 3.0;
                rWeight = 1.0 / 3.0;
                gWeight = 0.0;
            } else if (cf < 0.8) {
                iWeight = 1.0 / 3.0;
                rWeight = 2.0 / 3.0;
                gWeight = 0.0;
            } else {
                iWeight = 0.0;
                rWeight = 1.0;
                gWeight = 0.0;
            }
        }
        int n = Solution.n;
        double[][] d = new double[n][n];
        List<Integer> ib = iterationBest.permutation;
        List<Integer> rb = restartBest.permutation;
        List<Integer> bf = bestSoFar.permutation;
        for (int i = 1; i < n; i++) {
            d[ib.get(i - 1)][ib.get(i)] += iWeight;
            d[rb.get(i - 1)][rb.get(i)] += rWeight;
            d[bf.get(i - 1)][bf.get(i)] += gWeight;
        }
        double[][] ph = Ant.pheromone;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                ph[i][j] += lRate * (d[i][j] - ph[i][j]);
                if (ph[i][j] > tauMax) {
                    ph[i][j] = tauMax;
                }
                if (ph[i][j] < tauMin) {
                    ph[i][j] = tauMin;
                }
            }
        }
    }

    static double computeConvergenceFactor() {

        double retVal = 0.0;
        int n = Solution.n;
        int count = n * n;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                retVal = retVal + Math.max(tauMax - Ant.pheromone[i][j],
                        Ant.pheromone[i][j] - tauMin);
            }
        }
        retVal = retVal / (count * (tauMax - tauMin));
        retVal = (retVal - 0.5) * 2.0;
        return retVal;
    }

    public static void main(String[] args) {
        instance = "src/instances/AFG/rbg233.tw";
        Stopwatch stopwatch = new Stopwatch();
            Ant.init(instance);
            int toChoose = (int) (beamWidth * mu);
            sampleRate = (int) ((samplePercent * (Solution.n - 1) / 100.0 + 0.5) + 1);
            Solution.printParameters("#", System.err);

            Solution best = null;

            timeInit = stopwatch.getElapsedTime();

            System.err.printf("# Initialization Time %g\n",
                    timeInit);

            for (int trialCounter = 1; trialCounter <= nOfTrials; trialCounter++) {
                stopwatch = new Stopwatch();
                stream = System.out;
                trialBegin(trialCounter);
                int iter = 1;

                bestSoFar = null;
                restartBest = null;

                if (trialCounter == 1) {
                    Ant.initUniformPheromoneValues();
                } else {
                    Ant.resetUniformPheromoneValues();
                }

                boolean bsUpdate = false;
                boolean restart = false;
                timeTaken = 0.0;
                timeLocalSearch = 0.0;
                timeInit = 0.0;
                timeBeam = 0.0;
                Ant.timeSampling = 0.0;
                Solution.evaluations = 0;

                double start = System.nanoTime();

                // Hier ist die Hauptschleife des Beam-ACO
                while (iter <= nOfIter && stopwatch.getElapsedTime() < timeLimit) {
                    iterationBest = null;
                    double avgCost = 0.0;
                    double avgViols = 0.0;

                    double timeBeamStop = stopwatch.getElapsedTime();

                    for (int i = 0; i < nOfAnts; i++) {
                        Ant ant = new Ant();
                        Solution newSol;
                        if (beamWidth > 1) {
                            newSol = ant.beamConstruct(detRate, beamWidth,
                                    maxChildren,
                                    toChoose,
                                    nSamples, sampleRate);
                        } else {
                            newSol = ant.construct(detRate);
                        }
                        timeBeam = stopwatch.getElapsedTime() - timeBeamStop;
                        double timeLocalSearchStop = stopwatch.getElapsedTime();

                        Solution lsSol = newSol.localSearch();
                        int iterationLocal = 0;
                        while (lsSol.betterThan(newSol) && iterationLocal < maxLocalSearchPerIter) {
                            newSol = lsSol;
                            lsSol = newSol.localSearch();
                            iterationLocal++;
                        }
                        timeLocalSearch += stopwatch.getElapsedTime() - timeLocalSearchStop;

                        newSol = lsSol;
                        avgCost = avgCost + newSol.cost();
                        avgViols = avgViols + newSol.constraintViolations();
                        if (iterationBest == null) {
                            iterationBest = newSol;
                        } else if (newSol.betterThan(iterationBest)) {
                            iterationBest = newSol;
                        }
                    }

                    timeTaken = stopwatch.getElapsedTime();

                    if (iter == 1) {
                        assert iterationBest != null;
                        bestSoFar = new Solution(iterationBest);
                        restartBest = new Solution(iterationBest);

                        printTrace(bestSoFar, trialCounter, iter, timeTaken);

                    } else if (restart) {
                        restart = false;
                        assert iterationBest != null;
                        restartBest = new Solution(iterationBest);

                        if (iterationBest.betterThan(bestSoFar)) {
                            bestSoFar = new Solution(iterationBest);
                            printTrace(bestSoFar, trialCounter, iter, timeTaken);
                        }
                    } else {
                        assert iterationBest != null;
                        if (iterationBest.betterThan(restartBest)) {
                            restartBest = new Solution(iterationBest);
                        }

                        if (iterationBest.betterThan(bestSoFar)) {
                            bestSoFar = new Solution(iterationBest);
                            printTrace(bestSoFar, trialCounter, iter, timeTaken);
                        }
                    }

                    if (best == null) {
                        best = new Solution(bestSoFar);
                    } else if (bestSoFar.betterThan(best)) {
                        best = new Solution(bestSoFar);
                    }
                    double cf = computeConvergenceFactor();
                    if (bsUpdate && (cf > 0.99)) {
                        bsUpdate = false;
                        restart = true;
                        Ant.resetUniformPheromoneValues();
                    } else {
                        if (cf > 0.99)
                            bsUpdate = true;
                        updatePheromoneValues(bsUpdate, cf);
                    }
                    iter = iter + 1;
                }
                double end = System.nanoTime();
                double time = end - start;
                System.out.println("Time limit reached: " + time * 10e-10 + " at Iteration: " + iter);
                System.out.println("Best Tour: " + bestSoFar.permutation);
            }
    }
}
