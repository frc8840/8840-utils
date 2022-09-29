package frc.team_8840_lib.utils.controllers.swerve;

import com.ctre.phoenix.motorcontrol.ControlMode;
import com.ctre.phoenix.motorcontrol.DemandType;
import com.ctre.phoenix.motorcontrol.NeutralMode;
import com.ctre.phoenix.motorcontrol.TalonFXControlMode;
import com.ctre.phoenix.motorcontrol.can.TalonFX;
import com.ctre.phoenix.sensors.CANCoder;
import edu.wpi.first.math.controller.SimpleMotorFeedforward;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.SwerveDriveKinematics;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import frc.team_8840_lib.controllers.ControllerGroup;
import frc.team_8840_lib.utils.controllers.SCType;

public class SwerveModule extends ControllerGroup.SpeedController {
    private double driveSpeed;
    private double turnSpeed;

    private CTREConfig config;

    private TalonFX driveTalonFX;
    private TalonFX turnTalonFX;

    private CANCoder angleCANCoder;

    //Private ID is a number between 0 and 3 which is used to identify the module
    private int privateID;

    private SimpleMotorFeedforward feedforward;

    private double lastAngle;

    public SwerveModule(int drivePort, int steerPort, int swerveNum, CTREConfig config) {
        super(drivePort, config.getSettings().getType());

        this.privateID = swerveNum;

        driveSpeed = 0;
        turnSpeed = 0;

        this.config = config;

        this.angleCANCoder = new CANCoder(steerPort);
        this.configAngleEncoder();

        if (config.getSettings().getType() == SCType.SWERVE_Talon_FX) {
            this.driveTalonFX = new TalonFX(drivePort);
            this.turnTalonFX = new TalonFX(steerPort);
        }

        feedforward = new SimpleMotorFeedforward(config.getSettings().driveKS, config.getSettings().driveKV, config.getSettings().driveKA);

        this.lastAngle = getState().angle.getDegrees();
    }

    public void configMotors() {
        this.turnTalonFX.configFactoryDefault();
        this.driveTalonFX.configFactoryDefault();

        //Config the settings defined in the config
        this.turnTalonFX.configAllSettings(config.getTurnTalonFXConfiguration());
        this.driveTalonFX.configAllSettings(config.getDriveTalonFXConfiguration());

        //Invert
        this.turnTalonFX.setInverted(config.getSettings().turnInverted);
        this.driveTalonFX.setInverted(config.getSettings().driveInverted);

        //Set the neutral mode
        this.turnTalonFX.setNeutralMode(config.getSettings().turnNeutralMode);
        this.driveTalonFX.setNeutralMode(config.getSettings().driveNeutralMode);

        this.driveTalonFX.setSelectedSensorPosition(0);

        resetToAbsolute();
    }

    public void configAngleEncoder() {
        this.angleCANCoder.configFactoryDefault();
        this.angleCANCoder.configAllSettings(this.config.getCanCoderConfiguration());
    }

    public void resetToAbsolute() {
        double absPos = FalconConversions.degreesToFalcon(getRotation().getDegrees() - config.getSettings().angleOffsets[privateID], config.getSettings().angleGearRatio);
        this.turnTalonFX.setSelectedSensorPosition(absPos);
    }

    /**
     * Sets the % output of the drive motor to the given value
     * @param speed The speed to set the drive motor to
     * */
    public void setSpeed(double speed) {
        if (Math.abs(speed) > 1) {
            speed = Math.signum(speed);
        }

        this.driveSpeed = speed * config.getSettings().maxSpeed;

        if (Math.abs(speed) < this.config.getSettings().threshold) {
            driveTalonFX.set(TalonFXControlMode.PercentOutput, 0);
        } else {
            driveTalonFX.set(TalonFXControlMode.PercentOutput, speed);
        }
    }

    /**
     * Sets the angle of the module to the given angle in the desired state
     * */
    public void setAngle(SwerveModuleState desiredState) {
        double angle = desiredState.angle.getDegrees();
        if (Math.abs(desiredState.speedMetersPerSecond) <= this.config.getSettings().relativeThreshold()) {
            angle = lastAngle;
        }

        turnTalonFX.set(ControlMode.Position, FalconConversions.degreesToFalcon(angle, config.getSettings().angleGearRatio));
        lastAngle = angle;
    }

    /**
     * Sets angle of the module to the given angle in degrees. Ignores the jittering preventions though.
     * */
    public void setAngle(double angle) {
        this.setAngle(new SwerveModuleState(1, Rotation2d.fromDegrees(angle)));
    }

    public void setDesiredState(SwerveModuleState state, boolean isOpenLoop) {
        //TODO: fix the optimization to make it more efficient
        SwerveModuleState optimizedState = CTREModuleState.optimize(state, getRotation());

        if (isOpenLoop) {
            this.setSpeed(optimizedState.speedMetersPerSecond / config.getSettings().maxSpeed);
        } else {
            double velocity = FalconConversions.MPSToFalcon(optimizedState.speedMetersPerSecond, config.getSettings().wheelCircumference(), config.getSettings().driveGearRatio);
            this.driveTalonFX.set(TalonFXControlMode.Velocity, velocity, DemandType.ArbitraryFeedForward, feedforward.calculate(optimizedState.speedMetersPerSecond));
        }

        this.setAngle(optimizedState);
    }

    public double getSpeed() {
        return driveSpeed;
    }

    public double getTurnSpeed() {
        return turnSpeed;
    }

    public Rotation2d getRotation() {
        return Rotation2d.fromDegrees(angleCANCoder.getAbsolutePosition());
    }

    public SwerveModuleState getState() {
        double velocity = FalconConversions.falconToMPS(driveTalonFX.getSelectedSensorVelocity(), config.getSettings().wheelCircumference(), config.getSettings().driveGearRatio);
        Rotation2d angle = Rotation2d.fromDegrees(FalconConversions.falconToDegrees(turnTalonFX.getSelectedSensorPosition(), config.getSettings().angleGearRatio));
        return new SwerveModuleState(velocity, angle);
    }
}
