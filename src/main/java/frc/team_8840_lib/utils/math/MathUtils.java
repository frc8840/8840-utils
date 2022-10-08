package frc.team_8840_lib.utils.math;

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
}
