package frc.team_8840_lib.utils.math;

import edu.wpi.first.math.geometry.Translation2d;

public class MathUtils {
    public static double sigmoid(double x) {
        return 1 / (1 + Math.exp(-x));
    }

    public static double sigmoidDerivative(double x) {
        return sigmoid(x) * (1 - sigmoid(x));
    }

    public static double degreesToRadians(double degrees) {
        return degrees * Math.PI / 180;
    }

    public static double radiansToDegrees(double radians) {
        return radians * 180 / Math.PI;
    }

    public static double normalizeAngle(double angle) {
        if (angle <= 360 && angle >= 0) {
            return angle;
        }

        double newAngle = angle % 360;

        if (newAngle < 0) {
            newAngle += 360;
        } else if (newAngle > 360) {
            newAngle -= 360;
        }

        return newAngle;
    }

    public static double inchesToMeters(double inches) {
        return inches * 0.0254;
    }
    public static double metersToInches(double meters) {
        return meters * 39.3701;
    }

    public static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }
    
    /**
     * Returns a percentage used in the backend of the swerve utils.
     * @param a Time passed or whatever time measurement. Range: [0, 10]. Will start up after .5
     * @return Percentage of the change, range: [0, 1]
     */
    public static double swerveFunc(double a) {
        return swerveFunc(a, 0.5);
    }

    /**
     * Returns a percentage used in the backend of the swerve utils.
     * @param a Time passed or whatever time measurement. Range: [0, 9.5 + end0]. Will start up after end0.
     * @return Percentage of the change, range: [0, 1]
     */
    public static double swerveFunc(double a, double end0) {
        if (a < end0) return 0;
        if (a > 9.5 + end0) return 1;
        return Math.min(Math.max(1.018 * sigmoid(a - (4.5 + end0)) - 0.0112, 0), 1);
    }

    /**
     * Clamps a value between a min and max
     * @param value Value to clamp
     */
    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Returns the distance between two Translation2d objects
     * @param a First Translation2d
     * @param b Second Translation2d
     * @return Distance between the two Translation2d objects
     */
    public static double distance(Translation2d a, Translation2d b) {
        return Math.sqrt(Math.pow(a.getX() - b.getX(), 2) + Math.pow(a.getY() - b.getY(), 2));
    }

    /**
     * Returns the distance between (x1, y1) and (x2, y2)
     * @param x1 x value of first point
     * @param y1 y value of first point
     * @param x2 x value of second point
     * @param y2 y value of second point
     * @return Distance between the two points
     */
    public static double distance(double x1, double y1, double x2, double y2) {
        return Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
    }
}
