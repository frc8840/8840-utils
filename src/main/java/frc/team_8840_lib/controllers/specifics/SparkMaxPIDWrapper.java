package frc.team_8840_lib.controllers.specifics;

import com.revrobotics.CANSparkMax;
import com.revrobotics.SparkMaxPIDController;
import com.revrobotics.SparkPIDController;
import com.revrobotics.CANSparkBase.ControlType;

import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.controller.ProfiledPIDController;
import edu.wpi.first.math.controller.SimpleMotorFeedforward;
import edu.wpi.first.math.trajectory.TrapezoidProfile;
import edu.wpi.first.wpilibj.RobotController;

/**
 * Another fix for potential issues with REV's api. This is a wrapper for the onboard PID controller.
 * 
 * @author Jaiden Grimminck
 */
public class SparkMaxPIDWrapper {
    //Onboard PID controller
    private SparkPIDController onboardPIDController;
    private CANSparkMax controllerRef;

    //Encoder
    private SparkMaxEncoderWrapper encoder;

    //Local PID controller
    private PIDController pidController;
    private ProfiledPIDController profiledPIDController;
    private boolean useProfiledPIDController = false;

    private SimpleMotorFeedforward feedforward;

    boolean useManualPID = false;
    
    public SparkMaxPIDWrapper(CANSparkMax sparkMax) {
        controllerRef = sparkMax;
        onboardPIDController = sparkMax.getPIDController();
    }

    public void setFeedbackDevice(SparkMaxEncoderWrapper wrapper) {
        encoder = wrapper;
        if (!useManualPID) onboardPIDController.setFeedbackDevice(encoder.getEncoder());
    }

    /**
     * Sets the kP, kI, kD, kFF, and kIZone of the onboard PID controller
     * 
     * @param kP The proportional gain
     * @param kI The integral gain
     * @param kD The derivative gain
     * @param kFF The feedforward gain
     * @param kIZone The integral zone
     */
    public void setPID(double kP, double kI, double kD, double kFF, double kIZone) {
        if (!useManualPID) {
            onboardPIDController.setP(kP);
            onboardPIDController.setI(kI);
            onboardPIDController.setD(kD);
            onboardPIDController.setFF(kFF);
            onboardPIDController.setIZone(kIZone);
        }

        pidController = new PIDController(kP, kI, kD);
    }

    /**
     * Sets the kP, kI, kD, and TrapezoidProfile.Constraints of the local PID controller
     * @param kP The proportional gain
     * @param kI The integral gain
     * @param kD The derivative gain
     * @param constraints The constraints to use
     */
    public void setPIDProfile(double kP, double kI, double kD, TrapezoidProfile.Constraints constraints) {
        useProfiledPIDController = true;

        profiledPIDController = new ProfiledPIDController(kP, kI, kD, constraints);
    }

    /**
     * Sets the max and min output of the PID controller
     * @param min The minimum output
     * @param max The maximum output
     */
    public void setWrapAround(double min, double max) {
        if (useProfiledPIDController) {
            profiledPIDController.enableContinuousInput(min, max);
        } else {
            pidController.enableContinuousInput(min, max);
        }

        if (!useManualPID) {
            onboardPIDController.setPositionPIDWrappingEnabled(true);
            onboardPIDController.setPositionPIDWrappingMaxInput(max);
            onboardPIDController.setPositionPIDWrappingMinInput(min);
        }
    }

    /**
     * Sets the kS, kV, and kA of the feedforward
     * @param kS The static gain
     * @param kV The velocity gain
     * @param kA The acceleration gain
     */
    public void setFeedforward(double kS, double kV, double kA) {
        feedforward = new SimpleMotorFeedforward(kS, kV, kA);
    }

    /**
     * Sets whether to use the onboard PID controller or the local PID controller
     * @param useManualPID True to use the local PID controller, false to use the onboard PID controller
     */
    public void setUseManualPID(boolean useManualPID) {
        this.useManualPID = useManualPID;
    }

    /**
     * Sets the reference of the PID controller.
     * @param setpoint The setpoint to set
     * @param controlType The control type to use
     */
    public void setReference(double setpoint, ControlType controlType) {
        if (!useManualPID) {
            onboardPIDController.setReference(setpoint, controlType);
        } else {
            double currentPosition = encoder.getPosition();

            double pidOutput = 0;
            if (useProfiledPIDController) {
                pidOutput = profiledPIDController.calculate(currentPosition, setpoint);
            } else {
                pidOutput = pidController.calculate(currentPosition, setpoint);
            }

            //Set the voltage of the motor
            double batteryVoltage = RobotController.getBatteryVoltage();
            
            controllerRef.setVoltage((pidOutput) * batteryVoltage);
        }
    }

    /**
     * Sets the reference of the PID controller.
     * @param setpoint The setpoint to set
     * @param controlType The control type to use
     * @param pidSlot The PID slot to use
     * @param arbFeedforward The arbitrary feedforward to use
     */
    public void setReference(double setpoint, ControlType controlType, int pidSlot, double arbFeedforward) {
        if (!useManualPID) {
            onboardPIDController.setReference(setpoint, controlType, pidSlot, arbFeedforward);
        } else {
            double currentPosition = encoder.getPosition();

            double pidOutput = 0;
            if (useProfiledPIDController) {
                pidOutput = profiledPIDController.calculate(currentPosition, setpoint);
            } else {
                pidOutput = pidController.calculate(currentPosition, setpoint);
            }

            //Set the voltage of the motor
            double batteryVoltage = RobotController.getBatteryVoltage();

            controllerRef.setVoltage((pidOutput + arbFeedforward) * batteryVoltage);
        }
    }

    public double calculateFF(double setpoint) {
        if (feedforward != null) {
            return feedforward.calculate(setpoint);
        }

        return 0;
    }
}
