package frc.team_8840_lib.utils.math.operators;

import java.util.ArrayList;

import frc.team_8840_lib.utils.math.operators.specifics.Addition;
import frc.team_8840_lib.utils.math.operators.specifics.Divide;
import frc.team_8840_lib.utils.math.operators.specifics.Multiply;
import frc.team_8840_lib.utils.math.operators.specifics.Subtract;

public class Operation {
    public static Operation create() {
        return new Operation();
    }

    public static final Operation IDENTITY = new Operation();
    
    private ArrayList<Operator> operators;

    public Operation() {
        operators = new ArrayList<Operator>();
    }

    public Operation add(double value) {
        operators.add(new Addition(value));
        return this;
    }

    public Operation subtract(double value) {
        operators.add(new Subtract(value));
        return this;
    }

    public Operation multiply(double value) {
        operators.add(new Multiply(value));
        return this;
    }

    public Operation divide(double value) {
        operators.add(new Divide(value));
        return this;
    }

    public double calculate(double x) {
        double xCopy = x;
        for (Operator operator : operators) {
            xCopy = operator.calculate(xCopy);
        }
        return xCopy;
    }

    public double inverse(double y) {
        double yCopy = y;
        for (Operator operator : operators) {
            yCopy = operator.inverse(yCopy);
        }
        return yCopy;
    }
}
