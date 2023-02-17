package frc.team_8840_lib.utils.math.operators.specifics;

import frc.team_8840_lib.utils.math.operators.Operator;

public class Divide extends Operator {
    public Divide(double value) {
        super(value);
    }
    
    @Override
    public double calculate(double x) {
        return x / getValue();
    }

    @Override
    public double inverse(double x) {
        return x * getValue();
    }
}
