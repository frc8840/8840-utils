package frc.team_8840_lib.examples;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.util.Units;
import frc.team_8840_lib.controllers.SwerveGroup;
import frc.team_8840_lib.input.communication.CommunicationManager;
import frc.team_8840_lib.input.controls.GameController;
import frc.team_8840_lib.listeners.EventListener;
import frc.team_8840_lib.listeners.Robot;
import frc.team_8840_lib.pathing.PathPlanner;
import frc.team_8840_lib.utils.GamePhase;
import frc.team_8840_lib.utils.controllers.Pigeon;
import frc.team_8840_lib.utils.controllers.SCType;
import frc.team_8840_lib.utils.controllers.swerve.SwerveSettings;
import frc.team_8840_lib.utils.controls.Axis;

import java.util.TimerTask;

public class AutonomousExample extends EventListener {
    private SwerveGroup swerveDrive;
    private PathPlanner pathPlanner;

    @Override
    public void robotInit() {
        //All of this is documented in the SwerveDrive.java file, I'm just simplifying it down for this file.
        SwerveSettings settings = new SwerveSettings(SCType.SWERVE_SparkMax);

        settings.defaultAdjustToType();
        settings.updateKinematics();

        settings.angleOffsets[0] = 0; //First module
        settings.angleOffsets[1] = 0; //Second module
        settings.angleOffsets[2] = 0; //Third module
        settings.angleOffsets[3] = 0; //Fourth module

        settings.threshold = 0.000001;
        settings.useThresholdAsPercentage = true;

        swerveDrive = new SwerveGroup("NEO Swerve Drive", settings,
                new int[]{ 1, 3, 5, 7 }, //Drive IDs
                new int[]{ 2, 4, 6, 8 }, //Turn/Steering IDs
                new int[]{ 1, 2, 3, 4 },  //Encoder IDs
                new Pigeon(Pigeon.Type.TWO, 5, false)
        );

        GameController.autoConnect();
        Robot.getInstance().subscribeFixedPhase(new TimerTask() {
            @Override
            public void run() {
                onFixedAutonomous();
            }
        }, GamePhase.Autonomous);

        //This is the new method that'll be used for waiting for the dashboard to send the path from 8840-app
        CommunicationManager.getInstance().waitForAutonomousPath(points -> {
            pathPlanner = new PathPlanner();
            pathPlanner.updateTimePoints(points);
        });

        CommunicationManager.getInstance().createField();
        swerveDrive.resetOdometry(new Pose2d(7, 4, new Rotation2d(0)));
    }

    @Override
    public void robotPeriodic() {
        CommunicationManager.getInstance().updateSwerveInfo(swerveDrive);
        if (CommunicationManager.getInstance().fieldExists()) {
            swerveDrive.updateOdometry();

            CommunicationManager.getInstance().updateFieldObjectPose("SwerveRobot", swerveDrive.getPose());

            swerveDrive.updateFieldRobot();
        }
    }

    @Override
    public void onAutonomousEnable() {
        pathPlanner.reset();
        swerveDrive.resetOdometry(pathPlanner.getLastPose());
    }

    @Override
    public void onAutonomousPeriodic() {

    }

    public void onFixedAutonomous() {
        Pose2d pose = pathPlanner.moveToNext();
        Pose2d lastPose = pathPlanner.getLastPose();

        //Find Difference between the two poses
        double xDiff = (pose.getTranslation().getX() - lastPose.getTranslation().getX());
        double yDiff = (pose.getTranslation().getY() - lastPose.getTranslation().getY());
        double angleToNextPoint = Math.atan2(yDiff, xDiff);

        //Create a new translation with the difference
        Translation2d translation = new Translation2d(xDiff / Robot.DELTA_TIME, yDiff / Robot.DELTA_TIME);

        //Use pose to calculate the swerve module states
        swerveDrive.drive(translation, 0, true, false);
    }

    @Override
    public void onTeleopEnable() {
        GameController.addController(0);
    }

    @Override
    public void onTeleopPeriodic() {
        GameController controller = GameController.get(0);

        Translation2d translation = new Translation2d(controller.getAxis(Axis.Horizontal), controller.getAxis(Axis.Vertical)).times(swerveDrive.getSettings().maxSpeed);
        double rotation = controller.getAxis(Axis.Twist) * swerveDrive.getSettings().maxAngularSpeed;

        swerveDrive.drive(translation, Units.degreesToRadians(rotation / 100), true, false);
    }

    @Override
    public void onTestEnable() {

    }

    @Override
    public void onTestPeriodic() {

    }

    @Override
    public void onDisabled() {

    }

    @Override
    public void onDisabledPeriodic() {

    }
}
