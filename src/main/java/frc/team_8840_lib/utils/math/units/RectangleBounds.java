package frc.team_8840_lib.utils.math.units;

import edu.wpi.first.math.geometry.Translation2d;

public class RectangleBounds {
    private double x;
    private double y;
    private double width;
    private double height;

    public RectangleBounds(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public RectangleBounds(Translation2d position, double width, double height) {
        this.x = position.getX();
        this.y = position.getY();
        this.width = width;
        this.height = height;
    }

    public Translation2d getPosition() {
        return new Translation2d(x, y);
    }

    public double getCenterX() {
        return x + width / 2;
    }

    public double getCenterY() {
        return y + height / 2;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }
}
