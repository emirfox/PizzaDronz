package uk.ac.ed.inf;

public class ExecutionTimer {
    private long start;
    private long end;

    public ExecutionTimer() {

    }

    public void start() {
        start = System.currentTimeMillis();
    }

    public void stop() {
        end = System.currentTimeMillis();
    }

    /**
     * @return elapsed time in milliseconds between start() and stop().
     */
    public long getDuration() {
        return end - start;
    }
}
