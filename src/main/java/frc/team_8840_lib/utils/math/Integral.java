package frc.team_8840_lib.utils.math;

import frc.team_8840_lib.utils.interfaces.SingleVariableEquation;

public class Integral {
    SingleVariableEquation method;
    private double step;
    
    public Integral(SingleVariableEquation method, double step) {
        this.method = method;
        this.step = step;
    }

    public Integral(SingleVariableEquation method) {
        this(method, 0.0001);
    }

    public double calculate(double startPoint, double endPoint) {
        double sum = 0;
        for (double x = startPoint; x < endPoint; x += step) {
            sum += method.evaluate(x) * step;
        }

        return sum;
    }

    public double rightRiemannSum(double[] xPoints) {
        double sum = 0;
        for (int i = 0; i < xPoints.length - 1; i++) {
            sum += method.evaluate(xPoints[i]) * (xPoints[i + 1] - xPoints[i]);
        }

        return sum;
    }

    public double leftRiemannSum(double[] xPoints) {
        double sum = 0;
        for (int i = 1; i < xPoints.length; i++) {
            sum += method.evaluate(xPoints[i]) * (xPoints[i] - xPoints[i - 1]);
        }

        return sum;
    }
}
