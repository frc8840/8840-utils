package frc.team_8840_lib.controllers;

import com.ctre.phoenix.sensors.AbsoluteSensorRange;
import com.ctre.phoenix.sensors.CANCoderConfiguration;
import com.ctre.phoenix.sensors.SensorInitializationStrategy;
import com.ctre.phoenix.sensors.SensorTimeBase;
import com.revrobotics.CANSparkMax;
import com.revrobotics.CANSparkBase.ControlType;
import com.revrobotics.CANSparkLowLevel.MotorType;
import com.revrobotics.CANSparkLowLevel.PeriodicFrame;
import com.revrobotics.REVPhysicsSim;
import com.revrobotics.SparkPIDController;

import edu.wpi.first.math.controller.SimpleMotorFeedforward;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.kinematics.SwerveModulePosition;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import edu.wpi.first.math.system.plant.DCMotor;
import edu.wpi.first.wpilibj.RobotBase;
import frc.team_8840_lib.IO.devices.IOCANCoder;
import frc.team_8840_lib.controllers.specifics.SparkMaxEncoderWrapper;
import frc.team_8840_lib.info.console.Logger;
import frc.team_8840_lib.listeners.Robot;
import frc.team_8840_lib.utils.async.Promise;
import frc.team_8840_lib.utils.controllers.swerve.CTREModuleState;
import frc.team_8840_lib.utils.controllers.swerve.ModuleConfig;
import frc.team_8840_lib.utils.controllers.swerve.SwerveSettings;
import frc.team_8840_lib.utils.math.units.Unit;
import frc.team_8840_lib.utils.math.units.Unit.Type;

public class SwerveModule {
    public static enum Position {
        FRONT_LEFT, FRONT_RIGHT, BACK_LEFT, BACK_RIGHT;
    }

    private SwerveSettings m_settings;
    private ModuleConfig m_config;
    private Position m_position;

    private CANSparkMax m_driveMotor;
    private CANSparkMax m_turnMotor;
    private IOCANCoder m_encoder;

    private SparkMaxEncoderWrapper m_driveEncoderWrapper;
    private SparkMaxEncoderWrapper m_turnEncoderWrapper;

    private SparkPIDController m_drivePIDController;
    private SparkPIDController m_turnPIDController;

    private Rotation2d m_lastDesiredAngle;
    private Unit m_lastDesiredSpeed;
    private SimpleMotorFeedforward m_feedforward;

    private double m_drivePositionConversionFactor;
    private double m_driveVelocityConversionFactor;

    private double m_turnPositionConversionFactor;
    private double m_turnVelocityConversionFactor;

    private boolean m_isInitialized = false;

    /**
     * Creates a new swerve module
     * @param settings The settings for the swerve drive itself (gyro reversed, etc)
     * @param config The config for the swerve module (motor IDs, encoder IDs, etc)
     * @param position The position of the swerve module (front left, front right, back left, back right)
     */
    public SwerveModule(SwerveSettings settings, ModuleConfig config, Position position) {
        super();

        m_settings = settings;

        m_config = config;
        m_position = position;

        //declare the motors
        m_driveMotor = new CANSparkMax(m_config.getDriveMotorID(), MotorType.kBrushless);
        m_turnMotor = new CANSparkMax(m_config.getTurnMotorID(), MotorType.kBrushless);

        //declare the encoder
        m_encoder = new IOCANCoder(m_config.getEncoderID());

        //blah blah blah some simulation stuff
        m_encoder.setReal(Robot.isReal());
        if (Robot.isSimulation()) m_encoder.setCache(0);

        m_driveEncoderWrapper = new SparkMaxEncoderWrapper(m_driveMotor, "Swerve-" + this.m_position.name() + "-Drive");
        m_turnEncoderWrapper = new SparkMaxEncoderWrapper(m_turnMotor, "Swerve-" + this.m_position.name() + "-Turn");

        m_drivePIDController = m_driveMotor.getPIDController();
        m_turnPIDController = m_turnMotor.getPIDController();

        m_feedforward = new SimpleMotorFeedforward(settings.driveKS, settings.driveKV, settings.driveKA);

        int startInitialization = (int) System.currentTimeMillis();

        //a promise to go step by step through the initialization process
        //to make sure that everything is ready before we start
        //(similar to a promise in javascript)
        new Promise((res, rej) -> {
            //First, wait for CANCoders to be ready
            configCANCoder();

            Promise.WaitThen(() -> {
                return m_encoder.getAbsolutePosition() != 0 || Robot.isSimulation();
            }, res, rej, 10);
        }).then((res, rej) -> {
            //Wait for the motors to be ready
            Promise configPromise = configMotors();

            Promise.WaitThen(() -> {
                return configPromise.resolved();
            }, res, rej, 10);
        }).then((res, rej) -> {
            //Set the last angle.

            m_lastDesiredSpeed = new Unit(0, Type.METERS);
            m_lastDesiredAngle = getAngle();

            res.run();
        }).finish((res, rej) -> {
            int initializationTime = (int) System.currentTimeMillis() - startInitialization;
            Logger.Log(this.m_position.name() + " Swerve Module", "Initialized in " + (initializationTime / 1000) + " seconds!");

            m_isInitialized = true;
        });
    }

