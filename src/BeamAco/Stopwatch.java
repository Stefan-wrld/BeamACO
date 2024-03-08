package BeamAco;

public class Stopwatch {
    private final double startTime;

    public Stopwatch() {
        startTime = System.nanoTime();
    }

    public double getElapsedTime() {
        double end = System.nanoTime();
        double elapsedTime = end - startTime;
        return elapsedTime * 1e-9;
    }
}
