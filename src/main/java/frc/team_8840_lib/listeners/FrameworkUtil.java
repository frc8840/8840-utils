package frc.team_8840_lib.listeners;

import java.nio.file.Path;
import java.util.Timer;
import java.util.TimerTask;

import edu.wpi.first.wpilibj.DSControlWord;
import edu.wpi.first.wpilibj.DriverStation;
import frc.team_8840_lib.IO.IOManager;
import frc.team_8840_lib.info.console.Logger;
import frc.team_8840_lib.info.time.TimeKeeper;
import frc.team_8840_lib.input.communication.CommunicationManager;
import frc.team_8840_lib.utils.GamePhase;

/**
 * This class is supposed to be the framework of what will make the library work.
 * This will be returned from "no8840LibEventListener" 
 */
public class FrameworkUtil {
    public static FrameworkUtil getInstance() {
        return Robot.getInstance();
    }

    public static final double DELTA_TIME = 0.03125; //32 times per sec
    
    private TimerTask fixedAutonomous;
    private TimerTask fixedTeleop;
    private TimerTask fixedTest;

    private Timer fixedTimer;

    private DSControlWord controlWord;

    private GamePhase lastPhase;

    /**
     * This is supposed to be called at the start.
     */
    public void onStart() {
        controlWord = new DSControlWord();

        Logger.addClassToBeAutoLogged(new Logger());

        CommunicationManager.init();
        Logger.initWriter();
        IOManager.init();
        TimeKeeper.init();
        Logger.logCompetitionStart();

        lastPhase = GamePhase.Disabled;
    }

    /**
     * This method is supposed to be called periodically/often.
     * This will manage any phase timers or other utility methods.
     */
    public void periodicCall() {
        DriverStation.refreshData();
        controlWord.refresh();

        GamePhase currentPhase = GamePhase.getCurrentPhase();

        if (GamePhase.isEnabled()) {
            TimeKeeper.getInstance().checkSubscribers(GamePhase.getCurrentPhase());
        }

        if (lastPhase != currentPhase) {
            TimeKeeper.getInstance().changePhaseTimers(currentPhase);
            onGamePhaseChange(currentPhase);

            lastPhase = currentPhase;
        }
    }

    /**
     * Supposed to be called when the program finishes.
     */
    public void onEnd() {
        Logger.logCompetitionEnd();
        Logger.closeLogger();

        IOManager.close();
    }

    /**
     * Due to the nature of periodic tasks (called 3500 to 4000 times per second), it's hard to get an accurate time for timing each call.
     * By using a fixed rate, we can get a more accurate time for each call, which is extremely useful for things like PID and other control loops.
     * This also puts less strain on the CPU since it's only called every 0.03125 seconds (32 times per second), about 1/100th of the time it's called in the other loop.
     * This is also useful for things like logging, AI and other things that don't need to be called as often.
     */
    public void subscribeFixedPhase(TimerTask timerTask, GamePhase phase) {
        switch (phase) {
            case Autonomous:
                fixedAutonomous = timerTask;
                break;
            case Teleop:
                fixedTeleop = timerTask;
                break;
            case Test:
                fixedTest = timerTask;
                break;
            default:
                break;
        }
    }

    /**
     * This method is called when the GamePhase changes.
     * This will queue up the fixed rate tasks for the new phase, and resubscribe TimeKeeper subscriptions.
     * @param newPhase The new GamePhase.
     **/
    private void onGamePhaseChange(GamePhase newPhase) {
        try {
            if (TimeKeeper.getInstance().automaticallyResubscribeEvents) {
                TimeKeeper.getInstance().resubscribeAll(newPhase.getTimerName());
            }
        } catch (Exception e) {
            Logger.Log("There was an error resubscribing events.");
            e.printStackTrace();
        }

        if (fixedTimer != null) {
            fixedTimer.cancel();
            fixedTimer.purge();
            fixedTimer = null;
        }

        TimerTask task = null;
        switch (newPhase) {
            case Autonomous:
                task = new TimerTask() {
                    @Override
                    public void run() {
                        if (fixedAutonomous != null) {
                            fixedAutonomous.run();
                        }
                    }
                };
                break;
            case Teleop:
                task = new TimerTask() {
                    @Override
                    public void run() {
                        if (fixedTeleop != null) {
                            fixedTeleop.run();
                        }
                    }
                };
                break;
            case Test:
                task = new TimerTask() {
                    @Override
                    public void run() {
                        if (fixedTest != null) {
                            fixedTest.run();
                        }
                    }
                };
                break;
            default:
                break;
        }

        if (task != null) {
            fixedTimer = new Timer();
            fixedTimer.scheduleAtFixedRate(task, 0, (long) (DELTA_TIME * 1000));
            Logger.Log("[Robot] Started fixed rate task for " + newPhase.name());
        }
    }


    public DSControlWord getDSControlWord() {
        return controlWord;
    }
}
