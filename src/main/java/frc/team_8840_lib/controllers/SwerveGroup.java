package frc.team_8840_lib.controllers;

import java.util.Timer;
import java.util.TimerTask;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.*;
import edu.wpi.first.wpilibj.RobotBase;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import frc.team_8840_lib.info.console.AutoLog;
import frc.team_8840_lib.info.console.Logger;
import frc.team_8840_lib.info.console.Logger.LogType;
import frc.team_8840_lib.input.communication.CommunicationManager;
import frc.team_8840_lib.utils.controllers.Pigeon;
import frc.team_8840_lib.utils.controllers.swerve.CTREConfig;
import frc.team_8840_lib.utils.controllers.swerve.SwerveSettings;
import frc.team_8840_lib.utils.interfaces.SwerveLoop;
import frc.team_8840_lib.utils.logging.Loggable;
import frc.team_8840_lib.utils.math.MathUtils;

/**
 * Your standard swerve drive class. 
 * 
 * Based on the wonderful work of Team 364. 
 * <a href="https://github.com/Team364/BaseFalconSwerve">Repo Here</a> 
 * 
 * Uses CANCoders, Pigeon, and Talons/NEOs for the swerve modules. 
 * 
 * @author Jaiden Grimminck
 * */
public class SwerveGroup implements Loggable {
    private String name;

    public String getName() { return name; }

    private static final String[] moduleNames = {
            "Top Right",
            "Bottom Right",
            "Top Left",
            "Bottom Left"
    };

    public static String getModName(int i) {
        return moduleNames[i];
    }

    public static enum ModuleIndex {
        kTOP_RIGHT(0),
        kBOTTOM_RIGHT(1),
        kTOP_LEFT(2),
        kBOTTOM_LEFT(3);

        private int index;

        private ModuleIndex(int index) {
            this.index = index;
        }

        public int getIndex() {
            return index;
        }
    }

    private SwerveDriveOdometry odometry;

    private SwerveSettings settings;

    //motor ids
    private int[] driveIDs;
    private int[] steerIDs;

    //encoder ids
    private int[] encoderIDs;

    //gyroscope
    private Pigeon gyro;

    //The config for the swerve drive
    private CTREConfig config;

    //A list of modules
    private SwerveModule[] modules;

    private boolean fullyReady = false;
    public boolean ready() { return fullyReady; }

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
        this.name = name;
        
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
        
        //Set the gyro
        this.gyro = pigeon;
        this.gyro.config(); //Config for pigeon has to be called after the pigeon is created unlike others which are called in the constructor

        //Pose odometry usually crashes the program (idk why), so we're commenting it out.
        // pose_odometry = new SwerveDrivePoseEstimator(
        //     getAngle(), getPose(), 
        //     settings.getKinematics(), 
        //     VecBuilder.fill(0.1, 0.1, 0.1),
        //     VecBuilder.fill(0.05),
        //     VecBuilder.fill(0.1, 0.1, 0.1)
        // );

        //Create a new module list
        modules = new SwerveModule[4];

        //Then instantiate the list
        for (int i = 0; i < 4; i++) {
            modules[i] = new SwerveModule(this.driveIDs[i], this.steerIDs[i], this.encoderIDs[i], i, this.config);
        }

        TimerTask whenReady = new TimerTask() {
            @Override
            public void run() {
                //Set the odometry
                odometry = new SwerveDriveOdometry(settings.getKinematics(), getAngle(), getSwervePositions());

                fullyReady = true;
            }
        };

        TimerTask waitTillReady = new TimerTask() {
            int lastAmountReady = 0;
            double startTime = System.currentTimeMillis();

            @Override
            public void run() {
                int count = 0;
                int[] ready = new int[modules.length];

                for (SwerveModule module : modules) {
                    if (module.ready()) {
                        count++;
                        ready[module.getIndex()] = 1;
                    }
                }

                if (count != lastAmountReady) {
                    boolean isFirst = lastAmountReady == 0;

                    //create a string of the modules that are ready
                    String readyString = "";
                    for (int i = 0; i < ready.length; i++) {
                        if (ready[i] == 1) {
                            readyString += moduleNames[i] + ", ";
                        }
                    }
                    
                    Logger.Log((count - lastAmountReady) + (isFirst ? "" : " more") + " modules are now ready. " + readyString + "are ready. Took " + (System.currentTimeMillis() - startTime) + "ms.");

                    lastAmountReady = count;
                }

                if (count == modules.length) {
                    whenReady.run();
                    this.cancel();
                }
            }
        };
        
