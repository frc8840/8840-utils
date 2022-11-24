package frc.team_8840_lib.controllers;

import com.ctre.phoenix.motorcontrol.ControlMode;
import com.ctre.phoenix.motorcontrol.DemandType;
import com.ctre.phoenix.motorcontrol.TalonFXControlMode;
import com.ctre.phoenix.motorcontrol.can.TalonFX;
import com.ctre.phoenix.sensors.CANCoder;
import com.revrobotics.CANSparkMax;
import com.revrobotics.CANSparkMaxLowLevel;
import com.revrobotics.RelativeEncoder;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.controller.SimpleMotorFeedforward;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.RobotBase;
import frc.team_8840_lib.utils.controllers.SCType;
import frc.team_8840_lib.utils.controllers.swerve.CTREConfig;
import frc.team_8840_lib.utils.controllers.swerve.CTREModuleState;
import frc.team_8840_lib.utils.controllers.swerve.conversions.FalconConversions;
import frc.team_8840_lib.utils.controllers.swerve.conversions.NeoConversions;

public class SwerveModule extends ControllerGroup.SpeedController {
    private double driveSpeed;
    private double turnSpeed;

    private CTREConfig config;

    //TalonFX variables
    private TalonFX driveTalonFX;
    private TalonFX turnTalonFX;

    //NEO variables
    private CANSparkMax driveNEO;
    private CANSparkMax turnNEO;

    private RelativeEncoder neoDriveEncoder;
    private RelativeEncoder neoTurnEncoder;

    public static PIDController neoDrivePIDController = null;
    public static PIDController neoTurnPIDController = null;

    //Simulation variables
    private double falconDriveSimVelocity;
    private double neoDriveSimVelocity;
    private double canCoderAngle;

    //Angle encoder - used for both NEO and Falcon
    private CANCoder angleCANCoder;

    //Feedforward is also used for both
    private SimpleMotorFeedforward feedforward;

    //Private ID is a number between 0 and 3 which is used to identify the module
    private int privateID;

    private double lastAngle;

    public SwerveModule(int drivePort, int steerPort, int encoderPort, int swerveNum, CTREConfig config) {
        super(drivePort, config.getSettings().getType());

        this.privateID = swerveNum;

        driveSpeed = 0;
        turnSpeed = 0;

        falconDriveSimVelocity = 0;
        neoDriveSimVelocity = 0;

        this.config = config;

        this.angleCANCoder = new CANCoder(encoderPort);
        this.configAngleEncoder();

        if (config.getSettings().getType() == SCType.SWERVE_Talon_FX) {
            this.driveTalonFX = new TalonFX(drivePort);
            this.turnTalonFX = new TalonFX(steerPort);
        } else if (config.getSettings().getType() == SCType.SWERVE_SparkMax) {
            if (neoDrivePIDController == null) {
                neoDrivePIDController = new PIDController(
                        config.getSettings().drivePID.kP,
                        config.getSettings().drivePID.kI,
                        config.getSettings().drivePID.kD
                );
            }

            if (neoTurnPIDController == null) {
                neoTurnPIDController = new PIDController(
                        config.getSettings().turnPID.kP,
                        config.getSettings().turnPID.kI,
                        config.getSettings().turnPID.kD
                );
            }

            driveNEO = new CANSparkMax(drivePort, CANSparkMaxLowLevel.MotorType.kBrushless);
            turnNEO = new CANSparkMax(steerPort, CANSparkMaxLowLevel.MotorType.kBrushless);

            //Bit useless but it's here lol
            neoDriveEncoder = driveNEO.getEncoder();
            neoTurnEncoder = turnNEO.getEncoder();
        }

        this.configMotors();

        feedforward = new SimpleMotorFeedforward(config.getSettings().driveKS, config.getSettings().driveKV, config.getSettings().driveKA);

        this.lastAngle = getState().angle.getDegrees();
    }

    public void configMotors() {
        if (this.config.getSettings().getType() == SCType.SWERVE_Talon_FX) {
            configTalonMotors();
        } else if (this.config.getSettings().getType() == SCType.SWERVE_SparkMax) {
            configNEOMotors();
        }
    }

    public void configNEOMotors() {
        turnNEO.restoreFactoryDefaults();
        driveNEO.restoreFactoryDefaults();

        turnNEO.setSmartCurrentLimit((int) Math.round(config.getSettings().turnCurrentLimit.continuousCurrent));
        driveNEO.setSmartCurrentLimit((int) Math.round(config.getSettings().driveCurrentLimit.continuousCurrent));

        turnNEO.setInverted(config.getSettings().turnInverted);
        driveNEO.setInverted(config.getSettings().driveInverted);

        turnNEO.setIdleMode(config.getSettings().turnIdleMode);
        driveNEO.setIdleMode(config.getSettings().driveIdleMode);

        turnNEO.burnFlash();
        driveNEO.burnFlash();

        resetToAbsolute();
        neoDriveEncoder.setPosition(0d);
    }

