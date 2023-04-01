package frc.team_8840_lib.utils.math;

public class IntRange {
    private int min;
    private int max;

    public IntRange(int min, int max) {
        this.min = min;
        this.max = max;

        if (min > max) {
            this.min = max;
            this.max = min;
        }
    }

    public int getMin() {
        return this.min;
    }

    public int getMax() {
        return this.max;
    }

    public boolean inRange(int n) {
        return n >= this.min && n <= this.max;
    }

    public int clamp(int n) {
        return Math.max(this.min, Math.min(this.max, n));
    }

    public double clamp(double n) {
        return Math.max(this.min, Math.min(this.max, n));
    }

    public float clamp(float n) {
        return Math.max(this.min, Math.min(this.max, n));
    }

    public long clamp(long n) {
        return Math.max(this.min, Math.min(this.max, n));
    }
}
