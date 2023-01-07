package frc.team_8840_lib.utils.controllers.swerve;

import com.ctre.phoenix.motorcontrol.NeutralMode;
import com.revrobotics.CANSparkMax;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.SwerveDriveKinematics;
import edu.wpi.first.math.util.Units;
import frc.team_8840_lib.info.console.Logger;
import frc.team_8840_lib.utils.controllers.SCType;
import frc.team_8840_lib.utils.controllers.swerve.structs.CurrentLimit;
import frc.team_8840_lib.utils.controllers.swerve.structs.PIDStruct;

//A lot of these values are taken from Team 364's Constants.java file, esp the PID values: these can be fine-tuned since it's an instantiable class though, so it's not a big deal
public class SwerveSettings {
    private SCType type = SCType.SWERVE_Talon_FX;

    public SCType getType() {
        return type;
    }

    public SwerveSettings(SCType type) {
        if (!type.name().startsWith("SWERVE")) {
            Logger.Log("Error: SwerveSettings constructor called with non-swerve type, defaulting to TalonFX");
            this.type = SCType.SWERVE_Talon_FX;
        } else {
            this.type = type;
        }
    }

    public boolean invertGyro = false; // Counterclockwise is positive, clockwise is negative

    public double trackWidth = Units.inchesToMeters(21.73);
    public double wheelBase = Units.inchesToMeters(21.73);
    public double wheelDiameter = Units.inchesToMeters(3.94);

    public double wheelCircumference() {
        return wheelDiameter * Math.PI;
    }

    public int pinionTeethCount = 14;
    public double neoDrivingMotorReduction = 990d / ((double) (pinionTeethCount * 15));

    public double driveGearRatio = 6.86;
    public double angleGearRatio = 12.8;

    public double maxSpeed = 4.5; // m/s
    public double maxAngularSpeed = Math.PI; // rad/s

    public double driveOpenRampRate = 0.25;
    public double driveClosedRampRate = 0;

    public boolean canCoderInverted = false;
    public boolean driveInverted = false;
    public boolean turnInverted = false;

    //PIDStruct follows (kp, ki, kd, kf)
    public PIDStruct turnPID = new PIDStruct(0.6, 0.0, 12.0, 0.0);
    public PIDStruct drivePID = new PIDStruct(0.10, 0.0, 0.0, 0.0);

    public boolean useCurrentLimits = true;
    //Current limit follows (continuousCurrent, peakCurrent, currentDuration)
    public CurrentLimit driveCurrentLimit = new CurrentLimit(35, 60, 0.1);
    public CurrentLimit turnCurrentLimit = new CurrentLimit(25, 40, 0.1);
    //For NEOs
    public CurrentLimit secondaryDriveCurrentLimit = new CurrentLimit(25, 40, 0.1);
    public CurrentLimit secondaryTurnCurrentLimit = new CurrentLimit(25, 40, 0.1);

    public double driveKS = (0.667 / 12);
    public double driveKV = (2.44 / 12);
    public double driveKA = (0.27 / 12);

    public NeutralMode driveNeutralMode = NeutralMode.Brake;
    public NeutralMode turnNeutralMode = NeutralMode.Coast;
    public CANSparkMax.IdleMode driveIdleMode = CANSparkMax.IdleMode.kBrake;
    public CANSparkMax.IdleMode turnIdleMode = CANSparkMax.IdleMode.kBrake;

    public double[] angleOffsets = new double[]{0, 0, 0, 0};

    //Threshold used for driving. If the magnitude of the joystick is less than this value, the robot will not move.
    public double threshold = 0.05;
    //Whether to use the threshold as a percentage of the max speed or just a value.
    public boolean useThresholdAsPercentage = false;

    /**
     * Gets the min speed for the drive motors using threshold as a percentage of max speed (0.01% by default)
     * */
    public double relativeThreshold() {
        if (useThresholdAsPercentage) {
            return maxSpeed * threshold;
        } else {
            return threshold;
        }
    }

    private SwerveDriveKinematics swerveKinematics = new SwerveDriveKinematics(
            getPositions()
    );

    /**
     * Returns the kinematics for the swerve drive
     * */
    public SwerveDriveKinematics getKinematics() {
        return swerveKinematics;
    }

    /**
     * Returns the calculated positions of the modules with the given track width and wheel base
     * */
    public Translation2d[] getPositions() {
        return new Translation2d[] {
                new Translation2d(wheelBase / 2, -trackWidth / 2),
                new Translation2d(wheelBase / 2, trackWidth / 2),
                new Translation2d(-wheelBase / 2, -trackWidth / 2),
                new Translation2d(-wheelBase / 2, trackWidth / 2)
        };
    }

    /**
     * Updates the kinematics with the new wheelbase and trackwidth
     * */
    public void updateKinematics() {
        swerveKinematics =
                new SwerveDriveKinematics(
                        getPositions()
                );
    }

    /**
     * This function adjusts any values that may be different between the two swerve types
     * Use at your own risk, I would do adjustments AFTER calling this function.
     * */
    public void defaultAdjustToType() {
        if (this.type == SCType.SWERVE_Talon_FX) {
            //Drive KS, KV, and KA values are different for the TalonFX
            driveKS = 0.667 / 12;
            driveKV = 2.44 / 12;
            driveKA = 0.27 / 12;

            //The PID values are different for the TalonFX
            turnPID = new PIDStruct(0.6, 0.0, 12.0, 0.0);
            drivePID = new PIDStruct(0.10, 0.0, 0.0, 0.0);
        } else if (this.type == SCType.SWERVE_SparkMax) {
            //Drive KS, KV, and KA values are different for the SparkMax
            driveKS = 0.667;
            driveKV = 2.44;
            driveKA = 0.27;

            //The PID values are different for the SparkMax
            turnPID = new PIDStruct(0.99, 0.0, 0.0, 0.0);
            drivePID = new PIDStruct(0.10, 0.0, 0.0, 0.0);
        }
    }
}
