package frc.team_8840_lib.examples.replayable;

import frc.team_8840_lib.info.console.AutoLog;
import frc.team_8840_lib.info.console.Logger;
import frc.team_8840_lib.replay.Replayable;

public class ReplayableExample extends Replayable {

    @AutoLog
    private int exampleVariable = 0;

    @Override
    public String getBaseName() {
        return "ReplayableExample";
    }

    @Override
    public void replayInit() {
        Logger.Log(getBaseName(), "Entered Replay!");
    }

    @Override
    public void exitReplay() {
        Logger.Log(getBaseName(), "Exited Replay!");
    }
    
}
