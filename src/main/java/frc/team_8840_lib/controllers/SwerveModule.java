package frc.team_8840_lib.controllers;

import com.ctre.phoenix.motorcontrol.ControlMode;
import com.ctre.phoenix.motorcontrol.DemandType;
import com.ctre.phoenix.motorcontrol.TalonFXControlMode;
import com.ctre.phoenix.motorcontrol.can.TalonFX;
import com.revrobotics.CANSparkMax;
import com.revrobotics.CANSparkMaxLowLevel;
import com.revrobotics.REVPhysicsSim;
import com.revrobotics.RelativeEncoder;
import com.revrobotics.SparkMaxPIDController;

import edu.wpi.first.math.controller.SimpleMotorFeedforward;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.kinematics.SwerveModulePosition;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import edu.wpi.first.math.system.plant.DCMotor;
import edu.wpi.first.wpilibj.RobotBase;
import frc.team_8840_lib.IO.devices.IOCANCoder;
import frc.team_8840_lib.utils.IO.IOAccess;
import frc.team_8840_lib.utils.IO.IOLayer;
import frc.team_8840_lib.utils.IO.IOMethod;
import frc.team_8840_lib.utils.IO.IOMethodType;
import frc.team_8840_lib.utils.IO.IOPermission;
import frc.team_8840_lib.utils.IO.IOValue;
import frc.team_8840_lib.utils.controllers.swerve.CTREConfig;
import frc.team_8840_lib.utils.controllers.swerve.SwerveType;
import frc.team_8840_lib.utils.controllers.swerve.conversions.FalconConversions;

/**
 * Sources:
 * https://www.chiefdelphi.com/t/adapting-364s-base-swerve-drive-code-to-use-neos-for-steering/418402/3
 * Team 364
 * Team 3512
 * **/

@IOAccess(IOPermission.READ)
public class SwerveModule extends IOLayer {
    //Ports
    private int drivePort;
    private int turnPort;

    //Type
    private SwerveType type;

    //Speed
    private double driveSpeed;

    //Config
    private CTREConfig config;

    //TalonFX variables
    private TalonFX driveTalonFX;
    private TalonFX turnTalonFX;

    //NEO variables
    private CANSparkMax driveNEO;
    private CANSparkMax turnNEO;

    //Neo Encoders
    private RelativeEncoder neoDriveEncoder;
    private RelativeEncoder neoTurnEncoder;

    //Neo PID controllers
    public static SparkMaxPIDController neoDrivePIDController = null;
    public static SparkMaxPIDController neoTurnPIDController = null;

    //Simulation variables
    private double speedCache = 0;
    //private Rotation2d angleCache = Rotation2d.fromDegrees(0);

    private Rotation2d lastAngle;

    //Angle encoder - used for both NEO and Falcon
    private IOCANCoder angleCANCoder;

    //Feedforward is also used for both
    private SimpleMotorFeedforward feedforward;

    //Private ID is a number between 0 and 3 which is used to identify the module
    private int privateID;

    public SwerveModule(int drivePort, int steerPort, int encoderPort, int swerveNum, CTREConfig config) {
        super();

        this.drivePort = drivePort;
        this.turnPort = steerPort;

        this.type = config.getSettings().getType();

        this.privateID = swerveNum;

        driveSpeed = 0;

        speedCache = 0;
        //angleCache = Rotation2d.fromDegrees(0);

        this.config = config;

        this.angleCANCoder = new IOCANCoder(encoderPort);
        this.configAngleEncoder();

        if (config.getSettings().getType() == SwerveType.FALCON_500) {
            this.driveTalonFX = new TalonFX(this.drivePort);
            this.turnTalonFX = new TalonFX(this.turnPort);
        } else if (config.getSettings().getType() == SwerveType.SPARK_MAX) {
            driveNEO = new CANSparkMax(this.drivePort, CANSparkMaxLowLevel.MotorType.kBrushless);
            turnNEO = new CANSparkMax(this.turnPort, CANSparkMaxLowLevel.MotorType.kBrushless);

            neoDriveEncoder = driveNEO.getEncoder();
            neoTurnEncoder = turnNEO.getEncoder();

            neoDrivePIDController = driveNEO.getPIDController();
            neoTurnPIDController = turnNEO.getPIDController();
        }

        this.configMotors();

        feedforward = new SimpleMotorFeedforward(config.getSettings().driveKS, config.getSettings().driveKV, config.getSettings().driveKA);

        this.lastAngle = getState().angle;
    }

