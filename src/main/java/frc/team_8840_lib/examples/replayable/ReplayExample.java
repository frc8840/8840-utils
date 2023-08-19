package frc.team_8840_lib.examples.replayable;

import frc.team_8840_lib.listeners.EventListener;
import frc.team_8840_lib.listeners.Robot;
import frc.team_8840_lib.utils.GamePhase;

public class ReplayExample extends EventListener {

    ReplayableExample replayableExample = new ReplayableExample();

    @Override
    public void robotInit() {
        Robot.getInstance().subscribeFixedPhase(() -> {
            replayableExample.increaseExampleVariable();
        }, GamePhase.Teleop);

        Robot.getInstance().subscribeFixedPhase(() -> {
            replayableExample.increaseExampleVariable2();
        }, GamePhase.Autonomous);
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
