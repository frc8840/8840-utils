package frc.team_8840_lib.examples.replayable;

import java.io.File;

import frc.team_8840_lib.listeners.EventListener;
import frc.team_8840_lib.listeners.Robot;
import frc.team_8840_lib.replay.ReplayManager;
import frc.team_8840_lib.utils.GamePhase;

public class ReplayExample extends EventListener {

    ReplayableExample replayableExample = new ReplayableExample();
    File replayableFile = null;

    /**
     * This creates a new ReplayExample object.
     * @param replayFile The file to replay.
     */
    public ReplayExample(File replayFile) {
        this.replayableFile = replayFile;
    }

    /**
     * This creates a new ReplayExample object.
     * Specify a file in the constructor arguments to replay a file (that was created after this program was run once).
     */
    public ReplayExample() {
        
    }
    
    @Override
    public void robotInit() {
        //These methods will increase the example variables in the ReplayableExample class during Teleop and Autonomous.
        //This is just to show that the replay system works during both phases.
        Robot.getInstance().subscribeFixedPhase(() -> {
            replayableExample.increaseExampleVariable();
        }, GamePhase.Teleop);

        Robot.getInstance().subscribeFixedPhase(() -> {
            replayableExample.increaseExampleVariable2();
        }, GamePhase.Autonomous);

        /**
         * On your first run, this will create a new replay file. Play around with disabling/enabling the teleop/auto in simulation, then stop the program.
         * Then, re-run the program but specify the file that was just created in ~/8840applogs in the constructor arguments of this class to replay the file.
         */
        
        //Purely for styling the console output so the program startup finish is AFTER the replay startup finish.
        //It's not necessary to do this, you can just call it directly in robot init as well.
        Robot.getRealInstance().onFinishStartup(() -> {
            if (replayableFile != null) {
                ReplayManager.getInstance().enterReplay(replayableFile);
            }
        });
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
