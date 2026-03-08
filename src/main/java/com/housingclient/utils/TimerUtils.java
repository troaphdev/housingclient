package com.housingclient.utils;

public class TimerUtils {
    
    private long lastMs;
    
    public TimerUtils() {
        reset();
    }
    
    public void reset() {
        lastMs = System.currentTimeMillis();
    }
    
    public boolean hasReached(long milliseconds) {
        return System.currentTimeMillis() - lastMs >= milliseconds;
    }
    
    public boolean hasReachedAndReset(long milliseconds) {
        if (hasReached(milliseconds)) {
            reset();
            return true;
        }
        return false;
    }
    
    public long getElapsedTime() {
        return System.currentTimeMillis() - lastMs;
    }
    
    public long getLastMs() {
        return lastMs;
    }
    
    public void setLastMs(long lastMs) {
        this.lastMs = lastMs;
    }
    
    // Static utility methods
    public static long getCurrentMs() {
        return System.currentTimeMillis();
    }
    
    public static long getNanoTime() {
        return System.nanoTime();
    }
    
    public static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    public static void sleepNanos(long nanos) {
        try {
            Thread.sleep(nanos / 1_000_000, (int) (nanos % 1_000_000));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
