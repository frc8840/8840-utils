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
import com.revrobotics.REVLibError;
import com.revrobotics.REVPhysicsSim;
import com.revrobotics.SparkMaxPIDController;
import com.revrobotics.CANSparkMax.IdleMode;
import com.revrobotics.CANSparkMaxLowLevel.PeriodicFrame;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.controller.SimpleMotorFeedforward;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.kinematics.SwerveModulePosition;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import edu.wpi.first.math.system.plant.DCMotor;
import edu.wpi.first.wpilibj.RobotBase;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import frc.team_8840_lib.IO.devices.IOCANCoder;
import frc.team_8840_lib.controllers.specifics.SparkMaxEncoderWrapper;
import frc.team_8840_lib.info.console.Logger;
import frc.team_8840_lib.input.communication.CommunicationManager;
import frc.team_8840_lib.listeners.Robot;
import frc.team_8840_lib.utils.IO.IOAccess;
import frc.team_8840_lib.utils.IO.IOLayer;
import frc.team_8840_lib.utils.IO.IOMethod;
import frc.team_8840_lib.utils.IO.IOMethodType;
import frc.team_8840_lib.utils.IO.IOPermission;
import frc.team_8840_lib.utils.IO.IOValue;
import frc.team_8840_lib.utils.controllers.swerve.CTREConfig;
import frc.team_8840_lib.utils.controllers.swerve.CTREModuleState;
import frc.team_8840_lib.utils.controllers.swerve.SwerveOptimization;
import frc.team_8840_lib.utils.controllers.swerve.SwerveType;
import frc.team_8840_lib.utils.controllers.swerve.conversions.FalconConversions;
import frc.team_8840_lib.utils.math.MathUtils;