    public void configTalonMotors() {
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
        double rawDegrees = getRotation().getDegrees() - config.getSettings().angleOffsets[privateID];
        if (this.config.getSettings().getType() == SCType.SWERVE_Talon_FX) {
            double absPos = FalconConversions.degreesToFalcon(rawDegrees, config.getSettings().angleGearRatio);
            this.turnTalonFX.setSelectedSensorPosition(absPos);
        } else if (this.config.getSettings().getType() == SCType.SWERVE_SparkMax) {
            double absPos = FalconConversions.degreesToFalcon(rawDegrees, config.getSettings().angleGearRatio);
            this.turnNEO.getEncoder().setPosition(absPos);
        }
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

        if (this.getType() == SCType.SWERVE_Talon_FX) {
            if (Math.abs(speed) < this.config.getSettings().threshold) {
                driveTalonFX.set(TalonFXControlMode.PercentOutput, 0);
            } else {
                driveTalonFX.set(TalonFXControlMode.PercentOutput, this.driveSpeed);
            }
        } else if (this.getType() == SCType.SWERVE_SparkMax) {
            driveNEO.set(this.driveSpeed);
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

        if (this.getType() == SCType.SWERVE_Talon_FX) {
            turnTalonFX.set(ControlMode.Position, FalconConversions.degreesToFalcon(angle, config.getSettings().angleGearRatio));
        } else if (this.getType() == SCType.SWERVE_SparkMax) {
            turnNEO.setVoltage(
                    neoTurnPIDController.calculate(
                            neoTurnEncoder.getPosition(),
                            NeoConversions.degreesToNeo(angle, config.getSettings().angleGearRatio)
                    )
            );
        }

        lastAngle = angle;
        canCoderAngle = angle;
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
            if (getType() == SCType.SWERVE_Talon_FX) {
                falconDriveSimVelocity = optimizedState.speedMetersPerSecond;
            } else if (getType() == SCType.SWERVE_SparkMax) {
                neoDriveSimVelocity = optimizedState.speedMetersPerSecond;
            }
        } else {
            double velocity = 0;
            if (getType() == SCType.SWERVE_Talon_FX) {
                velocity = FalconConversions.MPSToFalcon(optimizedState.speedMetersPerSecond, config.getSettings().wheelCircumference(), config.getSettings().driveGearRatio);

                double ffvelo = feedforward.calculate(optimizedState.speedMetersPerSecond);
                this.driveTalonFX.set(TalonFXControlMode.Velocity, velocity, DemandType.ArbitraryFeedForward, ffvelo);

                falconDriveSimVelocity = optimizedState.speedMetersPerSecond;
            } else if (getType() == SCType.SWERVE_SparkMax) {
                velocity = NeoConversions.MPSToNeo(optimizedState.speedMetersPerSecond, config.getSettings().wheelCircumference(), config.getSettings().driveGearRatio);
                this.driveNEO.setVoltage(
                        neoDrivePIDController.calculate(
                                neoDriveEncoder.getVelocity(),
                                velocity
                        ) + feedforward.calculate(optimizedState.speedMetersPerSecond)
                );

                neoDriveSimVelocity = optimizedState.speedMetersPerSecond;
            }
        }

        this.setAngle(optimizedState);
    }

    public double getSpeed() {
        return driveSpeed;
    }

    public double getLastAngle() {
        return lastAngle;
    }

    public Rotation2d getRotation() {
        return Rotation2d.fromDegrees(RobotBase.isSimulation() ? canCoderAngle : angleCANCoder.getAbsolutePosition());
    }

    public SwerveModuleState getState() {
        double velocity = 0;
        Rotation2d angle = new Rotation2d();
        if (getType() == SCType.SWERVE_Talon_FX) {
            velocity = RobotBase.isSimulation() ? falconDriveSimVelocity : FalconConversions.falconToMPS(driveTalonFX.getSelectedSensorVelocity(), config.getSettings().wheelCircumference(), config.getSettings().driveGearRatio);
            angle = RobotBase.isSimulation() ? Rotation2d.fromDegrees(lastAngle) : Rotation2d.fromDegrees(FalconConversions.falconToDegrees(turnTalonFX.getSelectedSensorPosition(), config.getSettings().angleGearRatio));
        } else if (getType() == SCType.SWERVE_SparkMax) {
            velocity = RobotBase.isSimulation() ? neoDriveSimVelocity : NeoConversions.neoToMPS(neoDriveEncoder.getVelocity(), config.getSettings().wheelCircumference(), config.getSettings().driveGearRatio);
            angle = RobotBase.isSimulation() ? Rotation2d.fromDegrees(lastAngle) : Rotation2d.fromDegrees(NeoConversions.neoToDegrees(neoTurnEncoder.getPosition(), config.getSettings().angleGearRatio));
        }

        return new SwerveModuleState(velocity, angle);
    }
}