    /**
     * Configures the CANCoder settings (ran through the IOCANCoder class, but is the same as what you would do with a normal CANCoder)
     */
    @SuppressWarnings("deprecation")
    public void configCANCoder() {
        m_encoder.configFactoryDefault();

        CANCoderConfiguration encoderConfig = new CANCoderConfiguration();

        //setup the settings of the cancoder
        encoderConfig.absoluteSensorRange = AbsoluteSensorRange.Unsigned_0_to_360;
        encoderConfig.sensorDirection = m_settings.canCoderInverted;
        encoderConfig.initializationStrategy = SensorInitializationStrategy.BootToAbsolutePosition;
        encoderConfig.sensorTimeBase = SensorTimeBase.PerSecond;

        //send the settings to the cancoder
        m_encoder.configAllSettings(encoderConfig);
    }

    /**
     * Configure the settings of the motors
     * @return a promise that will be resolved (finished) when the motors are ready
     */
    public Promise configMotors() {
        m_turnEncoderWrapper.setManualConversion(m_settings.doManualConversion);
        m_driveEncoderWrapper.setManualConversion(m_settings.doManualConversion);
        
        m_turnMotor.restoreFactoryDefaults();
        m_driveMotor.restoreFactoryDefaults();

        //Set feedback device
        m_turnPIDController.setFeedbackDevice(m_turnEncoderWrapper.getEncoder());
        m_drivePIDController.setFeedbackDevice(m_driveEncoderWrapper.getEncoder());

        //Inverted
        m_turnMotor.setInverted(m_settings.turnInverted);
        m_driveMotor.setInverted(m_settings.driveInverted);

        //Idle mode
        m_turnMotor.setIdleMode(m_settings.turnIdleMode);
        m_driveMotor.setIdleMode(m_settings.driveIdleMode);

        //Set current limits (smart)
        m_turnMotor.setSmartCurrentLimit((int) Math.round(m_settings.turnCurrentLimit.continuousCurrent));
        m_driveMotor.setSmartCurrentLimit((int) Math.round(m_settings.driveCurrentLimit.continuousCurrent));

        //Set current limits (secondary)
        m_driveMotor.setSecondaryCurrentLimit((int) Math.round(m_settings.secondaryDriveCurrentLimit.continuousCurrent));
        m_turnMotor.setSecondaryCurrentLimit((int) Math.round(m_settings.secondaryTurnCurrentLimit.continuousCurrent));

        //set voltage compensation
        m_turnMotor.enableVoltageCompensation(m_settings.voltageCompensation);
        m_driveMotor.enableVoltageCompensation(m_settings.voltageCompensation);

        //Open loop and closed loop ramp rate
        m_driveMotor.setOpenLoopRampRate(m_settings.driveOpenRampRate);
        m_driveMotor.setClosedLoopRampRate(m_settings.driveClosedRampRate);

        //Set drive PID controller values
        m_drivePositionConversionFactor = m_settings.driveGearRatio * Math.PI * m_settings.wheelDiameter;
        m_driveEncoderWrapper.setPositionConversionFactor(m_drivePositionConversionFactor);
        m_driveVelocityConversionFactor = m_drivePositionConversionFactor / 60d;
        m_driveEncoderWrapper.setVelocityConversionFactor(m_driveVelocityConversionFactor);

        //Set turn PID controller values
        m_turnPositionConversionFactor = (1 / m_settings.angleGearRatio) * 360;
        m_turnEncoderWrapper.setPositionConversionFactor(m_turnPositionConversionFactor);
        m_turnVelocityConversionFactor = m_turnPositionConversionFactor / 60d;
        m_turnEncoderWrapper.setVelocityConversionFactor(m_turnVelocityConversionFactor);

        //Set turn PID controller values
        m_turnPIDController.setP(m_settings.turnPID.kP);
        m_turnPIDController.setI(m_settings.turnPID.kI);
        m_turnPIDController.setD(m_settings.turnPID.kD);
        m_turnPIDController.setFF(m_settings.turnPID.kF);
        m_turnPIDController.setIZone(m_settings.turnPID.kIZone);

        //Set drive PID controller values
        m_drivePIDController.setP(m_settings.drivePID.kP);
        m_drivePIDController.setI(m_settings.drivePID.kI);
        m_drivePIDController.setD(m_settings.drivePID.kD);
        m_drivePIDController.setFF(m_settings.drivePID.kF);
        m_drivePIDController.setIZone(m_settings.drivePID.kIZone);
        
        //Make the NEO turn PID controller wrap around from [0, 2pi]
        m_turnPIDController.setPositionPIDWrappingEnabled(true);
        m_turnPIDController.setPositionPIDWrappingMinInput(
            m_turnEncoderWrapper.calculatePosition(-180, true)
        );
        m_turnPIDController.setPositionPIDWrappingMaxInput(
            m_turnEncoderWrapper.calculatePosition(180, true)
        );

        //Burn flash
        m_driveMotor.burnFlash();
        m_turnMotor.burnFlash();

        return new Promise((res, rej) -> {
            Promise waitFor = resetToAbsolute();
            Promise.WaitThen(() -> { return waitFor.resolved(); }, res, rej, 10);
        }).finish((res, rej) -> {
            Logger.Log(this.m_position.name() + " Swerve Module", "Configured motors!");

            m_driveEncoderWrapper.getEncoder().setPosition(0);

            //https://github.com/frc3512/SwerveBot-2022/blob/9d31afd05df6c630d5acb4ec2cf5d734c9093bf8/src/main/java/frc/lib/util/CANSparkMaxUtil.java#L67
            setNeoCANStatusFrames(m_driveMotor, 10, 20, 500, 500, 500);
            setNeoCANStatusFrames(m_turnMotor, 10, 500, 20, 500, 500);
            
            if (RobotBase.isSimulation()) {
                REVPhysicsSim.getInstance().addSparkMax(m_driveMotor, DCMotor.getNEO(1));
                REVPhysicsSim.getInstance().addSparkMax(m_turnMotor, DCMotor.getNEO(1));
            }

            res.run();
        });  
    }

