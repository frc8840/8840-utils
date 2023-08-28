package frc.team_8840_lib.examples.replayable;

import frc.team_8840_lib.info.console.AutoLog;
import frc.team_8840_lib.info.console.Logger;
import frc.team_8840_lib.replay.Replayable;
import frc.team_8840_lib.utils.IO.IOMethod;
import frc.team_8840_lib.utils.IO.IOMethodType;
import frc.team_8840_lib.utils.IO.IOValue;

public class ReplayableExample extends Replayable {

    /**
     * This creates a new ReplayableExample object.
     * To make sure that the replayable system works, you must call the super() method in the constructor
     * in order to initialize the replayable system.
     * 
     * Note: Make sure that you use the right types when logging/replaying variables.
     * The types can be found under the LogType enum! 
     * If you want to replay a variable that's not a specified type in the LogType enum, consider using Strings and doing it through methods.
     */
    public ReplayableExample() {
        super();
    }

    /**
     * This method increases the example variable by 1. This is called in the robotInit() method in ReplayExample.java.
     * Here, we can use the replaying() method to check if we are in replay mode or not.
     * If not, we don't want to interfere with the replay, so we just return.
     */
    public void increaseExampleVariable() {
        //Check if we're in replay or not
        if (!replaying()) return;

        //If we are not, increase the variable
        exampleVariable++;
    }

    /**
     * This method increases the example variable 2 by 1. This is called in the robotInit() method in ReplayExample.java.
     * Here, we can use the replaying() method to check if we are in replay mode or not.
     * If not, we don't want to interfere with the replay, so we just return.
     */
    public void increaseExampleVariable2() {
        //Check if we're in replay or not
        if (!replaying()) return;

        //If we are not, increase the variable
        exampleVariable2++;
    }

    /**
     * This method is required on any Replayable object.
     * This is used to get the base name of the Replayable object.
     * If you have multiple instances of the same Replayable object, you can use this to differentiate between them, 
     * hence it being a method and not a variable.
     * For example, if I wanted to replay individual swerve modules, I could change the base name to have the position of the module in it.
     * 
     * If you have multiple Replayable objects with the getBaseName() method returning the same value, the replay system will not work!
     * It may cause errors, or it may just not work at all, as well as there may be issues in your log file.
     * 
     * @return The base name of the Replayable object.
     */
    @Override
    public String getBaseName() {
        return "ReplayableExample";
    }

    /**
     * This method is required on any Replayable object.
     * This is called when the replay system is initialized.
     * This is where you can put any code that you want to run when the replay system is initialized.
     */
    @Override
    public void replayInit() {
        Logger.Log(getBaseName(), "Entered Replay!");
    }

    /**
     * This method is required on any Replayable object.
     * This is called when the replay system is exited.
     * This is where you can put any code that you want to run when the replay system is exited.
     */
    @Override
    public void exitReplay() {
        Logger.Log(getBaseName(), "Exited Replay!");
        Logger.Log(getBaseName(), "Example Variable 1: " + exampleVariable);
        Logger.Log(getBaseName(), "Example Variable 2: " + exampleVariable2);
    }

    /**
     * This is an example of replaying a variable.
     * You don't need to specify the name of the variable, as it will automatically use the name of the variable unless you specify it.
     * You also don't need to specify a replay link, as it will automatically use the same variable name unless you specify it.
     * This can be used REGARDLESS of whether the variable is public or not.
     * 
     * This doesn't need to have the IOMethod annotation, as it is replaying itself (the replaylink isn't specified).
     */
    @AutoLog
    private int exampleVariable = 0;

    //This won't be logged directly, but through the replay system.
    private int exampleVariable2 = 0;

    /**
     * This is an example of a method that logs a method.
     * You don't need to specify the name of the method, but you will need to specify a replay link for it to work.
     * This method HAS TO BE PUBLIC, as the replay system will not be able to access it otherwise.
     * @return The example variable 2.
     */
    @AutoLog(name = "exampleVariable2", replaylink = "replayExampleVariable2")
    public int getExampleVariable2() {
        return exampleVariable2;
    }

    /**
     * This is an example of an IOMethod that will recieve data from the replay system.
     * Unfortunately the IO System hasn't been streamlined compared to the replay system, so you will need to specify a lot of things.
     * We recommend setting toNT to false in order to make sure that this won't be written to through the IO NT System.
     * The name needs to be the same as the replay link in the AutoLog annotation, same with the value_type.
     * The method_type needs to be WRITE, as we are writing to the variable.
     * This method HAS TO BE PUBLIC, as the replay system will not be able to access it otherwise.
     * 
     * Note: IOMethod notation will change in the future, and for replays this may change to a new annotation, away from the IO System.
     * @param exampleVariable2 The example variable 2 to be set.
     */
    @IOMethod(method_type = IOMethodType.WRITE, name = "replayExampleVariable2", value_type = IOValue.INT, toNT = false)
    public void setExampleVariable2(int exampleVariable2) {
        this.exampleVariable2 = exampleVariable2;
    }
    
}