    /**
     * Gets the type of swerve drive.
     */
    public SwerveType getType() {
        return type;
    }

    /**
     * Configures the motors to the correct settings
     */
    public void configMotors() {
        if (this.config.getSettings().getType() == SwerveType.FALCON_500) {
            configTalonMotors();
        } else if (this.config.getSettings().getType() == SwerveType.SPARK_MAX) {
            configNEOMotors();
        }
    }

    private double neoPositionConversionFactor = 1;
    private double neoVelocityConversionFactor = 1;
    
    private double neoTurnPositionConversionFactor = 1;

    /**
     * Configures the NEO motors
     */
    public void configNEOMotors() {
        turnNEO.restoreFactoryDefaults();
        turnNEO.setSmartCurrentLimit((int) Math.round(config.getSettings().turnCurrentLimit.continuousCurrent));
        turnNEO.setSecondaryCurrentLimit((int) Math.round(config.getSettings().secondaryTurnCurrentLimit.continuousCurrent));
        turnNEO.setInverted(config.getSettings().turnInverted);
        turnNEO.setIdleMode(config.getSettings().turnIdleMode);

        neoTurnPositionConversionFactor = (1 / config.getSettings().angleGearRatio) * 360;
        if (!RobotBase.isSimulation()) neoTurnEncoder.setPositionConversionFactor(neoTurnPositionConversionFactor);

        neoTurnPIDController.setP(config.getSettings().turnPID.kP);
        neoTurnPIDController.setI(config.getSettings().turnPID.kI);
        neoTurnPIDController.setD(config.getSettings().turnPID.kD);
        neoTurnPIDController.setFF(config.getSettings().turnPID.kF);

        //neoTurnPIDController.setFeedbackDevice(neoTurnEncoder);
        
        //Make the NEO turn PID controller wrap around from [0, 2pi]
        neoTurnPIDController.setPositionPIDWrappingEnabled(true);
        neoTurnPIDController.setPositionPIDWrappingMinInput(0);
        neoTurnPIDController.setPositionPIDWrappingMaxInput(2 * Math.PI);

        turnNEO.burnFlash();

        driveNEO.restoreFactoryDefaults();
        driveNEO.setSmartCurrentLimit((int) Math.round(config.getSettings().driveCurrentLimit.continuousCurrent));
        driveNEO.setSecondaryCurrentLimit((int) Math.round(config.getSettings().secondaryDriveCurrentLimit.continuousCurrent));
        driveNEO.setInverted(config.getSettings().driveInverted);
        driveNEO.setIdleMode(config.getSettings().driveIdleMode);
        driveNEO.setOpenLoopRampRate(config.getSettings().driveOpenRampRate);
        driveNEO.setClosedLoopRampRate(config.getSettings().driveClosedRampRate);

        //NOTE: As of 2023-01-08, the position factor is not working correctly in simulation.
        //This is a problem with REV's simulation, not our code.
        //Right now, we're doing the math manually if we're in simulation.
        neoPositionConversionFactor = config.getSettings().driveGearRatio * Math.PI * config.getSettings().wheelDiameter;
        if (!RobotBase.isSimulation()) neoDriveEncoder.setPositionConversionFactor(neoPositionConversionFactor);
        neoVelocityConversionFactor = neoPositionConversionFactor / 60d;
        if (!RobotBase.isSimulation()) neoDriveEncoder.setVelocityConversionFactor(neoVelocityConversionFactor);

        neoDrivePIDController.setP(config.getSettings().drivePID.kP);
        neoDrivePIDController.setI(config.getSettings().drivePID.kI);
        neoDrivePIDController.setD(config.getSettings().drivePID.kD);
        neoDrivePIDController.setFF(config.getSettings().drivePID.kF);

        neoDrivePIDController.setFeedbackDevice(neoDriveEncoder);

        driveNEO.burnFlash();

        resetToAbsolute();
        neoDriveEncoder.setPosition(0);
        
        if (RobotBase.isSimulation()) {
            REVPhysicsSim.getInstance().addSparkMax(driveNEO, DCMotor.getNEO(1));
        }
    }

    /**
     * Configures the TalonFX motors
     * */
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

