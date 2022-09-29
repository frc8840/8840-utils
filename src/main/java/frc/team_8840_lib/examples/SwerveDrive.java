package frc.team_8840_lib.examples;

import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import edu.wpi.first.math.util.Units;
import frc.team_8840_lib.controllers.SwerveGroup;
import frc.team_8840_lib.input.controls.GameController;
import frc.team_8840_lib.listeners.EventListener;
import frc.team_8840_lib.utils.controllers.Pigeon;
import frc.team_8840_lib.utils.controllers.SCType;
import frc.team_8840_lib.utils.controllers.swerve.SwerveSettings;
import frc.team_8840_lib.utils.controls.Axis;
import frc.team_8840_lib.utils.math.MathUtils;

public class SwerveDrive extends EventListener {
    private SwerveGroup swerveDrive;

    @Override
    public void robotInit() {
        //Create a new SwerveSettings
        //This has all the default values from Team 364 and Team 3512's robots, but you might want to adjust them.
        //Edit the values like the example below to your liking
        SwerveSettings settings = new SwerveSettings(SCType.SWERVE_Talon_FX);

        //I would call this function BEFORE making adjustments, just changes any values that were different between the two swerve types.
        //Use at risk though, it might adjust the values to something you don't want.
        settings.defaultAdjustToType();

        //The default is 4.5 for maxSpeed already, but you can adjust it how you want.
        //If you want to look at the default values, check out https://github.com/frc8840/8840-utils/tree/main/src/main/java/frc/team_8840_lib/utils/controllers/swerve/SwerveSettings.java
        settings.maxSpeed = 4.5;

        //This value is also the default
        settings.wheelBase = Units.inchesToMeters(21.73);
        //If you do change the wheelBase in the settings, you will need to call this function to update the kinematics.
        //This is because the kinematics are based on the wheelBase.
        settings.updateKinematics();

        //Very important! Adjust the angle offset to zero out the angles.
        //These are just random values, take time to adjust them to your liking.
        settings.angleOffsets[0] = 36.3; //First module
        settings.angleOffsets[1] = 10.51; //Second module
        settings.angleOffsets[2] = 2.25; //Third module
        settings.angleOffsets[3] = 70.31; //Fourth module

        //Set the threshold for the drive to be 0.01 and set it to be a percentage of the max speed.
        //This means that if the joystick is less than 1% of the max speed, it will be 0.
        //This is also used to prevent jittering in the angle motors of the swerve drive.
        settings.threshold = 0.01;
        settings.useThresholdAsPercentage = true;

        //Create a new swerve group
        swerveDrive = new SwerveGroup("Test Swerve Drive", settings,
                new int[]{ 1, 3, 5, 7 }, //Drive IDs
                new int[]{ 2, 4, 6, 8 }, //Turn/Steering IDs
                new int[]{ 1, 2, 3, 4 },  //Encoder IDs
                //Create a new Pigeon Gyro with ID of 5 and type of TWO (Pigeon 2.0) and set it to not inverted (false)
                new Pigeon(Pigeon.Type.TWO, 5, false)
                //Pigeon IMU also exists if you're using that instead
        );

        //Automatically add the controllers that are connected.
        GameController.autoConnect();
    }

    @Override
    public void robotPeriodic() {

    }

    @Override
    public void onAutonomousEnable() {

    }

    @Override
    public void onAutonomousPeriodic() {

    }

    @Override
    public void onTeleopEnable() {

    }

    @Override
    public void onTeleopPeriodic() {
        GameController mainController = GameController.get(0);

        //Get the vertical axis and the horizontal axis of the controller.
        double vertical = mainController.getAxis(Axis.Vertical);
        double horizontal = mainController.getAxis(Axis.Horizontal);

        //Calculate the direction of the robot
        double direction = Math.atan2(vertical, horizontal);

        //Create a new Translation2d with the x and y values of the controller multiplied by the max speed.
        Translation2d translation = new Translation2d(vertical, horizontal).times(swerveDrive.getSettings().maxSpeed);

        swerveDrive.drive(translation, direction, false, false);

        /*
        VALUES: (0.4, 0.9)
        _____________
        |       0   |  Example of the joystick GameController values
        |      //   |  In this example, the robot will go in the direction of the top right corner
        |     /_/   |  (but might not be relative, you may want to do some adjustments if you want it to be relative!)
        |           |  The robot will also go at the speed of how much it's pushed.
        |___________|  Note: This may not be exact example of what will happen. A lot of stuff needs to be tested still since we don't have swerve modules yet (lol).
        */
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