/**
 * This class is used to control a swerve module. It is meant to be used with the SwerveDrive class.
 * 
 * Sources:
 * https://www.chiefdelphi.com/t/adapting-364s-base-swerve-drive-code-to-use-neos-for-steering/418402/3
 * Team 364
 * Team 3512
 * 
 * @author Jaiden Grimminck
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
    private SparkMaxEncoderWrapper neoDriveEncoder;
    private SparkMaxEncoderWrapper neoTurnEncoder;

    //Neo PID controllers
    public SparkMaxPIDController neoDrivePIDController = null;
    public SparkMaxPIDController neoTurnPIDController = null;

    //Simulation variables
    private double speedCache = 0;
    //private Rotation2d angleCache = Rotation2d.fromDegrees(0);

    private Rotation2d lastAngle;

    private Rotation2d targetAngle;

    //Angle encoder - used for both NEO and Falcon
    private IOCANCoder angleCANCoder;

    //Feedforward is also used for both
    private SimpleMotorFeedforward feedforward;

    //Private ID is a number between 0 and 3 which is used to identify the module
    private int privateID;

    //Whether to provide power to the motors or not
    public boolean noRun = false;

    //Whether to do the optimization or not
    public boolean doOptimization = true;

    public boolean doSetAngle = true;
    
    public boolean doSetSpeed = true;

    private Rotation2d desiredAngle = Rotation2d.fromDegrees(0);
    private double desiredT = 0;

    /**
     * Get the private ID of the module
     * @return the private ID. Can be 0, 1, 2, or 3, and can be used as the index.
     */
    public int getIndex() {
        return privateID;
    }

    //When it is initialized, it will set this to true
    private boolean isInitialized = false;

    /**
     * Whether the module is ready to be used
     * @return true if it is ready, false if it is not
     */
    public boolean ready() {
        return isInitialized;
    }

    //Whether to do the manual conversion or not for the NEO. If false, it will use the REV API. If true, it will do manual conversion
    private boolean doManualConversion = true;

    /**
     * Constructor for the swerve module
     * @param drivePort - port for the drive motor
     * @param steerPort - port for the steer motor
     * @param encoderPort - port for the encoder
     * @param swerveNum - number between 0 and 3 which is used to identify the module
     * @param config - configuration for the swerve group.
     */
    public SwerveModule(int drivePort, int steerPort, int encoderPort, int swerveNum, CTREConfig config) {
        super();

        this.drivePort = drivePort;
        this.turnPort = steerPort;

        this.type = config.getSettings().getType();

        this.privateID = swerveNum;

        driveSpeed = 0;

        speedCache = 0;

        lastAngle = Rotation2d.fromDegrees(0);

        targetAngle = Rotation2d.fromDegrees(0);

        doManualConversion = config.getSettings().doManualConversion;
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
                    thisModule.driveNEO = new CANSparkMax(thisModule.drivePort, CANSparkMaxLowLevel.MotorType.kBrushless);
                    thisModule.turnNEO = new CANSparkMax(thisModule.turnPort, CANSparkMaxLowLevel.MotorType.kBrushless);
        
                    thisModule.neoDriveEncoder = new SparkMaxEncoderWrapper(driveNEO);
                    thisModule.neoTurnEncoder = new SparkMaxEncoderWrapper(turnNEO);
        
                    thisModule.neoDrivePIDController = driveNEO.getPIDController();
                    thisModule.neoTurnPIDController = turnNEO.getPIDController();
                }
        
                thisModule.configMotors();

                TimerTask finalConfiguration = new TimerTask() {
                    @Override
                    public void run() {
                        thisModule.feedforward = new SimpleMotorFeedforward(config.getSettings().driveKS, config.getSettings().driveKV, config.getSettings().driveKA);
        
                        thisModule.lastAngle = getState().angle;

                        Logger.Log("[" + thisModule.getBaseName() + "] Configured motors.");

                        thisModule.isInitialized = true;
                    }
                };

                TimerTask waitForConfiguration = new TimerTask() {
                    @Override
                    public void run() {
                        if (config.getSettings().getType() == SwerveType.FALCON_500) {
                            if (thisModule.driveTalonFX.getBusVoltage() != 0 || Robot.isSimulation()) {
                                Logger.Log("[" + thisModule.getBaseName() + "] Motors are ready, finalizing configuration...");
                                finalConfiguration.run();
                                this.cancel();
                            }
                        } else if (config.getSettings().getType() == SwerveType.SPARK_MAX) {
                            if (thisModule.neoDriveEncoder.getEncoder() != null && thisModule.neoTurnEncoder.getEncoder() != null) {
                                Logger.Log("[" + thisModule.getBaseName() + "] Motors are ready (encoders initalized), finalizing configuration...");
                                finalConfiguration.run();
                                this.cancel();
                            }
                        }
                    }
                };

                Timer secondTimer = new Timer();
                secondTimer.schedule(waitForConfiguration, 0, 100);
            }
        };  

        double timeStart = System.currentTimeMillis();

        TimerTask awaitForCANCoders = new TimerTask() {
            @Override
            public void run() {
                if ((angleCANCoder.getAbsolutePosition() != 0 || Robot.isSimulation()) || (System.currentTimeMillis() - timeStart) > 5000) {
                    if (System.currentTimeMillis() - timeStart > 5000) {
                        Logger.Log("[" + thisModule.getBaseName() + "] CANCoders are not ready, configuring motors anyway...");
                    }
                    Logger.Log("[" + thisModule.getBaseName() + "] CANCoders are ready (value: " + angleCANCoder.getAbsolutePosition() + "), configuring motors...");
                    configureMotors.run();
                    this.cancel();
                }
            }
        };

        Timer timer = new Timer();

        Logger.Log("[" + this.getBaseName() + "] Setting up CANCoders, and waiting a second until configuring motors...");

        //we want to wait a bit for the CANCoders to be ready
        //funny lil fix for the CANCoder not being ready
        timer.schedule(awaitForCANCoders, Robot.isReal() ? 1000 : 0, 100);
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
    private double neoTurnVelocityConversionFactor = 1;

    /**
     * Configures the NEO motors
     */
    public void configNEOMotors() {
        neoTurnEncoder.setManualConversion(doManualConversion);
        neoDriveEncoder.setManualConversion(doManualConversion);
        
        turnNEO.restoreFactoryDefaults();
        driveNEO.restoreFactoryDefaults();

        //Set feedback device
        neoTurnPIDController.setFeedbackDevice(neoTurnEncoder.getEncoder());
        neoDrivePIDController.setFeedbackDevice(neoDriveEncoder.getEncoder());

        //Inverted
        turnNEO.setInverted(config.getSettings().turnInverted);
        driveNEO.setInverted(config.getSettings().getReverseDrive(this.getIndex()) ? !config.getSettings().driveInverted : config.getSettings().driveInverted);

        //Idle mode
        turnNEO.setIdleMode(config.getSettings().turnIdleMode);
        driveNEO.setIdleMode(config.getSettings().driveIdleMode);

        //Set current limits (smart)
        turnNEO.setSmartCurrentLimit((int) Math.round(config.getSettings().turnCurrentLimit.continuousCurrent));
        driveNEO.setSmartCurrentLimit((int) Math.round(config.getSettings().driveCurrentLimit.continuousCurrent));

        //Set current limits (secondary)
        driveNEO.setSecondaryCurrentLimit((int) Math.round(config.getSettings().secondaryDriveCurrentLimit.continuousCurrent));
        turnNEO.setSecondaryCurrentLimit((int) Math.round(config.getSettings().secondaryTurnCurrentLimit.continuousCurrent));

        //set voltage compensation
        turnNEO.enableVoltageCompensation(config.getSettings().voltageCompensation);
        driveNEO.enableVoltageCompensation(config.getSettings().voltageCompensation);

        //Open loop and closed loop ramp rate
        driveNEO.setOpenLoopRampRate(config.getSettings().driveOpenRampRate);
        driveNEO.setClosedLoopRampRate(config.getSettings().driveClosedRampRate);

        //Invert encoder (illegal method, will figure out substitute later)
        //neoTurnEncoder.setInverted(config.getSettings().getEncoderIsReversed(getIndex()));
        
        //Set drive PID controller values
        neoPositionConversionFactor = config.getSettings().driveGearRatio * Math.PI * config.getSettings().wheelDiameter;
        neoDriveEncoder.setPositionConversionFactor(neoPositionConversionFactor);
        neoVelocityConversionFactor = neoPositionConversionFactor / 60d;
        neoDriveEncoder.setVelocityConversionFactor(neoVelocityConversionFactor);

        //Set turn PID controller values
        neoTurnPositionConversionFactor = (1 / config.getSettings().angleGearRatio) * 360;
        neoTurnEncoder.setPositionConversionFactor(neoTurnPositionConversionFactor);
        neoTurnVelocityConversionFactor = neoTurnPositionConversionFactor / 60d;
        neoTurnEncoder.setVelocityConversionFactor(neoTurnVelocityConversionFactor);

        //Set turn PID controller values
        neoTurnPIDController.setP(config.getSettings().turnPID.kP);
        neoTurnPIDController.setI(config.getSettings().turnPID.kI);
        neoTurnPIDController.setD(config.getSettings().turnPID.kD);
        neoTurnPIDController.setFF(config.getSettings().turnPID.kF);
        neoTurnPIDController.setIZone(config.getSettings().turnPID.kIZone);

        //Set drive PID controller values
        neoDrivePIDController.setP(config.getSettings().drivePID.kP);
        neoDrivePIDController.setI(config.getSettings().drivePID.kI);
        neoDrivePIDController.setD(config.getSettings().drivePID.kD);
        neoDrivePIDController.setFF(config.getSettings().drivePID.kF);
        neoDrivePIDController.setIZone(config.getSettings().drivePID.kIZone);
        
        //Make the NEO turn PID controller wrap around from [0, 2pi]
        neoTurnPIDController.setPositionPIDWrappingEnabled(true);
        neoTurnPIDController.setPositionPIDWrappingMinInput(
            neoTurnEncoder.calculatePosition(-180, true)
        );
        neoTurnPIDController.setPositionPIDWrappingMaxInput(
            neoTurnEncoder.calculatePosition(180, true)
        );

        //Burn flash
        driveNEO.burnFlash();
        turnNEO.burnFlash();

        resetToAbsolute();
        neoDriveEncoder.getEncoder().setPosition(0);

        //https://github.com/frc3512/SwerveBot-2022/blob/9d31afd05df6c630d5acb4ec2cf5d734c9093bf8/src/main/java/frc/lib/util/CANSparkMaxUtil.java#L67
        setNeoCANStatusFrames(driveNEO, 10, 20, 500, 500, 500);
        setNeoCANStatusFrames(turnNEO, 10, 500, 20, 500, 500);
        
        if (RobotBase.isSimulation()) {
            REVPhysicsSim.getInstance().addSparkMax(driveNEO, DCMotor.getNEO(1));
            REVPhysicsSim.getInstance().addSparkMax(turnNEO, DCMotor.getNEO(1));
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
        if (this.config.getSettings().getType() == SwerveType.FALCON_500) {
            double rawDegrees = getRotation().getDegrees() - config.getSettings().angleOffsets[privateID];
            double absPos = FalconConversions.degreesToFalcon(rawDegrees, config.getSettings().angleGearRatio);
            Logger.Log("[" + getBaseName() + "] Resetting to absolute position: " + rawDegrees + " degrees.");
            this.turnTalonFX.setSelectedSensorPosition(absPos);
        } else if (this.config.getSettings().getType() == SwerveType.SPARK_MAX) {
            double newPosition = getAbsoluteAngle().getDegrees() - config.getSettings().angleOffsets[privateID];

            if (!config.getSettings().manualOffset && !RobotBase.isSimulation()) {
                Logger.Log("[" + getBaseName() + "] Resetting to absolute position: " + newPosition + " degrees.");
                REVLibError status = neoTurnEncoder.setPosition(-newPosition);
                Logger.Log("[" + getBaseName() + "] Reset to absolute position: " + neoTurnEncoder.getPosition() + " degrees (should be " + newPosition + " degrees. Status: " + status.name() + ")");
            }

            if ((Math.abs(neoTurnEncoder.getPosition() - newPosition) > 0.1 || config.getSettings().manualOffset) && !RobotBase.isSimulation()) {
                neoTurnEncoder.setPosition(0);
                
                if (Math.abs(neoTurnEncoder.getPosition()) > 0.1) {
                    Logger.Log("[" + getBaseName() + "] Reset Drive Encoder to 0 failed, setting manual subtraction.");
                    neoTurnEncoder.doSubtractionOfStart(config.getSettings().manualOffset);
                }

                double newPos = angleCANCoder.getAbsolutePosition() - config.getSettings().angleOffsets[privateID];

                if (!config.getSettings().manualOffset) {
                    Logger.Log("[" + getBaseName() + "] Spark Maxes are not working. Doing the manual offsetting...");
                } else {
                    Logger.Log("[" + getBaseName() + "] Doing manual offsetting (based on setting)...");
                    Logger.Log("[" + getBaseName() + "] Angle Offset: " + config.getSettings().angleOffsets[privateID] + " degrees, current position: " + angleCANCoder.getAbsolutePosition() + " degrees. New position: " + newPos + " degrees.");
                }

                neoTurnEncoder.setManualOffset(true);
                neoTurnEncoder.setPosition(
                    newPos
                );

                Logger.Log("[" + getBaseName() + "] " + (!config.getSettings().manualOffset ? "TRY 2 " : "") + "Reset to absolute position: " + neoTurnEncoder.getPosition() + " degrees " + (!config.getSettings().manualOffset ? "(should be " + newPosition + " degrees.)" : "(should be " + config.getSettings().angleOffsets[privateID] + " degrees.)"));
            }

            if (RobotBase.isSimulation()) {
                Logger.Log("[" + getBaseName() + "] Skipped configuration of Spark Max encoder start position due to simulation.");
            }
        }
    }

    /**
     * Sets the brake for both the drive and turn motors
     * @param brake The brake mode. True is brake, false is coast.
     */
    public void setBrakeMode(boolean brake) {
        if (this.config.getSettings().getType() == SwerveType.FALCON_500) {
            this.turnTalonFX.setNeutralMode(brake ? NeutralMode.Brake : NeutralMode.Coast);
            this.driveTalonFX.setNeutralMode(brake ? NeutralMode.Brake : NeutralMode.Coast);
        } else if (this.config.getSettings().getType() == SwerveType.SPARK_MAX) {
            this.turnNEO.setIdleMode(brake ? IdleMode.kBrake : IdleMode.kCoast);
            this.driveNEO.setIdleMode(brake ? IdleMode.kBrake : IdleMode.kCoast);
        }
    }

    /**
     * Sets the individual brake modes for the drive and turn motors
     * @param driveBrake The brake mode for the drive motor. True is brake, false is coast.
     * @param turnBrake The brake mode for the turn motor. True is brake, false is coast.
     */
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
            CommunicationManager.getInstance().updateInfo(
                getBaseName(), "set_drive", speed
            );
            if (!noRun) driveNEO.set(speed);
        }
    }

    /**
     * Sets the NEO CAN Status Frames
     * @param m_motor The motor to set the status frames for
     * @param CANStatus0 The status frame 0 period
     * @param CANStatus1 The status frame 1 period
     * @param CANStatus2 The status frame 2 period
     * @param CANStatus3 The status frame 3 period
     * @param CANStatus4 The status frame 4 period
     */
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
                if (Math.abs(desiredState.speedMetersPerSecond) < this.config.getSettings().relativeThreshold()) {
                    if (!noRun) neoDrivePIDController.setReference(0, CANSparkMax.ControlType.kVelocity);
                    return;
                }

                //Drive neo PID controller takes in the conversion factor
                if (!noRun) {
                    neoDrivePIDController.setReference(
                        neoDriveEncoder.calculateVelocity(desiredState.speedMetersPerSecond),
                        CANSparkMax.ControlType.kVelocity,
                        0,
                        feedforward.calculate(neoDriveEncoder.calculateVelocity(desiredState.speedMetersPerSecond))
                    );
                }
            }
        }
    }

    /**
     * Sets the angle of the module to the given angle in the desired state
     * @param desiredState The desired state of the swerve module.
     * @param ignoreAngleLimit Whether or not to ignore the angle limit.
     * */
    public void setAngle(SwerveModuleState desiredState, boolean ignoreAngleLimit) {
        double angle = desiredState.angle.getDegrees();

        // if (Math.abs(desiredState.speedMetersPerSecond) <= this.config.getSettings().relativeThreshold() && !ignoreSpeedRequirement) {
        //     angle = lastAngle.getDegrees();
        // }

        double difference = Math.abs(angle - lastAngle.getDegrees());
        
        if (difference > 179 && !ignoreAngleLimit) { //Stop jittering
            //Logger.Log("[" + getBaseName() + "] Angle difference is too large! (" + difference + ")");
            return;
        }

        if (difference < 0.2 && !ignoreAngleLimit) {
            return;
        }

        if (this.getType() == SwerveType.FALCON_500) {
            turnTalonFX.set(ControlMode.Position, FalconConversions.degreesToFalcon(angle, config.getSettings().angleGearRatio));
        } else if (this.getType() == SwerveType.SPARK_MAX) {
            CommunicationManager.getInstance().updateInfo(getBaseName(), "set_angle", neoTurnEncoder.calculatePosition(angle));
            CommunicationManager.getInstance().updateInfo(getBaseName(), "angle", angle);
            if (!noRun) {
                neoTurnPIDController.setReference(
                    neoTurnEncoder.calculatePosition(angle),
                    CANSparkMax.ControlType.kPosition,
                    0
                );
            }
        }

        lastAngle = Rotation2d.fromDegrees(angle);
        this.angleCANCoder.setCache(angle);
       // angleCache = Rotation2d.fromDegrees(angle);
    }

    /**
     * Sets the angle of the module to the given angle in the desired state
     * @param desiredState The desired state of the swerve module.
     */
    public void setAngle(SwerveModuleState desiredState) {
        this.setAngle(desiredState, false);
    } 

    /**
     * Sets angle of the module to the given angle in degrees. Ignores the jittering preventions though.
     * @param angle The angle to set the module to in degrees.
     * */
    public void setAngle(double angle) {
        this.setAngle(new SwerveModuleState(0, Rotation2d.fromDegrees(angle)), true);
    }

    private Rotation2d lastSetAngle = Rotation2d.fromDegrees(0);
    private double lastSetSpeed = 0;

    /**
     * Sets the angle and speed of the module to the given state.
     * @param state The desired state of the swerve module.
     * @param isOpenLoop Whether the drive motor should be in open loop.
     * */
    public void setDesiredState(SwerveModuleState state, boolean isOpenLoop) {
        SwerveModuleState optimizedState = doOptimization ? CTREModuleState.optimize(state, lastSetAngle) : state;

        double difference = Math.abs(optimizedState.angle.getDegrees() - lastSetAngle.getDegrees());

        boolean isActualDifference = true;

        difference = difference % 360;
        if (difference > 179) {
            isActualDifference = false; //Stop the module from jittering
        }
        //Logger.Log("[" + getBaseName() + "] Difference: " + difference + ", " + isActualDifference);

        if (this.doSetAngle && isActualDifference) this.setAngle(optimizedState);

        double speedDifference = Math.abs(optimizedState.speedMetersPerSecond - lastSetSpeed);
        boolean sameSpeed = speedDifference < 0.01;

        if (this.doSetSpeed && !sameSpeed) this.setSpeed(optimizedState, isOpenLoop);

        lastSetSpeed = optimizedState.speedMetersPerSecond;
        lastSetAngle = optimizedState.angle;

 
        speedCache = optimizedState.speedMetersPerSecond;
    }

    /**
     * Sets the angle and speed of the module to the given state.
     * @param state The desired state of the swerve module.
     * @param isOpenLoop Whether the drive motor should be in open loop.
     * @param ignoreAngleLimit Whether or not to ignore the angle limit.
     * */
    public void setDesiredState(SwerveModuleState state, boolean isOpenLoop, boolean ignoreAngleLimit) {
        SwerveModuleState optimizedState = state;

        if (this.doSetAngle) this.setAngle(optimizedState, ignoreAngleLimit);

        double speedDifference = Math.abs(optimizedState.speedMetersPerSecond - lastSetSpeed);
        boolean sameSpeed = speedDifference < 0.01;

        if (this.doSetSpeed && !sameSpeed) this.setSpeed(optimizedState, isOpenLoop);

        lastSetSpeed = optimizedState.speedMetersPerSecond;
        lastSetAngle = optimizedState.angle;

 
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
            if (turnTalonFX == null) {
                Logger.Log("[" + getBaseName() + "] WARNING: Turn TalonFX is null!");
                return Rotation2d.fromDegrees(0);
            }
            return Rotation2d.fromDegrees(FalconConversions.falconToDegrees(turnTalonFX.getSelectedSensorPosition(), config.getSettings().angleGearRatio));
        } else if (this.getType() == SwerveType.SPARK_MAX) {
            if (neoTurnEncoder == null) {
                Logger.Log("[" + getBaseName() + "] WARNING: NEO Turn Encoder is null!");
                return Rotation2d.fromDegrees(0);
            }
            return Rotation2d.fromDegrees(neoTurnEncoder.getPosition());
        } else {
            return Rotation2d.fromDegrees(0);
        }
    }

    /**
     * Returns the current position of the module.
     * @return The current position of the module.
     */
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
        }

        return new SwerveModulePosition(
            distanceMeters,
            getAngle()
        );
    }

    /**
     * Returns the raw return value of the turn encoder.
     * @return The raw return value of the turn encoder.
     */
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

    /**
     * Returns the raw return value of the drive encoder.
     * @return The raw return value of the drive encoder.
     */
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

    /**
     * Returns the raw velocity value of the drive encoder.
     * @return The raw velocity value of the drive encoder.
     */
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
            if (neoDriveEncoder == null) {
                Logger.Log("[" + getBaseName() + "] WARNING: NEO Drive Encoder is null!");
            } else {
                velocity = RobotBase.isSimulation() ? speedCache : neoDriveEncoder.getVelocity();
            }
        }

        return new SwerveModuleState(velocity, angle);
    }
    
    /**
     * Get encoder for the drive motor
     * @return The encoder for the drive motor
     */
    public SparkMaxEncoderWrapper getDriveEncoder() {
        return neoDriveEncoder;
    }

    /**
     * Get encoder for the turn motor
     * @return The encoder for the turn motor
     */
    public SparkMaxEncoderWrapper getTurnEncoder() {
        return neoTurnEncoder;
    }

    /**
     * Returns the values of the module in a string.
     * @return The values of the module in a string.
     */
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

    /**
     * Returns the swerve module name for the IO system.
     * @return The base name of the module for IO.
     */
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
