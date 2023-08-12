package frc.team_8840_lib.utils.controllers.swerve;

import com.revrobotics.CANSparkMax;

import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.SwerveDriveKinematics;
import edu.wpi.first.math.util.Units;
import frc.team_8840_lib.listeners.Robot;
import frc.team_8840_lib.utils.controllers.swerve.structs.CurrentLimit;
import frc.team_8840_lib.utils.controllers.swerve.structs.PIDStruct;
import frc.team_8840_lib.utils.math.units.Unit;
import frc.team_8840_lib.utils.math.units.Unit.Type;

//A lot of these values are taken from Team 364's Constants.java file, esp the PID values: these can be fine-tuned since it's an instantiable class though, so it's not a big deal
public class SwerveSettings {
    public boolean invertGyro = false; // Counterclockwise is positive, clockwise is negative

    public Unit trackWidth = new Unit(21.73, Unit.Type.INCHES);
    public Unit wheelBase = new Unit(21.73, Unit.Type.INCHES);
    public double wheelDiameter = Units.inchesToMeters(3.94);

    public boolean doManualConversion = Robot.isSimulation();

    public double wheelCircumference() {
        return wheelDiameter * Math.PI;
    }

    public double driveGearRatio = 6.86;
    public double angleGearRatio = 12.8;

    public Unit maxSpeed = new Unit(4.5, Type.FEET); // m/s
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

    public CurrentLimit secondaryDriveCurrentLimit = new CurrentLimit(35, 40, 0.1);
    public CurrentLimit secondaryTurnCurrentLimit = new CurrentLimit(25, 40, 0.1);

    public double voltageCompensation = 12;

    public double driveKS = (0.667);
    public double driveKV = (2.44);
    public double driveKA = (0.27);

    public CANSparkMax.IdleMode driveIdleMode = CANSparkMax.IdleMode.kBrake;
    public CANSparkMax.IdleMode turnIdleMode = CANSparkMax.IdleMode.kBrake;

    //Threshold used for driving. If the magnitude of the joystick is less than this value, the robot will not move.
    public double threshold = 0.05;
    //Whether to use the threshold as a percentage of the max speed or just a value.
    public boolean useThresholdAsPercentage = false;

    public Rotation2d gyroscopeStartingAngle = new Rotation2d(0);

    /**
     * Gets the min speed for the drive motors using threshold as a percentage of max speed (0.01% by default)
     * */
    public double relativeThreshold() {
        if (useThresholdAsPercentage) {
            return maxSpeed.get(Type.METERS) * threshold;
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
        double ctrackWidth = trackWidth.get(Type.METERS);
        double cwheelBase = wheelBase.get(Type.METERS);

        return new Translation2d[] {
            new Translation2d(ctrackWidth / 2.0, cwheelBase / 2.0), //TOP RIGHT
            new Translation2d(-ctrackWidth / 2.0, cwheelBase / 2.0), //tech TOP LEFT, but it's actually the bottom right
            new Translation2d(ctrackWidth / 2.0, -cwheelBase / 2.0), //tech BOTTOM RIGHT, but it's actually the top left
            new Translation2d(-ctrackWidth / 2.0, -cwheelBase / 2.0) //BOTTOM LEFT
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
                public static final Unit maxSpeed_NEO = new Unit(12.0, Unit.Type.FEET);
            }

            /**
             * A list of settings for the MK4i Level 2.
             */
            public static class L2 {
                public static final Unit maxSpeed_NEO = new Unit(14.5, Unit.Type.FEET);
            }

            /**
             * A list of settings for the MK4i Level 3.
             */
            public static class L3 {
                public static final Unit maxSpeed_NEO = new Unit(16.0, Unit.Type.FEET);
            }
        }
    }
}
