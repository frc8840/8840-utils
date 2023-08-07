package frc.team_8840_lib.examples;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import frc.team_8840_lib.controllers.SwerveGroup;
import frc.team_8840_lib.info.console.Logger;
import frc.team_8840_lib.input.communication.CommunicationManager;
import frc.team_8840_lib.input.controls.GameController;
import frc.team_8840_lib.listeners.EventListener;
import frc.team_8840_lib.listeners.Robot;
import frc.team_8840_lib.pathing.PathConjugate;
import frc.team_8840_lib.pathing.PathPlanner;
import frc.team_8840_lib.pathing.PathConjugate.ConjugateType;
import frc.team_8840_lib.utils.GamePhase;
import frc.team_8840_lib.utils.async.Promise;
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

    @Override
    public void robotInit() {
        //All of this is documented in the SwerveDrive.java file, I'm just simplifying it down for this file.
        SwerveSettings settings = new SwerveSettings(SwerveType.SPARK_MAX);

        settings.defaultAdjustToType();
        settings.updateKinematics();

        // settings.angleOffsets[0] = 0; //First module
        // settings.angleOffsets[1] = 0; //Second module
        // settings.angleOffsets[2] = 0; //Third module
        // settings.angleOffsets[3] = 0; //Fourth module

        settings.threshold = 0.01;
        settings.useThresholdAsPercentage = true;

        swerveDrive = new SwerveGroup("NEO Swerve Drive", settings,
                new int[]{ 1, 3, 5, 7 }, //Drive IDs
                new int[]{ 2, 4, 6, 8 }, //Turn/Steering IDs
                new int[]{ 9, 10, 11, 12 },  //Encoder IDs
                new Pigeon(Pigeon.Type.TWO, 13, false)
        );

        Robot.getInstance().subscribeFixedPhase(new TimerTask() {
            @Override
            public void run() {
                onFixedAutonomous();
            }
        }, GamePhase.Autonomous);

        /**
         * This is an example of constructing a path.
         * Since this is an example, we'll create a "wait for path" to be sent 
         * from the dashboard.
         * This SHOULD NOT be used in a real robot, as it will cause the robot to
         * not move at all.
         * This should be used for testing purposes only.
         */
        PathPlanner.addAuto("default", new PathPlanner(
            PathConjugate.waitForPath(),
            PathConjugate.runOnce(() -> {
                Logger.Log("Finished running path!");
                swerveDrive.stop();
            })
        ));

        /**
         * This is another example of constructing a path, featuring:
         * • A runOnce command
         * • A command
         * • A path segment loaded from a file
            <code>
            String homePath = System.getProperty("user.home");
            //... add this to the auto with PathPlanner#addAuto
            new PathPlanner(
                PathConjugate.runOnce(
                    () -> {
                        Logger.Log("Auto Start!");
                    }
                ),
                PathConjugate.command(
                    new MoveFlywheel(),
                ),
                PathConjugate.command(
                    new StopFlywheel(),
                ),
                PathConjugate.loadPathFromFile(Path.of(homePath, "8840appdata", "segment_1_0000000.json")),
                PathConjugate.loadPathFromFile(Path.of(homePath, "8840appdata", "segment_2_0000000.json")),
                PathConjugate.command(
                    new WhatEverCommand(),
                )
            );
            </code>

            Note: this code should be surrounded with a try and catch statement 
            since it can throw an IOException. 
            If you are confident that the file exists, you can run 
            `PathConjugate#loadPathFromFile(Path path, PathMovement default)` 
            instead.
        */

        CommunicationManager.getInstance().createField();

        GameController.expectController(-1, GameController.Type.Simulated);

        //Wait till the swerve drive is ready to be used
        Robot.getRealInstance()
            .waitForFullfillConditions(1000, new Promise((res, rej) -> {
                Promise.WaitThen(() -> {
                    return swerveDrive.ready();
                }, res, rej, 10);
            }))
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
        PathPlanner.getSelectedAuto().start();
        swerveDrive.resetOdometry(
            //Get the first pose of the first path segment
            PathPlanner.getSelectedAuto().getFirstMovement().getPath().getLastPose()
        );
    }

    @Override
    public void onAutonomousPeriodic() {

    }

    public void onFixedAutonomous() {
        if (!PathPlanner.getSelectedAuto().finished()) {
            if (PathPlanner.getSelectedAuto().getCurrentType() == ConjugateType.Path) {
                Pose2d pose = PathPlanner.getSelectedAuto().current().getPath().moveToNext();
                Pose2d lastPose = PathPlanner.getSelectedAuto().current().getPath().getLastPose();

                //Find Difference between the two poses
                double xDiff = (pose.getTranslation().getX() - lastPose.getTranslation().getX());
                double yDiff = (pose.getTranslation().getY() - lastPose.getTranslation().getY());

                //Create a new translation with the difference
                Translation2d translation = new Translation2d(xDiff / Robot.DELTA_TIME, yDiff / Robot.DELTA_TIME);

                //Use pose to calculate the swerve module states
                swerveDrive.drive(translation, pose.getRotation().getRadians(), true, false);
            }
            PathPlanner.getSelectedAuto().fixedExecute();
        }
    }

    @Override
    public void onTeleopEnable() {

    }

    @Override
    public void onTeleopPeriodic() {
        GameController controller = GameController.get(-1);

        Translation2d translation = new Translation2d(0,0);// new Translation2d(controller.getAxis(Axis.Horizontal), controller.getAxis(Axis.Vertical)).times(swerveDrive.getSettings().maxSpeed);
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
