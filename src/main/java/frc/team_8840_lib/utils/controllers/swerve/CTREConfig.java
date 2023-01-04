package frc.team_8840_lib.utils.controllers.swerve;

import com.ctre.phoenix.motorcontrol.can.TalonFXConfiguration;
import com.ctre.phoenix.sensors.AbsoluteSensorRange;
import com.ctre.phoenix.sensors.CANCoderConfiguration;
import com.ctre.phoenix.sensors.SensorInitializationStrategy;
import com.ctre.phoenix.sensors.SensorTimeBase;

public class CTREConfig {
    public static CTREConfig create(SwerveSettings settings) {
        return new CTREConfig(settings);
    }

    private CANCoderConfiguration canCoderConfiguration;
    private TalonFXConfiguration driveTalonFXConfiguration;
    private TalonFXConfiguration turnTalonFXConfiguration;

    private SwerveSettings settings;

    public CTREConfig(SwerveSettings settings) {
        this.settings = settings;

        this.setupConfig();
    }

    public CANCoderConfiguration getCanCoderConfiguration() {
        return this.canCoderConfiguration;
    }

    public TalonFXConfiguration getDriveTalonFXConfiguration() {
        return this.driveTalonFXConfiguration;
    }

    public TalonFXConfiguration getTurnTalonFXConfiguration() {
        return this.turnTalonFXConfiguration;
    }

    public SwerveSettings getSettings() {
        return this.settings;
    }

    public void setupConfig() {
        //CAN Coder Configurations
        this.canCoderConfiguration = new CANCoderConfiguration();

        this.canCoderConfiguration.absoluteSensorRange = AbsoluteSensorRange.Unsigned_0_to_360;
        this.canCoderConfiguration.sensorDirection = this.settings.canCoderInverted;
        this.canCoderConfiguration.initializationStrategy = SensorInitializationStrategy.BootToAbsolutePosition;
        this.canCoderConfiguration.sensorTimeBase = SensorTimeBase.PerSecond;

        //Drive Talon FX Configurations
        this.driveTalonFXConfiguration = new TalonFXConfiguration();

        this.driveTalonFXConfiguration.slot0 = this.settings.drivePID.toCTRE(this.driveTalonFXConfiguration.slot0);
        this.driveTalonFXConfiguration.supplyCurrLimit = this.settings.driveCurrentLimit.toCTRE(this.settings.useCurrentLimits);
        this.driveTalonFXConfiguration.initializationStrategy = SensorInitializationStrategy.BootToZero;
        this.driveTalonFXConfiguration.openloopRamp = this.settings.driveOpenRampRate;
        this.driveTalonFXConfiguration.closedloopRamp = this.settings.driveClosedRampRate;

        this.turnTalonFXConfiguration = new TalonFXConfiguration();

        this.turnTalonFXConfiguration.slot0 = this.settings.turnPID.toCTRE(this.turnTalonFXConfiguration.slot0);
        this.turnTalonFXConfiguration.supplyCurrLimit = this.settings.turnCurrentLimit.toCTRE(this.settings.useCurrentLimits);
        this.turnTalonFXConfiguration.initializationStrategy = SensorInitializationStrategy.BootToZero;
    }
}