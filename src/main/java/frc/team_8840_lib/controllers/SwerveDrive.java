package frc.team_8840_lib.controllers;

import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.math.kinematics.SwerveDriveKinematics;
import edu.wpi.first.math.kinematics.SwerveDriveOdometry;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import frc.team_8840_lib.info.console.AutoLog;
import frc.team_8840_lib.info.console.Logger;
import frc.team_8840_lib.replay.Replayable;
import frc.team_8840_lib.utils.IO.IOAccess;
import frc.team_8840_lib.utils.IO.IOMethod;
import frc.team_8840_lib.utils.IO.IOMethodType;
import frc.team_8840_lib.utils.IO.IOPermission;
import frc.team_8840_lib.utils.IO.IOValue;
import frc.team_8840_lib.utils.async.Promise;
import frc.team_8840_lib.utils.controllers.Pigeon;
import frc.team_8840_lib.utils.controllers.swerve.ModuleConfig;
import frc.team_8840_lib.utils.controllers.swerve.SwerveSettings;
import frc.team_8840_lib.utils.math.units.Unit;
import frc.team_8840_lib.utils.math.units.Unit.Type;

@IOAccess(IOPermission.READ_WRITE)
public class SwerveDrive extends Replayable {

    private SwerveDriveOdometry m_odometry;
    private SwerveSettings m_settings;

    private SwerveModule m_frontLeft;
    private SwerveModule m_frontRight;
    private SwerveModule m_backLeft;
    private SwerveModule m_backRight;

    private Pigeon m_pigeon;

    private boolean m_isInitialized;

    private boolean m_replayOpenLoop = false;

    public SwerveDrive(
        ModuleConfig frontLeft, 
        ModuleConfig frontRight, 
        ModuleConfig backLeft, 
        ModuleConfig backRight, 
        Pigeon pigeon, SwerveSettings settings
    ) {
        super();
        m_settings = settings;

        m_frontLeft = new SwerveModule(m_settings, frontLeft, SwerveModule.Position.FRONT_LEFT);
        m_frontRight = new SwerveModule(m_settings, frontRight, SwerveModule.Position.FRONT_RIGHT);
        m_backLeft = new SwerveModule(m_settings, backLeft, SwerveModule.Position.BACK_LEFT);
        m_backRight = new SwerveModule(m_settings, backRight, SwerveModule.Position.BACK_RIGHT);

        m_pigeon = pigeon;

        int startTime = (int) System.currentTimeMillis();

        new Promise((res, rej) -> {
            Promise.WaitThen(() -> {
                return m_frontLeft.initalized() && m_frontRight.initalized() && m_backLeft.initalized() && m_backRight.initalized();
            }, res, rej, 10);
        }).then((res, rej) -> {
            int endTime = (int) System.currentTimeMillis();

            Logger.Log(getBaseName(), "Initialized in " + (endTime - startTime) + "ms");

            m_isInitialized = true;

            res.run();
        }).catch_err((err) -> {
            err.printStackTrace();
            Logger.Log(getBaseName(), "Failed to initialize!");
        });
    }

    /**
     * Sets the speed and rotation of each module using a translation and rotation.
     * @param translation Translation for the robot
     * @param rotationSpeed Rotation speed of the robot.
     * @param fieldRelative Whether the translation is field relative or not
     * @param openLoop Whether to use open loop or closed loop
     * */
    public void drive(Translation2d translation, Rotation2d rotationSpeed, boolean fieldRelative, boolean openLoop) {
        ChassisSpeeds chassisSpeeds = (
            fieldRelative ?
            ChassisSpeeds.fromFieldRelativeSpeeds(
                translation.getX(),
                translation.getY(),
                rotationSpeed.getRadians(), getAngle()
            ) :
            new ChassisSpeeds(
                translation.getX(),
                translation.getY(),
                rotationSpeed.getRadians()
            )
        );

        SwerveModuleState[] states = m_settings.getKinematics().toSwerveModuleStates(chassisSpeeds);

        SwerveDriveKinematics.desaturateWheelSpeeds(states, m_settings.maxSpeed.get(Type.METERS));
        
        setModuleStates(states[0], states[1], states[2], states[3], openLoop, true);
    }

    public void stop() {
        m_frontLeft.stop();
        m_frontRight.stop();
        m_backLeft.stop();
        m_backRight.stop();
    }

    public void spin(Rotation2d rotationPerSecond, boolean openLoop) {
        ChassisSpeeds chassisSpeeds = new ChassisSpeeds(0, 0, rotationPerSecond.getRadians());

        SwerveModuleState[] states = m_settings.getKinematics().toSwerveModuleStates(chassisSpeeds);

        SwerveDriveKinematics.desaturateWheelSpeeds(states, m_settings.maxSpeed.get(Type.METERS));

        setModuleStates(states[0], states[1], states[2], states[3], openLoop, false);
    }