    /**
     * Configures the CAN angle encoder.
     * */
    public void configAngleEncoder() {
        this.angleCANCoder.setReal(!RobotBase.isSimulation());
        if (RobotBase.isSimulation()) {
            this.angleCANCoder.setCache(0);
        }

        this.angleCANCoder.configFactoryDefault();
        this.angleCANCoder.configAllSettings(this.config.getCanCoderConfiguration());
    }

    /**
     * Resets the encoder to their absolute position.
     * */
    public void resetToAbsolute() {
        double rawDegrees = getRotation().getDegrees() - config.getSettings().angleOffsets[privateID];
        if (this.config.getSettings().getType() == SwerveType.FALCON_500) {
            double absPos = FalconConversions.degreesToFalcon(rawDegrees, config.getSettings().angleGearRatio);
            this.turnTalonFX.setSelectedSensorPosition(absPos);
        } else if (this.config.getSettings().getType() == SwerveType.SPARK_MAX) {
            double newPosition = getAbsoluteAngle().getDegrees() - config.getSettings().angleOffsets[privateID];
            neoTurnEncoder.setPosition(newPosition / (RobotBase.isSimulation() ? neoTurnPositionConversionFactor : 1));
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

        if (this.getType() == SwerveType.FALCON_500) {
            if (Math.abs(speed) < this.config.getSettings().threshold) {
                driveTalonFX.set(TalonFXControlMode.PercentOutput, 0);
            } else {
                driveTalonFX.set(TalonFXControlMode.PercentOutput, this.driveSpeed);
            }
        } else if (this.getType() == SwerveType.SPARK_MAX) {
            driveNEO.set(this.driveSpeed);
        }
    }

    /**
     * Sets the speed of the drive motor using the desired state.
     * @param desiredState The desired state of the swerve module.
     * @param isOpenLoop Whether or not the drive motor should be in open loop.
     * */
    public void setSpeed(SwerveModuleState desiredState, boolean isOpenLoop) {
        if (isOpenLoop) {
            this.setSpeed(desiredState.speedMetersPerSecond / config.getSettings().maxSpeed);
        } else {
            if (getType() == SwerveType.FALCON_500) {
                double velocity = FalconConversions.MPSToFalcon(desiredState.speedMetersPerSecond, config.getSettings().wheelCircumference(), config.getSettings().driveGearRatio);

                this.driveTalonFX.set(
                        TalonFXControlMode.Velocity,
                        velocity,
                        DemandType.ArbitraryFeedForward,
                        feedforward.calculate(desiredState.speedMetersPerSecond)
                );
            } else if (getType() == SwerveType.SPARK_MAX) {
                //Drive neo PID controller takes in the conversion factor
                driveNEO.getPIDController().setReference(
                       desiredState.speedMetersPerSecond / (RobotBase.isSimulation() ? neoVelocityConversionFactor : 1),
                       CANSparkMax.ControlType.kVelocity,
                       0,
                       feedforward.calculate(desiredState.speedMetersPerSecond)
                );
                /*
                driveNEO.getPIDController().setReference(
                        desiredState.speedMetersPerSecond / 
                        CANSparkMax.ControlType.kVelocity
                ); 
                0,
                        feedforward.calculate(desiredState.speedMetersPerSecond)
                 */
            }
        }
    }

    /**
     * Sets the angle of the module to the given angle in the desired state
     * @param desiredState The desired state of the swerve module.
     * */
    public void setAngle(SwerveModuleState desiredState) {
        double angle = desiredState.angle.getDegrees();
        if (Math.abs(desiredState.speedMetersPerSecond) <= this.config.getSettings().relativeThreshold()) {
            angle = lastAngle.getDegrees();
        }

        if (this.getType() == SwerveType.FALCON_500) {
            turnTalonFX.set(ControlMode.Position, FalconConversions.degreesToFalcon(angle, config.getSettings().angleGearRatio));
        } else if (this.getType() == SwerveType.SPARK_MAX) {
            turnNEO.getPIDController().setReference(angle / neoTurnPositionConversionFactor, CANSparkMax.ControlType.kPosition);
        }

        lastAngle = Rotation2d.fromDegrees(angle);
        this.angleCANCoder.setCache(angle);
       // angleCache = Rotation2d.fromDegrees(angle);
    }

    /**
     * Sets angle of the module to the given angle in degrees. Ignores the jittering preventions though.
     * */
    public void setAngle(double angle) {
        this.setAngle(new SwerveModuleState(1, Rotation2d.fromDegrees(angle)));
    }

    /**
     * Sets the angle and speed of the module to the given state.
     * @param state The desired state of the swerve module.
     * @param isOpenLoop Whether the drive motor should be in open loop.
     * */
    public void setDesiredState(SwerveModuleState state, boolean isOpenLoop) {
        //TODO: fix the optimization to make it more efficient
        SwerveModuleState optimizedState = SwerveModuleState.optimize(state, getRotation());

        this.setAngle(optimizedState);
        this.setSpeed(optimizedState, isOpenLoop);

        speedCache = optimizedState.speedMetersPerSecond;
    }

    /**
     * Returns the current speed only if the drive motor is using open loop. Otherwise, just use getState().
     * @return The open loop speed of the drive motor
     * */
    public double getSpeed() {
        return driveSpeed;
    }

    /**
     * Returns the last angle of the turn motor
     * @return The last angle of the turn motor
     * */
    public Rotation2d getLastAngle() {
        return lastAngle;
    }

    /**
     * Returns the current rotation of the module.
     * @return The current rotation of the module.
     * */
    public Rotation2d getRotation() {
        //return RobotBase.isSimulation() ? angleCache : Rotation2d.fromDegrees(angleCANCoder.getAbsolutePosition());
        return Rotation2d.fromDegrees(angleCANCoder.getAbsolutePosition());
    }

    /**
     * Returns the absolute angle of the module.
     * @return The absolute angle of the module.
     * */
    public Rotation2d getAbsoluteAngle() {
        return Rotation2d.fromDegrees(angleCANCoder.getAbsolutePosition());
    }

    /**
     * Returns the angle of the module.
     * @return The angle of the module.
     * */
    private Rotation2d getAngle() {
        //if (RobotBase.isSimulation()) return angleCache;
        if (RobotBase.isSimulation()) return Rotation2d.fromDegrees(angleCANCoder.getAbsolutePosition());

        if (this.getType() == SwerveType.FALCON_500) {
            return Rotation2d.fromDegrees(FalconConversions.falconToDegrees(turnTalonFX.getSelectedSensorPosition(), config.getSettings().angleGearRatio));
        } else if (this.getType() == SwerveType.SPARK_MAX) {
            return Rotation2d.fromDegrees(neoTurnEncoder.getPosition() * (RobotBase.isSimulation() ? neoTurnPositionConversionFactor : 1));
        } else {
            return Rotation2d.fromDegrees(0);
        }
    }

    public SwerveModulePosition getPosition() {
        //First argument is the distance measured by the wheel of the module
        double distanceMeters = 0;
        if (getType() == SwerveType.FALCON_500) {
            //No clue if the falcon works.
            distanceMeters = (
                FalconConversions.falconToMPS(
                    driveTalonFX.getSelectedSensorPosition(), 
                    config.getSettings().wheelCircumference(), 
                    config.getSettings().driveGearRatio
                ) 
                    * config.getSettings().wheelCircumference() 
                    * config.getSettings().driveGearRatio
            );

        } else if (getType() == SwerveType.SPARK_MAX) {
            distanceMeters = neoDriveEncoder.getPosition();
            distanceMeters *= (RobotBase.isSimulation() ? neoPositionConversionFactor : 1);
        }

        return new SwerveModulePosition(
            distanceMeters,
            getAngle()
        );
    }

    /**
     * Returns the speed and angle of the module.
     * @return SwerveModuleState with information.
     * */
    public SwerveModuleState getState() {
        double velocity = 0;
        Rotation2d angle = getAngle();

        if (getType() == SwerveType.FALCON_500) {
            velocity = RobotBase.isSimulation() ? speedCache : FalconConversions.falconToMPS(driveTalonFX.getSelectedSensorVelocity(), config.getSettings().wheelCircumference(), config.getSettings().driveGearRatio);
        } else if (getType() == SwerveType.SPARK_MAX) {
            velocity = RobotBase.isSimulation() ? speedCache : neoDriveEncoder.getVelocity();
        }

        return new SwerveModuleState(velocity, angle);
    }

    @IOMethod(name = "Drive Encoder Position", value_type = IOValue.DOUBLE, method_type = IOMethodType.READ)
    public double getDriveEncoderPosition() {
        if (driveTalonFX == null && driveNEO == null) return 0;
        
        return getType() == SwerveType.FALCON_500 ? driveTalonFX.getSelectedSensorPosition() : neoDriveEncoder.getPosition();
    }

    public String getBaseName() {
        return "Swerve Module " + this.privateID;
    }
}
