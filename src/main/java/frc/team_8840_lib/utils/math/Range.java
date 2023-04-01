package frc.team_8840_lib.utils.math;

public class Range {
    private double min;
    private double max;

    public Range(double min, double max) {
        this.min = min;
        this.max = max;

        if (min > max) {
            this.min = max;
            this.max = min;
        }
    }

    public double getMin() {
        return this.min;
    }

    public double getMax() {
        return this.max;
    }

    public boolean inRange(double n) {
        return n >= this.min && n <= this.max;
    }

    public double clamp(double n) {
        return Math.max(this.min, Math.min(this.max, n));
    }
}