        //Wait for the modules to be ready
        Timer timer = new Timer();
        timer.schedule(waitTillReady, 0, 10);

        //Add to logger
        Logger.addClassToBeAutoLogged(this);
    }

    /**
     * Sets the speed of every module. Uses open loop.
     * @param value Speeds for each module
     * */
    public void setSpeed(double value) {
        for (SwerveModule module : modules) {
            module.setSpeed(value);
        }
    }

    /**
     * Sets the state of every module using one state. Uses closed or open loop.
     * @deprecated Use {@link #setModuleStates(SwerveModuleState[])} instead. This is deprecated since it's dangerous to use, and it's not used anywhere in the code. Only use this method for testing.
     * @param state State for each module.
     * */
    @Deprecated
    public void setState(SwerveModuleState state, boolean openLoop) {
        for (SwerveModule module : modules) {
            module.setDesiredState(state, openLoop);
        }
    }

    /**
     * Sets the state of every module using an array of states. Uses open loop.
     * @deprecated Use {@link #setModuleStates(SwerveModuleState[])} instead. This is deprecated since it's dangerous to use, and it's not used anywhere in the code. Only use this method for testing.
     * */
    @Deprecated
    public void setState(SwerveModuleState state) {
        setState(state, true);
    }

    /**
     * Sets the speed and rotation of each module using a translation and rotation.
     * @param translation Translation for the robot
     * @param rotation Rotation of the robot, in radians.
     * @param fieldRelative Whether the translation is field relative or not
     * @param openLoop Whether to use open loop or closed loop
     * */
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

        if (RobotBase.isSimulation()) {
            gyro.setDummyAngle(MathUtils.radiansToDegrees(rotation));
        }

        //Convert the chassis speeds to module speeds
        SwerveModuleState[] states = settings.getKinematics().toSwerveModuleStates(chassisSpeeds);

        //Set the module states
        setModuleStates(states, openLoop);
    }

    /**
     * Return SwerveModuleStates for drive
     * @param translation2d Translation for the robot
     * @param rotation Rotation of the robot, in radians.
     * @param fieldRelative Whether the translation is field relative or not
     */
    public SwerveModuleState[] driveStates(Translation2d translation2d, double rotation, boolean fieldRelative, boolean applyDesaturation) {
        ChassisSpeeds chassisSpeeds = (
            fieldRelative ? 
            ChassisSpeeds.fromFieldRelativeSpeeds(
                translation2d.getX(),
                translation2d.getY(),
                rotation, getAngle()
            ) :
            new ChassisSpeeds(
                translation2d.getX(),
                translation2d.getY(),
                rotation
            )
        );

        SwerveModuleState[] states = settings.getKinematics().toSwerveModuleStates(chassisSpeeds);

        if (applyDesaturation) {
            SwerveDriveKinematics.desaturateWheelSpeeds(states, settings.maxSpeed);
        }

        return states;
    }

    /**
     * Sets the module states of the robot using a list of states.
     * It's recommended to use {@link #drive(Translation2d, double, boolean, boolean)} instead of this directly since this can be dangerous to use for things other than testing.
     * @param states States for each module
     *               The order of the states is as follows:
     *               Front Left, Front Right, Back Left, Back Right
     * @param isOpenLoop Whether to use open loop or closed loop
     * */
    public void setModuleStates(SwerveModuleState[] states, boolean isOpenLoop) {
        //Desaturate the states to make sure that the robot doesn't go faster than the max speed
        SwerveDriveKinematics.desaturateWheelSpeeds(states, settings.maxSpeed);

        for (int i = 0; i < 4; i++) {
            modules[i].setDesiredState(states[i], isOpenLoop);
        }

        updateOdometry();
    }

    /**
     * Sets the module states of the robot using a list of states.
     * It's recommended to use {@link #drive(Translation2d, double, boolean, boolean)} instead of this directly since this can be dangerous to use for things other than testing.
     * @param states States for each module
     *               The order of the states is as follows:
     *               Front Left, Front Right, Back Left, Back Right
     * @param isOpenLoop Whether to use open loop or closed loop
     * @param ignoreAngleLimit Whether to ignore the angle limit or not
     * */
    public void setModuleStates(SwerveModuleState[] states, boolean isOpenLoop, boolean ignoreAngleLimit) {
        //Desaturate the states to make sure that the robot doesn't go faster than the max speed
        SwerveDriveKinematics.desaturateWheelSpeeds(states, settings.maxSpeed);

        //Loop through, and set the states
        for (int i = 0; i < 4; i++) {
            modules[i].setDesiredState(states[i], isOpenLoop, ignoreAngleLimit);
        }

        //Update odometry
        updateOdometry();
    }

    /**
     * Rotates the robot in place.
     * @param radiansPerSecond The speed at which the robot should rotate, in radians per second.
     */
    public void spin(double radiansPerSecond) {
        //Create a chassis speeds object with the correct rotation
        ChassisSpeeds chassisSpeeds = new ChassisSpeeds(0, 0, radiansPerSecond);

        //Set the angle of the robot to the correct angle to rotate in place
        SwerveModuleState[] states = settings.getKinematics().toSwerveModuleStates(chassisSpeeds);

        //For debugging purpaces, print the speeds and angles of each module.
        for (int i = 0; i < 4; i++) {
            SmartDashboard.putNumber(getModName(i) + "/speed", states[i].speedMetersPerSecond);
            SmartDashboard.putNumber(getModName(i) + "/angle", states[i].angle.getDegrees());
        }

        //Set the module states, open loop (true). Ignore optimization (true) since this is being buggy.
        setModuleStates(states, true, true);
    }


    /**
     * Sets the module rotations to form an "x" with the directions of the wheels.
     * This is especially useful when the robot is stopped since it can prevent other robots from pushing it.
     */
    public void applyXBrake() {
        //Turn the modules so they form an "x" with the directions of the wheels
        //This is especially useful when the robot is stopped since it can prevent other robots from pushing it.

        Rotation2d x1 = Rotation2d.fromDegrees(45);
        Rotation2d x2 = Rotation2d.fromDegrees(135);

        //Set the module states, but only the angles. Ignore the angle jitter prevention.
        modules[0].setAngle(new SwerveModuleState(0, x1), true);
        modules[1].setAngle(new SwerveModuleState(0, x2), true);
        modules[2].setAngle(new SwerveModuleState(0, x2), true);
        modules[3].setAngle(new SwerveModuleState(0, x1), true);
    }

    /**
     * Sets all module speeds to 0
     */
    public void stop() {
        setSpeed(0);
    }

    /**
     * Sets the brake modes of all the modules
     * @param brake Whether to brake (true) or coast (false)
     */
    public void setBrakeModes(boolean brake) {
        loop((module, i) -> module.setBrakeMode(brake));
    }

    /**
     * Set the drive brake and steer brake modes of all the modules
     * @param driveBrake Whether to brake (true) the drive motors
     * @param steerBrake Whether to brake (true) the steer motors
     */
    public void setIndividualBrakeModes(boolean driveBrake, boolean steerBrake) {
        loop((module, i) -> module.setIndividualBrakeMode(driveBrake, steerBrake));
    }

    /**
     * Sets the angle of all the modules to a certain degree
     * @param degree The degree to set the modules to
     */
    public void setAllModuleAngles(double degree) {
        loop((module, i) -> module.setAngle(degree));
    }

    /**
     * Sets the module states of the robot using a list of states, and makes it automatically use closed loop.
     * It's recommended to use {@link #drive(Translation2d, double, boolean, boolean)} instead of this directly since this can be dangerous to use for things other than testing.
     * @param states States for each module
     * */
    public void setModuleStates(SwerveModuleState[] states) {
        setModuleStates(states, false);
    }

    /**
     * Gets the modules states of the swerve drive
     * @return The states of the modules
     * */
    public SwerveModuleState[] getModuleStates() {
        SwerveModuleState[] states = new SwerveModuleState[4];
        loop((module, i) -> states[i] = module.getState());
        return states;
    }

    /**
     * Uses a lambda to loop through each module. This can be used for information gathering, setting variables, and more.
     * @param s_loop The lambda to use. Takes in the module and the index of the module.
     */
    public void loop(SwerveLoop s_loop) {
        for (int i = 0; i < 4; i++) {
            s_loop.run(modules[i], i);
        }
    }

    /**
     * Resets the odometry of the robot to the provided pose.
     * @param pose The pose to reset the odometry to
     * */
    public void resetOdometry(Pose2d pose) {
        odometry.resetPosition(getAngle(), getSwervePositions(), pose);
        //pose_odometry.resetPosition(pose, getAngle());
    }

    /**
     * Whether to provide power to the modules or not.
     * @param providePowerToThem Whether to provide power to the modules or not
     */
    public void providePower(boolean providePowerToThem) {
        loop((module, i) -> {
            module.noRun = !providePowerToThem;
        });
    }

    /**
     * Whether to do optimization in the modules or not.
     * @param doOptimization Whether to provide power to the modules or not
     */
    public void doStateOptimization(boolean doOptimization) {
        loop((module, i) -> {
            module.doOptimization = doOptimization;
        });
    }

    public void doSetAngle(boolean doSetAngle) {
        loop((module, i) -> {
            module.doSetAngle = doSetAngle;
        });
    }

    public void doSetSpeed(boolean doSetSpeed) {
        loop((module, i) -> {
            module.doSetSpeed = doSetSpeed;
        });
    }

    /**
     * Updates the odometry of the robot using the current position of the robot.
     * */
    public void updateOdometry() {
        odometry.update(getAngle(), getSwervePositions());
        //pose_odometry.update(getAngle(), getModuleStates());
    }

    /**
     * Returns the current pose of the robot.
     * @return The current pose of the robot
     * */
    public Pose2d getPose() {
        if (odometry == null) {
            return new Pose2d(0,0, new Rotation2d(0));
        }

        return odometry.getPoseMeters();
    }

    /**
     * Returns the pose estimator of the robot
     * @return The current estimation of the pose of the robot
     */
    // public Pose2d getEstimatedPose() {
    //     return pose_odometry.getEstimatedPosition();
    // }

    /**
     * Returns the modules of the swerve drive.
     * @return The modules of the swerve drive
     */
    public SwerveModule[] getModules() {
        return modules;
    }

    /**
     * Returns the settings of the swerve drive.
     * @return The settings of the swerve drive
     * */
    public SwerveSettings getSettings() {
        return settings;
    }

    /**
     * Returns the angle of the gyroscope on the robot.
     * @return The angle of the gyroscope on the robot
     * */
    private Rotation2d getAngle() {
        return gyro.getAngle(this.settings.invertGyro);
    }

    /**
     * Gets the name of module 0 to 4
     * @param i Module index
     * @return Module name
     */
    public String getModuleName(int i) {
        return moduleNames[i];
    }

    public SwerveModulePosition[] getSwervePositions() {
        return new SwerveModulePosition[] {
            modules[0].getPosition(),
            modules[1].getPosition(),
            modules[2].getPosition(),
            modules[3].getPosition()
        };
    }

    /**
     * Updates the swerve drive simulation model. This is used for simulation.
     * */
    public void updateFieldRobot() {
        if (!CommunicationManager.getInstance().fieldExists()) return;

        //Module positions
        Translation2d[] modulePositions = getSettings().getPositions();

        Pose2d pose = getPose();

        for (int i = 0; i < 4; i++) {
            Translation2d modulePosition = modulePositions[i]
                .rotateBy(getAngle())
                .plus(pose.getTranslation());
            
            CommunicationManager.getInstance().updateFieldObjectPose(
                this.name + " Module " + i, 
                new Pose2d(
                    modulePosition,
                    modules[i].getState().angle.plus(getAngle())
                )
            );
        }

        //CommunicationManager.getInstance().updateFieldObjectPose("Estimated " + getName(), getEstimatedPose());
    }

    /**
     * Logs the speeds of the modules
     * @return The speeds of the modules
     */
    @AutoLog(logtype = LogType.DOUBLE_ARRAY, name = "Swerve Drive Module Speeds")
    public double[] logSpeeds() {
        double[] speeds = new double[4];
        loop((module, i) -> {
            speeds[i] = module.getState().speedMetersPerSecond;
        });

        return speeds;
    }

    /**
     * Logs the angles of the modules
     * @return The angles of the modules
     */
    @AutoLog(logtype = LogType.DOUBLE_ARRAY, name = "Swerve Drive Module Angles")
    public double[] logAngles() {
        double[] angles = new double[4];
        loop((module, i) -> {
            angles[i] = module.getState().angle.getDegrees();
        });

        return angles;
    }
}