    /**
     * Sets the angle of the turn encoder to the absolute angle subtracted by the offset so forward is 0°
     * @return A promise that resolves (finishes) when the turn encoder is successfully set to the absolute angle subtracted by the offset
     */
    public Promise resetToAbsolute() {
        m_turnEncoderWrapper.getEncoder().setPosition(0);

        return new Promise((res, rej) -> {
            if (Robot.isSimulation()) {
                m_turnEncoderWrapper.setManualOffset(true);
                m_turnEncoderWrapper.setPosition(0);
                
                res.run();
                return;
            }

            if (m_config.manualOffset) {
                rej.onError(new Exception("Skipped to wrapper setup because manual offset is enabled!"));
                return;
            }

            int start = (int) System.currentTimeMillis();

            double newPosition = getAbsoluteAngle().getDegrees() - m_config.getTurnOffset();

            m_turnEncoderWrapper.setPosition(newPosition);

            Promise.WaitThen(() -> {
                if (Robot.isSimulation()) {
                    return true;
                }

                if (start + 1000 < System.currentTimeMillis()) {
                    //This means that it failed to reset to absolute
                    Logger.Log(m_position.name() + " Swerve Module", "Failed to reset to absolute!");

                    throw new RuntimeException("Failed to reset to absolute!");
                }

                return Math.abs(m_turnEncoderWrapper.getEncoder().getPosition() - newPosition) < 0.1 || Robot.isSimulation();
            }, res, rej, 10);
        }).then((res, rej) -> {
            Logger.Log(m_position.name() + " Swerve Module", "Successfully reset to absolute through REV API! " + (Robot.isSimulation() ? "(Process fast due to simulation.)" : ""));

            res.run();
        }).catch_err((e) -> {
            m_turnEncoderWrapper.doSubtractionOfStart(true);

            double newPosition = getAbsoluteAngle().getDegrees() - m_config.getTurnOffset();

            m_turnEncoderWrapper.setManualOffset(true);
            m_turnEncoderWrapper.setPosition(newPosition);

            Logger.Log(
                m_position.name() + " Swerve Module",
                "Was unable to reset to absolute through REV API, using fallback method! " +
                "(Confirmation: " + m_turnEncoderWrapper.getPosition() + " should be equal to 0!)"
            );
        });
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
     * Sets the speed of the drive motor
     * @param speed The speed to set the motor to. Assume the units are per second
     * @param openLoop Whether or not to use open loop control
     */
    public void setSpeed(Unit speed, boolean openLoop) {
        setSpeed(speed, openLoop, false);
    }

    /**
     * Sets the speed of the drive motor
     * @param speed The speed to set the motor to. Assume the units are per second
     * @param openLoop Whether or not to use open loop control
     * @param ignoreSpeedFlags Whether or not to ignore the speed flags
     */
    public void setSpeed(Unit speed, boolean openLoop, boolean ignoreSpeedFlags) {
        double speedDifference = Math.abs(speed.get(Unit.Type.METERS) - m_lastDesiredSpeed.get(Unit.Type.METERS));

        if (speedDifference < 0.01 && Math.abs(speed.get(Unit.Type.METERS)) > 0.1 && !ignoreSpeedFlags) {
            return;
        }
        
        if (openLoop) {
            double speedPercentage = speed.get(Unit.Type.METERS) / m_settings.maxSpeed.get(Unit.Type.METERS);

            if (Math.abs(speedPercentage) > 1) {
                speedPercentage = Math.signum(speedPercentage);
            }

            m_driveMotor.set(speed.get(Unit.Type.METERS) / m_settings.maxSpeed.get(Unit.Type.METERS));
        } else {
            m_drivePIDController.setReference(
                speed.get(Unit.Type.METERS),
                ControlType.kVelocity,
                0,
                m_feedforward.calculate(speed.get(Unit.Type.METERS))
            );
        }
    }

    /**
     * Sets the angle of the turn motor
     * @param angle The angle to set the motor to
     * @param ignoreAngleLimit Whether or not to ignore the angle movement limit (used for preventing jolts/snapping and damage to the module)
     */
    public void setAngle(Rotation2d angle, boolean ignoreAngleLimit) {
        double difference = Math.abs(angle.getDegrees() - m_lastDesiredAngle.getDegrees());

        if ((difference > 179 || difference < 0.2) && !ignoreAngleLimit) {
            return;
        }

        m_turnPIDController.setReference(
            m_turnEncoderWrapper.calculatePosition(angle.getDegrees()),
            ControlType.kPosition,
            0,
            m_settings.turnPID.kF
        );
    }

    /**
     * Sets the desired state of the module (speed and angle) with automatic angle optimization
     * @param state The state to set the module to
     * @param openLoop Whether or not to use open loop control
     */
    public void setDesiredState(SwerveModuleState state, boolean openLoop) {
        setDesiredState(state, openLoop, true);
    }

    /**
     * Sets the desired state of the module (speed and angle)
     * @param state The state to set the module to
     * @param openLoop Whether or not to use open loop control
     * @param runOptimization Whether or not to run the optimization algorithm on the angle
     */
    public void setDesiredState(SwerveModuleState state, boolean openLoop, boolean runOptimization) {
        SwerveModuleState optimizedState = runOptimization ? CTREModuleState.optimize(state, m_lastDesiredAngle) : state;

        setSpeed(new Unit(optimizedState.speedMetersPerSecond, Unit.Type.METERS), openLoop, runOptimization);
        setAngle(optimizedState.angle, runOptimization);

        m_lastDesiredAngle = optimizedState.angle;
    }

    /**
     * Sets the speed of the drive motor to 0
     */
    public void stop() {
        setSpeed(new Unit(0, Unit.Type.METERS), true, true);
    }

    /**
     * Gets the absolute angle (angle of the encoder) of the module
     * @return The absolute angle/angle of the encoder of the module
     */
    public Rotation2d getAbsoluteAngle() {
        return Rotation2d.fromDegrees(m_encoder.getAbsolutePosition());
    }

    /**
     * Gets the current state of the module
     * @return The swerve module state derived through the internal encoder velocity and the rotation of the module
     */
    public SwerveModuleState getState() {
        return new SwerveModuleState(
            m_driveEncoderWrapper.getVelocity(),
            Rotation2d.fromDegrees(m_turnEncoderWrapper.getPosition())
        );
    }

    /**
     * Gets the current angle of the module
     * @return The current angle of the module
     */
    public Rotation2d getAngle() {
        return Rotation2d.fromDegrees(m_turnEncoderWrapper.getPosition());
    }

    /**
     * Gets the desired angle of the module
     * @return The desired angle of the module
     */
    public Rotation2d getDesiredAngle() {
        return m_lastDesiredAngle;
    }

    /**
     * Gets the current speed of the module
     * @return The current speed of the module. Assume these units are per seconds.
     */
    public Unit getSpeed() {
        return new Unit(m_driveEncoderWrapper.getVelocity(), Type.METERS);
    }

    /**
     * Gets the current position of the module
     * @return The current position of the module derived through the internal encoder position and the rotation of the module
     */
    public SwerveModulePosition getPosition() {
        return new SwerveModulePosition(
            m_driveEncoderWrapper.getPosition(), 
            getAngle()
        );
    }

    /**
     * Returns if the initialized flag has been enabled.
     */
    public boolean initalized() {
        return m_isInitialized;
    }
}
