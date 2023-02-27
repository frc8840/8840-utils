package frc.team_8840_lib.utils.controllers.swerve;

import com.ctre.phoenix.motorcontrol.NeutralMode;
import com.revrobotics.CANSparkMax;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.SwerveDriveKinematics;
import edu.wpi.first.math.util.Units;
import frc.team_8840_lib.listeners.Robot;
import frc.team_8840_lib.utils.controllers.swerve.structs.CurrentLimit;
import frc.team_8840_lib.utils.controllers.swerve.structs.PIDStruct;
import frc.team_8840_lib.utils.math.operators.Operation;

//A lot of these values are taken from Team 364's Constants.java file, esp the PID values: these can be fine-tuned since it's an instantiable class though, so it's not a big deal
public class SwerveSettings {
    private SwerveType type = SwerveType.FALCON_500;

    public SwerveType getType() {
        return type;
    }

    public SwerveSettings(SwerveType type) {
        this.type = type;
    }

    public boolean invertGyro = false; // Counterclockwise is positive, clockwise is negative

    public double trackWidth = Units.inchesToMeters(21.73);
    public double wheelBase = Units.inchesToMeters(21.73);
    public double wheelDiameter = Units.inchesToMeters(3.94);

    public boolean doManualConversion = Robot.isSimulation();

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

    public boolean manualOffset = true;

    //PIDStruct follows (kp, ki, kd, kf)
    public PIDStruct turnPID = new PIDStruct(0.99, 0.0, 0.0, 0.0);
    public PIDStruct drivePID = new PIDStruct(0.10, 0.0, 0.0, 0.0);

    public boolean useCurrentLimits = true;
    //Current limit follows (continuousCurrent, peakCurrent, currentDuration)
    public CurrentLimit driveCurrentLimit = new CurrentLimit(35, 60, 0.1);
    public CurrentLimit turnCurrentLimit = new CurrentLimit(25, 40, 0.1);
    //For NEOs
    public CurrentLimit secondaryDriveCurrentLimit = new CurrentLimit(25, 40, 0.1);
    public CurrentLimit secondaryTurnCurrentLimit = new CurrentLimit(25, 40, 0.1);

    public double voltageCompensation = 12;

    public double driveKS = (0.667 / 12);
    public double driveKV = (2.44 / 12);
    public double driveKA = (0.27 / 12);

    public NeutralMode driveNeutralMode = NeutralMode.Brake;
    public NeutralMode turnNeutralMode = NeutralMode.Coast;
    public CANSparkMax.IdleMode driveIdleMode = CANSparkMax.IdleMode.kBrake;
    public CANSparkMax.IdleMode turnIdleMode = CANSparkMax.IdleMode.kBrake;

    public Operation[] finalAngleCalculation = new Operation[] {
        Operation.IDENTITY,
        Operation.IDENTITY,
        Operation.IDENTITY,
        Operation.IDENTITY
    };

    public Operation[] finalSpeedCalculation = new Operation[] {
        Operation.IDENTITY,
        Operation.IDENTITY,
        Operation.IDENTITY,
        Operation.IDENTITY
    };

    public boolean useAngleCalculations = true;
    public boolean useSpeedCalculations = true;

    public double[] angleOffsets = new double[]{0, 0, 0, 0};

    //Threshold used for driving. If the magnitude of the joystick is less than this value, the robot will not move.
    public double threshold = 0.05;
    //Whether to use the threshold as a percentage of the max speed or just a value.
    public boolean useThresholdAsPercentage = false;

    private boolean[] reversedDrive = new boolean[] {false, false, false, false};

    private boolean[] reversedTurnEncoders = new boolean[] {false, false, false, false};

    /**
     * Returns whether the drive motor for the given index is reversed.
     * @param index The index of the drive motor.
     * @return Whether the drive motor for the given index is reversed.
     */
    public boolean getReverseDrive(int index) {
        if (index > reversedDrive.length || index < 0) {
            throw new IndexOutOfBoundsException("Index " + index + " is out of bounds for the reversedDrive array (length " + reversedDrive.length + ")");
        }

        return reversedDrive[index];
    }

    /**
     * Reverses the drive motor for the given indexes.
     * @param indexes The indexes of the drive motors to reverse.
     */
    public void reverseDrive(int... indexes) {
        for (int i : indexes) {
            if (i > reversedDrive.length || i < 0) {
                throw new IndexOutOfBoundsException("Index " + i + " is out of bounds for the reversedDrive array (length " + reversedDrive.length + ")");
            }

            reversedDrive[i] = true;
        }
    }

    /**
     * Returns whether the encoder for the given index is reversed.
     * @param index The index of the encoder.
     * @return Whether the encoder for the given index is reversed.
     */
    public boolean getEncoderIsReversed(int index) {
        if (index > reversedTurnEncoders.length || index < 0) {
            throw new IndexOutOfBoundsException("Index " + index + " is out of bounds for the reversedTurnEncoders array (length " + reversedTurnEncoders.length + ")");
        }

        return reversedTurnEncoders[index];
    }

    /**
     * Inverts the encoder for the given indexes.
     * @param indexes The indexes of the encoders to invert.
     */
    public void invertEncoder(int... indexes) {
        for (int i : indexes) {
            if (i > reversedTurnEncoders.length || i < 0) {
                throw new IndexOutOfBoundsException("Index " + i + " is out of bounds for the reversedTurnEncoders array (length " + reversedTurnEncoders.length + ")");
            }

            reversedTurnEncoders[i] = true;
        }
    }

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
            new Translation2d(trackWidth / 2.0, wheelBase / 2.0), //TOP RIGHT
            new Translation2d(-trackWidth / 2.0, wheelBase / 2.0), //tech TOP LEFT, but it's actually the bottom right
            new Translation2d(trackWidth / 2.0, -wheelBase / 2.0), //tech BOTTOM RIGHT, but it's actually the top left
            new Translation2d(-trackWidth / 2.0, -wheelBase / 2.0) //BOTTOM LEFT
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
        if (this.type == SwerveType.FALCON_500) {
            //Drive KS, KV, and KA values are different for the TalonFX
            driveKS = 0.667 / 12;
            driveKV = 2.44 / 12;
            driveKA = 0.27 / 12;

        } else if (this.type == SwerveType.SPARK_MAX) {
            //Drive KS, KV, and KA values are different for the SparkMax
            driveKS = 0.667;
            driveKV = 2.44;
            driveKA = 0.27;
        }
    }

    /**
     * A list of settings for the Swerve Drive Specialties.
     */
    public static class SDS {
        /**
         * A list of settings for the MK4i.
         */
        public static class MK4i {
            /**
             * A list of settings for the MK4i Level 1.
             */
            public static class L1 {
                public static final double maxSpeed_NEO = 12.0;
                public static final double maxSpeed_Falcon500 = 13.5;
            }

            /**
             * A list of settings for the MK4i Level 2.
             */
            public static class L2 {
                public static final double maxSpeed_NEO = 14.5;
                public static final double maxSpeed_Falcon500 = 16.3;
            }

            /**
             * A list of settings for the MK4i Level 3.
             */
            public static class L3 {
                public static final double maxSpeed_NEO = 16.0;
                public static final double maxSpeed_Falcon500 = 18.0;
            }
        }
    }
}
