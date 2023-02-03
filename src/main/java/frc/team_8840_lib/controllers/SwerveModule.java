package frc.team_8840_lib.controllers;

import java.util.Timer;
import java.util.TimerTask;

import com.ctre.phoenix.motorcontrol.ControlMode;
import com.ctre.phoenix.motorcontrol.DemandType;
import com.ctre.phoenix.motorcontrol.NeutralMode;
import com.ctre.phoenix.motorcontrol.TalonFXControlMode;
import com.ctre.phoenix.motorcontrol.can.TalonFX;
import com.revrobotics.CANSparkMax;
import com.revrobotics.CANSparkMaxLowLevel;
import com.revrobotics.REVPhysicsSim;
import com.revrobotics.RelativeEncoder;
import com.revrobotics.SparkMaxPIDController;
import com.revrobotics.CANSparkMax.IdleMode;
import com.revrobotics.CANSparkMaxLowLevel.PeriodicFrame;

import edu.wpi.first.math.controller.SimpleMotorFeedforward;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.kinematics.SwerveModulePosition;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import edu.wpi.first.math.system.plant.DCMotor;
import edu.wpi.first.wpilibj.RobotBase;
import frc.team_8840_lib.IO.devices.IOCANCoder;
import frc.team_8840_lib.info.console.Logger;
import frc.team_8840_lib.listeners.Robot;
import frc.team_8840_lib.utils.IO.IOAccess;
import frc.team_8840_lib.utils.IO.IOLayer;
import frc.team_8840_lib.utils.IO.IOMethod;
import frc.team_8840_lib.utils.IO.IOMethodType;
import frc.team_8840_lib.utils.IO.IOPermission;
import frc.team_8840_lib.utils.IO.IOValue;
import frc.team_8840_lib.utils.controllers.swerve.CTREConfig;
import frc.team_8840_lib.utils.controllers.swerve.SwerveType;
import frc.team_8840_lib.utils.controllers.swerve.conversions.FalconConversions;
import frc.team_8840_lib.utils.controllers.swerve.structs.PIDStruct;

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

    private boolean doManualConversion = true;

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

        SwerveModule thisModule = this;

        TimerTask configureMotors = new TimerTask() {
            @Override
            public void run() {
                if (config.getSettings().getType() == SwerveType.FALCON_500) {
                    thisModule.driveTalonFX = new TalonFX(thisModule.drivePort);
                    thisModule.turnTalonFX = new TalonFX(thisModule.turnPort);
                } else if (config.getSettings().getType() == SwerveType.SPARK_MAX) {
                    driveNEO = new CANSparkMax(thisModule.drivePort, CANSparkMaxLowLevel.MotorType.kBrushless);
                    turnNEO = new CANSparkMax(thisModule.turnPort, CANSparkMaxLowLevel.MotorType.kBrushless);
        
                    neoDriveEncoder = driveNEO.getEncoder();
                    neoTurnEncoder = turnNEO.getEncoder();
        
                    neoDrivePIDController = driveNEO.getPIDController();
                    neoTurnPIDController = turnNEO.getPIDController();
                }
        
                thisModule.configMotors();
        
                feedforward = new SimpleMotorFeedforward(config.getSettings().driveKS, config.getSettings().driveKV, config.getSettings().driveKA);
        
                thisModule.lastAngle = getState().angle;

                Logger.Log("[" + thisModule.getBaseName() + "] Configured motors.");
            }
        };

        TimerTask awaitForCANCoders = new TimerTask() {
            @Override
            public void run() {
                if (angleCANCoder.getAbsolutePosition() != 0 || Robot.isSimulation()) {
                    Logger.Log("[" + thisModule.getBaseName() + "] CANCoders are ready, configuring motors...");
                    configureMotors.run();
                    this.cancel();
                }
            }
        };

        Timer timer = new Timer();

        Logger.Log("[" + this.getBaseName() + "] Setting up CANCoders, and waiting a second until configuring motors...");

        //we want to wait a bit for the CANCoders to be ready
        //funny lil fix for the CANCoder not being ready
        timer.schedule(awaitForCANCoders, 0, 100);
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

        //set the brake mode to true or false, just preconfig this
        this.setIndividualBrakeMode(true, false);
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
        if (!doManualConversion) neoTurnEncoder.setPositionConversionFactor(neoTurnPositionConversionFactor);

        neoTurnPIDController.setP(config.getSettings().turnPID.kP);
        neoTurnPIDController.setI(config.getSettings().turnPID.kI);
        neoTurnPIDController.setD(config.getSettings().turnPID.kD);
        neoTurnPIDController.setFF(config.getSettings().turnPID.kF);

        neoTurnPIDController.setFeedbackDevice(neoTurnEncoder);
        
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
        if (!doManualConversion) neoDriveEncoder.setPositionConversionFactor(neoPositionConversionFactor);
        neoVelocityConversionFactor = neoPositionConversionFactor / 60d;
        if (!doManualConversion) neoDriveEncoder.setVelocityConversionFactor(neoVelocityConversionFactor);

        neoDrivePIDController.setP(config.getSettings().drivePID.kP);
        neoDrivePIDController.setI(config.getSettings().drivePID.kI);
        neoDrivePIDController.setD(config.getSettings().drivePID.kD);
        neoDrivePIDController.setFF(config.getSettings().drivePID.kF);

        neoDrivePIDController.setFeedbackDevice(neoDriveEncoder);

        driveNEO.burnFlash();

        resetToAbsolute();
        neoDriveEncoder.setPosition(0);

        //https://github.com/frc3512/SwerveBot-2022/blob/9d31afd05df6c630d5acb4ec2cf5d734c9093bf8/src/main/java/frc/lib/util/CANSparkMaxUtil.java#L67
        setNeoCANStatusFrames(driveNEO, 10, 20, 500, 500, 500);
        setNeoCANStatusFrames(turnNEO, 10, 500, 20, 500, 500);
        
        if (RobotBase.isSimulation()) {
            REVPhysicsSim.getInstance().addSparkMax(driveNEO, DCMotor.getNEO(1));
            REVPhysicsSim.getInstance().addSparkMax(turnNEO, DCMotor.getNEO(1));
        }
    }

    public void updateNeoDrivePID(PIDStruct pid) {
        neoDrivePIDController.setP(pid.kP);
        neoDrivePIDController.setI(pid.kI);
        neoDrivePIDController.setD(pid.kD);
        neoDrivePIDController.setFF(pid.kF);
    }

    public void updateNeoTurnPID(PIDStruct pid) {
        neoTurnPIDController.setP(pid.kP);
        neoTurnPIDController.setI(pid.kI);
        neoTurnPIDController.setD(pid.kD);
        neoTurnPIDController.setFF(pid.kF);
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
            neoTurnEncoder.setPosition(newPosition / (doManualConversion ? neoTurnPositionConversionFactor : 1));
        }
    }

    public void setBrakeMode(boolean brake) {
        if (this.config.getSettings().getType() == SwerveType.FALCON_500) {
            this.turnTalonFX.setNeutralMode(brake ? NeutralMode.Brake : NeutralMode.Coast);
            this.driveTalonFX.setNeutralMode(brake ? NeutralMode.Brake : NeutralMode.Coast);
        } else if (this.config.getSettings().getType() == SwerveType.SPARK_MAX) {
            this.turnNEO.setIdleMode(brake ? IdleMode.kBrake : IdleMode.kCoast);
            this.driveNEO.setIdleMode(brake ? IdleMode.kBrake : IdleMode.kCoast);
        }
    }

    public void setIndividualBrakeMode(boolean driveBrake, boolean turnBrake) {
        if (this.config.getSettings().getType() == SwerveType.FALCON_500) {
            this.turnTalonFX.setNeutralMode(turnBrake ? NeutralMode.Brake : NeutralMode.Coast);
            this.driveTalonFX.setNeutralMode(driveBrake ? NeutralMode.Brake : NeutralMode.Coast);
        } else if (this.config.getSettings().getType() == SwerveType.SPARK_MAX) {
            this.turnNEO.setIdleMode(turnBrake ? IdleMode.kBrake : IdleMode.kCoast);
            this.driveNEO.setIdleMode(driveBrake ? IdleMode.kBrake : IdleMode.kCoast);
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
                driveTalonFX.set(TalonFXControlMode.PercentOutput, speed);
            }
        } else if (this.getType() == SwerveType.SPARK_MAX) {
            driveNEO.set(speed);
        }
    }

    public void setNeoCANStatusFrames(CANSparkMax m_motor, int CANStatus0, int CANStatus1, int CANStatus2, int CANStatus3, int CANStatus4)
    {
        m_motor.setPeriodicFramePeriod(PeriodicFrame.kStatus0, CANStatus0);
        m_motor.setPeriodicFramePeriod(PeriodicFrame.kStatus1, CANStatus1);
        m_motor.setPeriodicFramePeriod(PeriodicFrame.kStatus2, CANStatus2);
        m_motor.setPeriodicFramePeriod(PeriodicFrame.kStatus3, CANStatus3);
        m_motor.setPeriodicFramePeriod(PeriodicFrame.kStatus4, CANStatus4);
        //  https://docs.revrobotics.com/sparkmax/operating-modes/control-interfaces
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
                neoDrivePIDController.setReference(
                       desiredState.speedMetersPerSecond / (doManualConversion ? neoVelocityConversionFactor : 1),
                       CANSparkMax.ControlType.kVelocity,
                       0,
                       feedforward.calculate(desiredState.speedMetersPerSecond / (doManualConversion ? neoVelocityConversionFactor : 1))
                );
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
            if (turnTalonFX == null) return Rotation2d.fromDegrees(0);
            return Rotation2d.fromDegrees(FalconConversions.falconToDegrees(turnTalonFX.getSelectedSensorPosition(), config.getSettings().angleGearRatio));
        } else if (this.getType() == SwerveType.SPARK_MAX) {
            if (neoTurnEncoder == null) return Rotation2d.fromDegrees(0);
            return Rotation2d.fromDegrees(neoTurnEncoder.getPosition() * (doManualConversion ? neoTurnPositionConversionFactor : 1));
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
                    driveTalonFX == null ? 0 : driveTalonFX.getSelectedSensorPosition(), 
                    config.getSettings().wheelCircumference(), 
                    config.getSettings().driveGearRatio
                ) 
                    * config.getSettings().wheelCircumference() 
                    * config.getSettings().driveGearRatio
            );

        } else if (getType() == SwerveType.SPARK_MAX) {
            distanceMeters = neoDriveEncoder == null ? 0 : neoDriveEncoder.getPosition();
            distanceMeters *= (doManualConversion ? neoPositionConversionFactor : 1);
        }

        return new SwerveModulePosition(
            distanceMeters,
            getAngle()
        );
    }

    public double getRawTurnPosition() {
        if (getType() == SwerveType.FALCON_500) {
            if (turnTalonFX == null) return 0;
            return turnTalonFX.getSelectedSensorPosition();
        } else if (getType() == SwerveType.SPARK_MAX) {
            if (neoTurnEncoder == null) return 0;
            return neoTurnEncoder.getPosition();
        } else {
            return 0;
        }
    }

    public double getRawDrivePosition() {
        if (getType() == SwerveType.FALCON_500) {
            if (driveTalonFX == null) return 0;
            return driveTalonFX.getSelectedSensorPosition();
        } else if (getType() == SwerveType.SPARK_MAX) {
            if (neoDriveEncoder == null) return 0;
            return neoDriveEncoder.getPosition();
        } else {
            return 0;
        }
    }

    public double getRawDriveVelocity() {
        if (getType() == SwerveType.FALCON_500) {
            if (driveTalonFX == null) return 0;
            return driveTalonFX.getSelectedSensorPosition();
        } else if (getType() == SwerveType.SPARK_MAX) {
            if (neoDriveEncoder == null) return 0;
            return neoDriveEncoder.getVelocity();
        } else {
            return 0;
        }
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
            velocity = RobotBase.isSimulation() ? speedCache : neoDriveEncoder.getVelocity() * (doManualConversion ? neoVelocityConversionFactor : 1);
        }

        return new SwerveModuleState(velocity, angle);
    }

    @IOMethod(name = "DriveData", value_type = IOValue.STRING, method_type = IOMethodType.READ)
    public String getDriveData() {
        if (driveTalonFX == null && driveNEO == null) return "";

        int type;
        int id;
        double rawDrivePosition;
        double rawDriveVelocity;
        double driveConversionFactor;
        double driveVelocityConversionFactor;
        double rawTurnPosition;
        double turnConversionFactor;
        double canCoderPosition;
        double velocity;
        double angle;

        type = this.getType().ordinal();
        id = this.privateID;

        rawDrivePosition = this.getRawDrivePosition();
        rawDriveVelocity = this.getRawDriveVelocity();

        driveConversionFactor = this.doManualConversion ? this.neoPositionConversionFactor : 1;
        driveVelocityConversionFactor = this.doManualConversion ? this.neoVelocityConversionFactor : 1;

        rawTurnPosition = this.getRawTurnPosition();
        turnConversionFactor = this.doManualConversion ? this.neoTurnPositionConversionFactor : 1;

        canCoderPosition = this.angleCANCoder.getAbsolutePosition();

        velocity = this.getState().speedMetersPerSecond;
        angle = this.getAngle().getDegrees();

        return type + "," + id + "," + rawDrivePosition + "," + rawDriveVelocity + "," + driveConversionFactor + "," + driveVelocityConversionFactor + "," + rawTurnPosition + "," + turnConversionFactor + "," + canCoderPosition + "," + velocity + "," + angle;
    }

    public String getBaseName() {
        return "Swerve Module " + this.privateID;
    }

    /**
     * Returns the raw CANSparkMax objects.
     * @return CANSparkMax[] with the drive and turn motors [driveController, turnController]
     */
    public CANSparkMax[] getRawNEO() {
        return new CANSparkMax[] {driveNEO, turnNEO};
    }

    /**
     * Returns the raw TalonFX objects.
     * @return TalonFX[] with the drive and turn motors [driveController, turnController]
     */
    public TalonFX[] getRawFalcon() {
        return new TalonFX[] {driveTalonFX, turnTalonFX};
    }
}
