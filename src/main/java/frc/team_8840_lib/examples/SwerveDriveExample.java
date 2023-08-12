package frc.team_8840_lib.examples;

import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import edu.wpi.first.wpilibj.XboxController;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.button.Trigger;
import frc.team_8840_lib.controllers.SwerveDrive;
import frc.team_8840_lib.info.console.Logger;
import frc.team_8840_lib.listeners.EventListener;
import frc.team_8840_lib.listeners.Robot;
import frc.team_8840_lib.utils.async.Promise;
import frc.team_8840_lib.utils.controllers.Pigeon;
import frc.team_8840_lib.utils.controllers.swerve.ModuleConfig;
import frc.team_8840_lib.utils.controllers.swerve.SwerveSettings;
import frc.team_8840_lib.utils.controllers.swerve.structs.PIDStruct;
import frc.team_8840_lib.utils.math.units.Unit;

/**
 * This example goes more into detail about initializing the SwerveDrive class.
 * See the AutonomousExample file for better usage on driving/autonomous.
 * @see frc.team_8840_lib.examples.AutonomousExample
 * @author Jaiden Grimminck
 */
public class SwerveDriveExample extends EventListener {
    private SwerveDrive m_swerveDrive;

    private XboxController m_controller;

    private boolean m_zeroed = false;

    @Override
    public void robotInit() {
        Logger.Log("SwerveDriveExample", "Swerve Drive Example Initialized!");
        Logger.Log("SwerveDriveExample", "This example is meant to show how to initialize the SwerveDrive class, and how to potentially use it.");
        Logger.Log("SwerveDriveExample", "The code in this class should ideally be spread out in many different classes and commands, but it's all in one class for simplicity.");

        //Create a new SwerveSettings
        //This has all the default values from Team 364 and Team 3512's robots, but you might want to adjust them.
        //Edit the values like the example below to your liking
        SwerveSettings settings = new SwerveSettings();

        //The default is 4.5 for maxSpeed already, but you can adjust it how you want.
        //If you want to look at the default values, check out https://github.com/frc8840/8840-utils/tree/main/src/main/java/frc/team_8840_lib/utils/controllers/swerve/SwerveSettings.java
        settings.maxSpeed = new Unit(4.5, Unit.Type.FEET);

        //Set the track width and wheel base of the robot.
        settings.trackWidth = new Unit(18.75, Unit.Type.INCHES);
        settings.wheelBase = new Unit(22.75, Unit.Type.INCHES);

        if (Robot.isReal()) {
            settings.drivePID = new PIDStruct(0.025, 0, 0, 0);
            settings.turnPID = new PIDStruct(0.012, 0, 0, 0);

            settings.driveKA = 0.0;
            settings.driveKV = 0.0;
            settings.driveKS = 0.0;
        }

        //If you do change the wheelBase in the settings, you will need to call this function to update the kinematics.
        //This is because the kinematics are based on the wheelBase.
        settings.updateKinematics();

        //Set the threshold for the drive to be 0.01 and set it to be a percentage of the max speed.
        //This means that if the joystick is less than 1% of the max speed, it will be 0.
        //This is also used to prevent jittering in the angle motors of the swerve drive.
        settings.threshold = 0.01;
        settings.useThresholdAsPercentage = true;

        final ModuleConfig frontLeft = new ModuleConfig(11, 12, 23, 105.8203);
        final ModuleConfig frontRight = new ModuleConfig(18, 17, 22, 323.877);
        final ModuleConfig backRight = new ModuleConfig(16, 15, 21, 41.8359);
        final ModuleConfig backLeft = new ModuleConfig(13, 14, 24, 215.332);

        //Create a new swerve group
        m_swerveDrive = new SwerveDrive(
            frontLeft,
            frontRight,
            backLeft,
            backRight,
            new Pigeon(Pigeon.Type.TWO, 42),
            settings
        );
        
        //Setup Xbox Controller
        m_controller = new XboxController(0);

        new Trigger(m_controller::getBButton).onTrue(
            Commands.runOnce(() -> {
                m_zeroed = !m_zeroed;
            })
        );

        Robot.getRealInstance().waitForFullfillConditions(
            3000,
            new Promise((res, rej) -> {
                Promise.WaitThen(() -> { return m_swerveDrive.isReady(); }, res, rej, 10);
            })
        );
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

    /**
     * By the way, this method is very scuffed, so I would make major adjustments before using it.
     * Also, I have no clue if swerve works. Test it before using it. (heheheha)
     * */
    @Override
    public void onTeleopPeriodic() {
        //If the zeroed is enabled, zero the swerve drive.
        if (m_zeroed) {
            SwerveModuleState zeroed = new SwerveModuleState(0, new Rotation2d(0));
            m_swerveDrive.setModuleStates(zeroed, zeroed, zeroed, zeroed, Robot.isReal(), false);
        }
        
        //If the threshold is not met, stop the robot
        if (Math.abs(getForward()) < 0.1 && Math.abs(getStrafe()) < 0.1) {
            if (Math.abs(m_controller.getRightX()) < 0.1) {
                m_swerveDrive.stop();
            } else {
                //If the rotate threshold is met, rotate the robot
                m_swerveDrive.spin(Rotation2d.fromRadians(m_controller.getRightX()), Robot.isReal());
            }
            return;
        }

        //Create a new Translation2d with the x and y values of the controller.
        Translation2d translation = new Translation2d(
            getForward(),
            getStrafe()
        );
        
        //Multiply by the max speed.
        translation = translation.times(m_swerveDrive.getSettings().maxSpeed.get(Unit.Type.METERS));

        //Drive
        m_swerveDrive.drive(translation, Rotation2d.fromRadians(m_controller.getRightX()), true, Robot.isReal());
    }

    public double getForward() {
        return -m_controller.getLeftY();
    }

    public double getStrafe() {
        return m_controller.getLeftX();
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
