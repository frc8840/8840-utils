package frc.team_8840_lib.examples.replayable;

import java.io.File;

import frc.team_8840_lib.listeners.EventListener;
import frc.team_8840_lib.listeners.Robot;
import frc.team_8840_lib.replay.ReplayManager;
import frc.team_8840_lib.utils.GamePhase;

public class ReplayExample extends EventListener {

    ReplayableExample replayableExample = new ReplayableExample();
    File replayableFile = new File(System.getProperty("user.home") + "/8840applogs/7-19-2023_1-6.baydat");

    @Override
    public void robotInit() {
        Robot.getInstance().subscribeFixedPhase(() -> {
            replayableExample.increaseExampleVariable();
        }, GamePhase.Teleop);

        Robot.getInstance().subscribeFixedPhase(() -> {
            replayableExample.increaseExampleVariable2();
        }, GamePhase.Autonomous);

        ReplayManager.getInstance().enterReplay(replayableFile);
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
