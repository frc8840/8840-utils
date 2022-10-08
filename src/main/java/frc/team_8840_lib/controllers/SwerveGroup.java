package frc.team_8840_lib.controllers;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.math.kinematics.SwerveDriveKinematics;
import edu.wpi.first.math.kinematics.SwerveDriveOdometry;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import frc.team_8840_lib.utils.controllers.Pigeon;
import frc.team_8840_lib.utils.controllers.SCType;
import frc.team_8840_lib.utils.controllers.swerve.CTREConfig;
import frc.team_8840_lib.utils.controllers.swerve.SwerveSettings;
import frc.team_8840_lib.utils.interfaces.SwerveLoop;

/**
 * Based on the wonderful work of Team 364
 * <a href="https://github.com/Team364/BaseFalconSwerve">Repo Here</a>
 * */
public class SwerveGroup extends ControllerGroup {
    private SwerveDriveOdometry odometry;

    private SwerveSettings settings;

    //motor ids
    private int[] driveIDs;
    private int[] steerIDs;

    //encoder ids
    private int[] encoderIDs;

    //gyroscope
    private Pigeon gyro;

    private CTREConfig config;

    private SwerveModule[] modules;

    /**
     * Creates a new swerve group (4 modules)
     * @param name Name of the swerve group
     * @param settings Settings for the swerve group
     * @param driveIDs Drive motor IDs
     * @param steerIDs Steering/turning motor IDs
     * @param encoderIDs Encoder IDs
     * @param pigeon Gyroscope (Pigeon 2.0 or IMU, use Pigeon class)
     * */
    public SwerveGroup(String name, SwerveSettings settings, int[] driveIDs, int[] steerIDs, int[] encoderIDs, Pigeon pigeon) {
        super(name, settings.getType()); //init w/ no ports so it doesn't create any objects.

        if (driveIDs.length != steerIDs.length && driveIDs.length != encoderIDs.length) {
            throw new IllegalArgumentException("Drive and steer ports must be the same length");
        }

        if (driveIDs.length != 4) {
            throw new IllegalArgumentException("Drive and steer ports must each have 4 motors");
        }

        this.settings = settings;

        this.config = CTREConfig.create(settings);

        //Each index of the array corresponds to a module
        //For example, drivePorts[0] is the drive port for module 0
        this.driveIDs = driveIDs;
        this.steerIDs = steerIDs;

        this.encoderIDs = encoderIDs;

        this.gyro = pigeon;
        this.gyro.config(); //Config for pigeon has to be called after the pigeon is created unlike others which are called in the constructor

        odometry = new SwerveDriveOdometry(settings.getKinematics(), getAngle());

        modules = new SwerveModule[4];

        for (int i = 0; i < 4; i++) {
            modules[i] = new SwerveModule(driveIDs[i], steerIDs[i], encoderIDs[i], i, config);
        }
    }

    public void setSpeed(double value) {
        for (SwerveModule module : modules) {
            module.setSpeed(value);
        }
    }

    public void setState(SwerveModuleState state, boolean openLoop) {
        for (SwerveModule module : modules) {
            module.setDesiredState(state, openLoop);
        }
    }

    public void setState(SwerveModuleState state) {
        setState(state, true);
    }

    public void drive(Translation2d translation, double rotation, boolean fieldRelative, boolean openLoop) {
        ChassisSpeeds chassisSpeeds = (
            fieldRelative ?
            ChassisSpeeds.fromFieldRelativeSpeeds(
                translation.getX(),
                translation.getY(),
                rotation, getAngle()
            ) :
            new ChassisSpeeds(
                translation.getX(),
                translation.getY(),
                rotation
            )
        );

        //Convert the chassis speeds to module speeds
        SwerveModuleState[] states = settings.getKinematics().toSwerveModuleStates(chassisSpeeds);

        //Set the module states
        setModuleStates(states, openLoop);
    }

    public void setModuleStates(SwerveModuleState[] states, boolean isOpenLoop) {
        //Desaturate the states to make sure that the robot doesn't go faster than the max speed
        SwerveDriveKinematics.desaturateWheelSpeeds(states, settings.maxSpeed);

        for (int i = 0; i < 4; i++) {
            modules[i].setDesiredState(states[i], isOpenLoop);
        }
    }

    public void setModuleStates(SwerveModuleState[] states) {
        setModuleStates(states, false);
    }

    public SwerveModuleState[] getModuleStates() {
        SwerveModuleState[] states = new SwerveModuleState[4];
        loop((module, i) -> states[i] = module.getState());
        return states;
    }

    public void loop(SwerveLoop s_loop) {
        for (int i = 0; i < 4; i++) {
            s_loop.run(modules[i], i);
        }
    }

    public void resetOdometry(Pose2d pose) {
        odometry.resetPosition(pose, getAngle());
    }

    public void updateOdometry() {
        odometry.update(getAngle(), getModuleStates());
    }

    public Pose2d getPose() {
        return odometry.getPoseMeters();
    }

    public SwerveSettings getSettings() {
        return settings;
    }

    private Rotation2d getAngle() {
        return gyro.getAngle(this.settings.invertGyro);
    }
}