    public void setModuleStates(SwerveModuleState frontRight, SwerveModuleState backRight, SwerveModuleState frontLeft, SwerveModuleState backLeft, boolean openLoop, boolean runOptimization) {
        m_frontRight.setDesiredState(frontRight, openLoop, runOptimization);
        m_backRight.setDesiredState(backRight, openLoop, runOptimization);
        m_frontLeft.setDesiredState(frontLeft, openLoop, runOptimization);
        m_backLeft.setDesiredState(backLeft, openLoop, runOptimization);
    }

    /**
     * Returns the angle of the gyroscope on the robot.
     * @return The angle of the gyroscope on the robot
     * */
    public Rotation2d getAngle() {
        double yaw = m_pigeon.getYawPitchRoll()[0];
        return (m_settings.invertGyro ? Rotation2d.fromDegrees(360 - yaw) : Rotation2d.fromDegrees(yaw)).plus(m_settings.gyroscopeStartingAngle);
    }

    public boolean isReady() {
        return this.m_isInitialized;
    }

    @Override
    public String getBaseName() {
        return "SwerveDrive";
    }

    /* Logging Methods */

    @AutoLog(name = "Gyroscope", replaylink = "replayGyroscope")
    public double[] getPointing() {
        return m_pigeon.getYawPitchRoll();
    }

    @AutoLog(name = "openLoop", replaylink = "replayOpenLoop")
    public boolean isOpenLoop() {
        return m_replayOpenLoop;
    }

    @IOMethod(name = "replayOpenLoop", value_type = IOValue.BOOLEAN, method_type = IOMethodType.WRITE, toNT = false)
    public void replayOpenLoop(boolean openLoop) {
        if (!this.replaying()) return;

        m_replayOpenLoop = openLoop;
    }

    @AutoLog(name = "Angles", replaylink = "replayAngles")
    public double[] getAngles() {
        return new double[] {
            m_frontRight.getAngle().getDegrees(),
            m_backRight.getAngle().getDegrees(),
            m_frontLeft.getAngle().getDegrees(),
            m_backLeft.getAngle().getDegrees()
        };
    }

    @IOMethod(name = "replayAngles", value_type = IOValue.DOUBLE_ARRAY, method_type = IOMethodType.WRITE, toNT = false)
    public void replayAngles(double[] angles) {
        if (!this.replaying()) return;

        Rotation2d[] rotAngles = new Rotation2d[] {
            Rotation2d.fromDegrees(angles[0]),
            Rotation2d.fromDegrees(angles[1]),
            Rotation2d.fromDegrees(angles[2]),
            Rotation2d.fromDegrees(angles[3])
        };

        m_frontRight.setAngle(rotAngles[0], m_isInitialized);
        m_backRight.setAngle(rotAngles[1], m_isInitialized);
        m_frontLeft.setAngle(rotAngles[2], m_isInitialized);
        m_backLeft.setAngle(rotAngles[3], m_isInitialized);
    }

    @AutoLog(name = "Speeds")
    public double[] getSpeeds() {
        return new double[] {
            m_frontRight.getSpeed().get(Type.METERS),
            m_backRight.getSpeed().get(Type.METERS),
            m_frontLeft.getSpeed().get(Type.METERS),
            m_backLeft.getSpeed().get(Type.METERS)
        };
    }

    @IOMethod(name = "replaySpeeds", value_type = IOValue.DOUBLE_ARRAY, method_type = IOMethodType.WRITE, toNT = false)
    public void replaySpeeds(double[] speeds) {
        if (!this.replaying()) return;

        m_frontRight.setSpeed(new Unit(speeds[0], Type.METERS), m_replayOpenLoop);
        m_backRight.setSpeed(new Unit(speeds[1], Type.METERS), m_replayOpenLoop);
        m_frontLeft.setSpeed(new Unit(speeds[2], Type.METERS), m_replayOpenLoop);
        m_backLeft.setSpeed(new Unit(speeds[3], Type.METERS), m_replayOpenLoop);
    }

    private Pigeon m_copy_pigeon;

    @Override
    public void replayInit() {
        m_copy_pigeon = m_pigeon;
        m_pigeon = new Pigeon(Pigeon.Type.DUMMY, m_pigeon.getID());
    }

    @Override
    public void exitReplay() {
        m_pigeon = m_copy_pigeon;
        m_copy_pigeon = null;
    }
    
}
