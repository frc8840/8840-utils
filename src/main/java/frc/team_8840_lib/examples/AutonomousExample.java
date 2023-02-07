package frc.team_8840_lib.examples;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import frc.team_8840_lib.controllers.SwerveGroup;
import frc.team_8840_lib.input.communication.CommunicationManager;
import frc.team_8840_lib.input.controls.GameController;
import frc.team_8840_lib.listeners.EventListener;
import frc.team_8840_lib.listeners.Robot;
import frc.team_8840_lib.pathing.PathPlanner;
import frc.team_8840_lib.utils.GamePhase;
import frc.team_8840_lib.utils.controllers.Pigeon;
import frc.team_8840_lib.utils.controllers.swerve.SwerveSettings;
import frc.team_8840_lib.utils.controllers.swerve.SwerveType;
import frc.team_8840_lib.utils.controls.Axis;

import java.util.TimerTask;

import com.revrobotics.REVPhysicsSim;

/**
 * This is another example on how to use the SwerveDrive class. This example is for autonomous, and uses the PathPlanner class to generate a path.
 * @author Jaiden Grimminck
 */
public class AutonomousExample extends EventListener {
    private SwerveGroup swerveDrive;
    private PathPlanner pathPlanner;

    @Override
    public void robotInit() {
        //All of this is documented in the SwerveDrive.java file, I'm just simplifying it down for this file.
        SwerveSettings settings = new SwerveSettings(SwerveType.SPARK_MAX);

        settings.defaultAdjustToType();
        settings.updateKinematics();

        settings.angleOffsets[0] = 0; //First module
        settings.angleOffsets[1] = 0; //Second module
        settings.angleOffsets[2] = 0; //Third module
        settings.angleOffsets[3] = 0; //Fourth module

        settings.threshold = 0.01;
        settings.useThresholdAsPercentage = true;

        swerveDrive = new SwerveGroup("NEO Swerve Drive", settings,
                new int[]{ 1, 3, 5, 7 }, //Drive IDs
                new int[]{ 2, 4, 6, 8 }, //Turn/Steering IDs
                new int[]{ 9, 10, 11, 12 },  //Encoder IDs
                new Pigeon(Pigeon.Type.TWO, 5, false)
        );

        Robot.getInstance().subscribeFixedPhase(new TimerTask() {
            @Override
            public void run() {
                onFixedAutonomous();
            }
        }, GamePhase.Autonomous);

        //This is the new method that'll be used for waiting for the dashboard to send the path from 8840-app
        CommunicationManager.getInstance().waitForAutonomousPath(points -> {
            if (points.length == 0) {
                //Ignore it. This can happen to request the autonomous to be set to nothing, but we can ignore it.
                return;
            }
            pathPlanner = new PathPlanner();
            pathPlanner.updateTimePoints(points);
        });

        CommunicationManager.getInstance().createField();

        GameController.expectController(-1, GameController.Type.Simulated);

        //Wait till the swerve drive is ready to be used
        Robot.getRealInstance()
            .waitForFullfillConditions(1000, () -> swerveDrive.ready())
            .onFinishFullfillment(() -> {
                swerveDrive.resetOdometry(new Pose2d(7, 4, new Rotation2d(0)));
            });
    }

    @Override
    public void robotPeriodic() {
        if (Robot.isSimulation()) {
            REVPhysicsSim.getInstance().run();
        }

        CommunicationManager.getInstance().updateSwerveInfo(swerveDrive);
        if (CommunicationManager.getInstance().fieldExists()) {
            swerveDrive.updateOdometry();

            CommunicationManager.getInstance().updateFieldObjectPose("SwerveRobot", swerveDrive.getPose());

            swerveDrive.updateFieldRobot();
        }
    }

    @Override
    public void onAutonomousEnable() {
        // if (pathPlanner == null) return;

        pathPlanner.reset();
        swerveDrive.resetOdometry(pathPlanner.getLastPose());
    }

    @Override
    public void onAutonomousPeriodic() {

    }

    public void onFixedAutonomous() {
        // if (pathPlanner == null) return;

        if (!pathPlanner.finished()) {
            Pose2d pose = pathPlanner.moveToNext();
            Pose2d lastPose = pathPlanner.getLastPose();

            //Find Difference between the two poses
            double xDiff = (pose.getTranslation().getX() - lastPose.getTranslation().getX());
            double yDiff = (pose.getTranslation().getY() - lastPose.getTranslation().getY());

            //Create a new translation with the difference
            Translation2d translation = new Translation2d(xDiff / Robot.DELTA_TIME, yDiff / Robot.DELTA_TIME);

            //Use pose to calculate the swerve module states
            swerveDrive.drive(translation, pose.getRotation().getRadians(), true, false);
        }
    }

    @Override
    public void onTeleopEnable() {

    }

    @Override
    public void onTeleopPeriodic() {
        GameController controller = GameController.get(-1);

        Translation2d translation = new Translation2d(controller.getAxis(Axis.Horizontal), controller.getAxis(Axis.Vertical)).times(swerveDrive.getSettings().maxSpeed);
        double rotation = ((360 - controller.getAxis(Axis.Twist)) / 360) * 2 * Math.PI;

        swerveDrive.drive(translation, rotation, true, false);
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
