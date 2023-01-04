package frc.team_8840_lib.utils.controllers;

import com.revrobotics.CANSparkMax;

public class MotorInfo {
    public final double temperature;
    public final double voltage;
    public final double current;

    /**
     * Creates a storage struct for the tempature, voltage, and current of a controller
     * @param temperature Tempature of controller
     * @param voltage Voltage of controller
     * @param current Current of controller
     */
    public MotorInfo(double temperature, double voltage, double current) {
        this.temperature = temperature;
        this.voltage = voltage;
        this.current = current;
    }

    /**
     * Returns the motor info of a CANSparkMax controller.
     * @param controller CANSparkMax controller
     * @return MotorInfo of controller.
     */
    public static MotorInfo fromController(CANSparkMax controller) {
        return new MotorInfo(controller.getMotorTemperature(), controller.getAppliedOutput(), controller.getOutputCurrent());
    }
}
