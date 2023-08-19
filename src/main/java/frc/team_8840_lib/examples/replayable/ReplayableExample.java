package frc.team_8840_lib.examples.replayable;

import frc.team_8840_lib.info.console.AutoLog;
import frc.team_8840_lib.info.console.Logger;
import frc.team_8840_lib.replay.Replayable;
import frc.team_8840_lib.utils.IO.IOMethod;
import frc.team_8840_lib.utils.IO.IOMethodType;
import frc.team_8840_lib.utils.IO.IOValue;

public class ReplayableExample extends Replayable {

    public ReplayableExample() {
        super();
    }

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

    @AutoLog
    private int exampleVariable = 0;

    private int exampleVariable2 = 0;

    @AutoLog(name = "exampleVariable2", replaylink = "replayExampleVariable")
    public int getExampleVariable2() {
        return exampleVariable2;
    }

    @IOMethod(method_type = IOMethodType.WRITE, name = "replayExampleVariable2", value_type = IOValue.INT, toNT = false)
    public void setExampleVariable2(int exampleVariable2) {
        this.exampleVariable2 = exampleVariable2;
    }
    
}
