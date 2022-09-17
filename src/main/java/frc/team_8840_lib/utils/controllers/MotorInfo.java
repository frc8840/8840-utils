package frc.team_8840_lib.utils.controllers;

import com.revrobotics.CANSparkMax;

public class MotorInfo {
    public final double temperature;
    public final double voltage;
    public final double current;

    public MotorInfo(double temperature, double voltage, double current) {
        this.temperature = temperature;
        this.voltage = voltage;
        this.current = current;
    }

    public static MotorInfo fromController(CANSparkMax controller) {
        return new MotorInfo(controller.getMotorTemperature(), controller.getAppliedOutput(), controller.getOutputCurrent());
    }
}
