package frc.team_8840_lib.examples;

import frc.team_8840_lib.controllers.ControllerGroup;
import frc.team_8840_lib.info.time.TimeKeeper;
import frc.team_8840_lib.listeners.EventListener;
import frc.team_8840_lib.utils.GamePhase;
import frc.team_8840_lib.utils.controllers.SCType;
import frc.team_8840_lib.utils.time.SubscriptionType;

/**
 * This is an example of making some motors move at different times during autonomous.
 * @author Jaiden Grimminck
 */
public class MotorExample extends EventListener {

    ControllerGroup kGroup;

    @Override
    public void robotInit() {
        kGroup = new ControllerGroup("group", SCType.Spark, 0, 1);

        TimeKeeper.getInstance()
                .subscribe(
                        "two_motors_move",
                        GamePhase.Autonomous.getTimerName(), 3,
                        SubscriptionType.BeforeTime,
                        () -> {
                            kGroup.setSpeed(0.5);
                        },
                        () -> {
                            kGroup.stop();
                        }
                )
                .subscribe(
                        "one_motor_move",
                        GamePhase.Autonomous.getTimerName(), 4, 7,
                        SubscriptionType.BetweenTimes,
                        () -> {
                            kGroup.setPortSpeed(0, 0.5);
                        },
                        () -> {
                            kGroup.stop();
                        }
                )
                .subscribe(
                        "opposite_direction",
                        GamePhase.Autonomous.getTimerName(), 8, 11,
                        SubscriptionType.BetweenTimes,
                        () -> {
                            kGroup.setPortSpeed(0, 0.5);
                            kGroup.setPortSpeed(1, -0.5);
                        },
                        () -> {
                            kGroup.stop();
                        }
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

    @Override
    public void onTeleopPeriodic() {

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
