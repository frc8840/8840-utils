package frc.team_8840_lib.examples;

import frc.team_8840_lib.controllers.ControllerGroup;
import frc.team_8840_lib.info.console.Logger;
import frc.team_8840_lib.info.time.TimeKeeper;
import frc.team_8840_lib.input.communication.CommunicationManager;
import frc.team_8840_lib.input.controls.GameController;
import frc.team_8840_lib.listeners.EventListener;
import frc.team_8840_lib.utils.GamePhase;
import frc.team_8840_lib.utils.controllers.SCType;
import frc.team_8840_lib.utils.controls.Axis;
import frc.team_8840_lib.utils.time.SubscriptionType;

public class TankDrive extends EventListener {

    private ControllerGroup drive;
    private ControllerGroup intake;

    @Override
    public void robotInit() {
        //This method is called once the robot is initialized.
        //Use this method to initialize your robot.
        //In this example, we're assuming the robot is a tank drive robot, with a singular PWM motor for the intake.
        //It also has an autonomous mode that is controlled by timing.

        //Create the left and right groups, and add the Spark Max controllers (using PWM) that's connected to port 0 to 3.
        ControllerGroup left = new ControllerGroup("left", SCType.PWM_SparkMax, 0, 1);
        ControllerGroup right = new ControllerGroup("right", SCType.PWM_SparkMax, 2, 3);
        //Inverse all the motors on the right side.
        right.invert();

        //Note: You'll have to check if the motors need to be inverted the other way.
        //1 should be the forward value, and -1 should be the backwards value. If this is wrong, then you'll have to invert the motors the other way.
        //Luckily this is simple to do, just use left.invert(); instead of right.invert();

        //Set the drive to the left and right groups.
        drive = ControllerGroup.combine("tank_drive", left, right);

        //Create the intake controller, then evolve it into a controller group with the name "intake".
        intake = ControllerGroup.createSC(4, SCType.Spark).evolve("intake");

        //Automatically add all connected controllers. This is useful, but can be dangerous if your connections don't work at the beginning.
        //Use with caution.
        GameController.autoConnect();

        //Update the communication manager that the autonomous mode has started.
        CommunicationManager.getInstance().updateInfo("auto", "intake", "none");
        CommunicationManager.getInstance().updateInfo("auto", "movement", "none");

        //After, register the events for autonomous. Since we don't want these events registered multiple times in the onAutonomousEnable() method, we'll register them here.
        /*
        This subscription below is the same as this piece of code for a TimedRobot:
        <code>
        boolean stoppedIntake = false;
        //...
        if (autonomousTimer.get() < 2) {
            intake.set(1);
        } else if (!stoppedIntake) {
            intake.set(0)
            stopIntake = true;
        }
        </code>
        */

        //Create a subscription that will run the intake for 2 seconds, then stop.
        TimeKeeper.getInstance()
                .subscribe("shoot_ball", GamePhase.Autonomous.getTimerName(), 2d, SubscriptionType.BeforeTime, () -> {
                    intake.setSpeed(1);

                    //Update the communication manager that the intake is running.
                    CommunicationManager.getInstance().updateInfo("auto", "intake", "running");
                }, () -> {
                    //Once it's finished, stop the intake.
                    //A second callback (stored under <key>_onceFinished) can be supplied to the TimeKeeper#subscribe method if you're using SubscriptionType.BeforeTime.
                    intake.setSpeed(0);

                    //Send to the dashboard that the intake has stopped.
                    CommunicationManager.getInstance().updateInfo("auto", "intake", "stopped");
                })

                //Create a subscription that'll move the robot back slowly for 10 seconds at 2 seconds
                .subscribe("move_back", GamePhase.Autonomous.getTimerName(), 2d, 12d, SubscriptionType.BetweenTimes, () -> {
                    //Go slowly backwards
                    drive.setSpeed(-0.1);

                    //Update the communication manager that the robot is moving back.
                    CommunicationManager.getInstance().updateInfo("auto", "movement", "backwards");
                }, () -> {
                    //Once it's finished, stop the robot.
                    drive.setSpeed(0);

                    //Send to the dashboard that the robot has stopped.
                    CommunicationManager.getInstance().updateInfo("auto", "movement", "stopped");
                });
    }

    @Override
    public void robotPeriodic() {
        //This method is called periodically when the robot is on.
    }

    @Override
    public void onAutonomousEnable() {
        
    }

    @Override
    public void onAutonomousPeriodic() {
        //Autonomous is already handled by the TimeKeeper subscriptions. If you want other things in the auto period, you can do it here.
    }

    @Override
    public void onTeleopEnable() {
        //This method is called when teleop is enabled. It only runs once.
    }

    @Override
    public void onTeleopPeriodic() {
        //This method runs periodically while teleop is enabled.

        //Get the main controller on port 0.
        GameController mainController = GameController.get(0);

        //Get the vertical axis and the horizontal axis of the controller.
        double speed = mainController.getAxis(Axis.Vertical);
        double turn = mainController.getAxis(Axis.Horizontal);

        //Set the drive speed and turn speed.
        drive.setSubGroupSpeed("left", speed - turn);
        drive.setSubGroupSpeed("right", -(speed + turn));
    }

    @Override
    public void onTestEnable() {
        //This method is called when test mode is enabled. It only runs once.
        Logger.Log("Test enabled!");
    }

    @Override
    public void onTestPeriodic() {
        //This method is called periodically while test mode is enabled.
    }

    @Override
    public void onDisabled() {
        //This method is called once after when the robot is disabled.
        Logger.Log("Disabled robot!");
    }

    @Override
    public void onDisabledPeriodic() {
        //This method is called periodically while the robot is disabled.
    }
}
