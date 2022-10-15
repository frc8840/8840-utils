package frc.team_8840_lib.listeners;

import edu.wpi.first.hal.HAL;
import edu.wpi.first.wpilibj.RobotBase;
import frc.team_8840_lib.info.console.Logger;
import frc.team_8840_lib.info.time.TimeKeeper;
import frc.team_8840_lib.input.communication.CommunicationManager;
import frc.team_8840_lib.utils.GamePhase;

import java.util.Timer;
import java.util.TimerTask;

public class Robot extends RobotBase {
    private static Robot instance;
    public static Robot getInstance() { return instance; }

    private static EventListener listener;

    public static void assignListener(EventListener listener) {
        //using system.out.println just in case the logger hasn't been initialized yet
        if (Robot.listener != null) {
            System.out.println("(Robot#assignListener, Line 13): Warning! Unsafe operation: assigning a new event listener. Old listener will be overwritten. In the future, please only assign the listener once.");
        }

        System.out.println("(Robot#assignListener, Line 17): Assigning listener: " + listener.getClass().getName());

        Robot.listener = listener;
    }

    private static boolean hasListener() {
        return listener != null;
    }

    public Robot() {
        super();
        if (instance == null) {
            instance = this;
        } else throw new RuntimeException("Robot already instantiated.");
    }

    //Volatile since can be assessed by stuff outside the program
    private volatile boolean exit;

    private GamePhase lastPhase;

    public static final double DELTA_TIME = 0.03125; //32 times per sec

    private TimerTask fixedAutonomous;
    private TimerTask fixedTeleop;
    private TimerTask fixedTest;

    private Timer fixedTimer;

    /**
     * Due to the nature of periodic tasks (called 3500 to 4000 times per second), it's hard to get an accurate time for timing each call.
     * By using a fixed rate, we can get a more accurate time for each call, which is extremely useful for things like PID and other control loops.
     * This also puts less strain on the CPU since it's only called every 0.03125 seconds (32 times per second), about 1/100th of the time it's called in the other loop.
     * This is also useful for things like logging, AI and other things that don't need to be called as often.
     */
    public void subscribeFixedPhase(TimerTask timerTask, GamePhase phase) {
        if (!hasListener()) {
            Logger.Log("No listener assigned. Cannot subscribe to fixed phase.");
            return;
        }

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
        }
    }

    /**
     * This method is called when the GamePhase changes.
     * This will queue up the fixed rate tasks for the new phase.
     * @param newPhase The new GamePhase.
     **/
    private void onGamePhaseChange(GamePhase newPhase) {
        if (fixedTimer != null) {
            fixedTimer.cancel();
            fixedTimer.purge();
            fixedTimer = null;
        }

        TimerTask task = null;
        switch (newPhase) {
            case Autonomous:
                task = fixedAutonomous;
                break;
            case Teleop:
                task = fixedTeleop;
                break;
            case Test:
                task = fixedTest;
                break;
        }

        if (task != null) {
            fixedTimer = new Timer();
            fixedTimer.scheduleAtFixedRate(task, 0, (long) (DELTA_TIME * 1000));
            Logger.Log("Started fixed rate task for " + newPhase.name());
        }
    }

    @Override
    public void startCompetition() {
        // Tell the DS that the robot is ready to be enabled
        HAL.observeUserProgramStarting();

        CommunicationManager.init();
        TimeKeeper.init();
        Logger.logCompetitionStart();

        lastPhase = GamePhase.Disabled;

        if (!hasListener()) {
            Logger.Log("(Robot#startCompetition, Line 26): Warning: No event listener assigned. Please assign a listener before starting the competition.");
        } else {
            listener.robotInit();
        }

        // Loop forever, calling the appropriate mode-dependent function
        while (!exit) {
            GamePhase currentPhase = GamePhase.getCurrentPhase();

            if (GamePhase.isEnabled()) {
                TimeKeeper.getInstance().checkSubscribers(GamePhase.getCurrentPhase());
            }

            if (lastPhase != currentPhase) {
                TimeKeeper.getInstance().changePhaseTimers(currentPhase);
                onGamePhaseChange(currentPhase);
            }

            listener.robotPeriodic();

            switch (currentPhase) {
                case Disabled:
                    if (lastPhase != currentPhase) {
                        listener.onDisabled();
                        lastPhase = currentPhase;
                    }
                    listener.onDisabledPeriodic();
                    break;
                case Autonomous:
                    if (lastPhase != currentPhase) {
                        listener.onAutonomousEnable();
                        lastPhase = currentPhase;
                    }
                    listener.onAutonomousPeriodic();
                    break;
                case Teleop:
                    if (lastPhase != currentPhase) {
                        listener.onTeleopEnable();
                        lastPhase = currentPhase;
                    }
                    listener.onTeleopPeriodic();
                    break;
                case Test:
                    if (lastPhase != currentPhase) {
                        listener.onTestEnable();
                        lastPhase = currentPhase;
                    }
                    listener.onTestPeriodic();
                    break;
                default:
                    Logger.Log("(Robot#startCompetition, Line 53): Warning: Unknown game phase. Please report this to the developers.");
                    break;
            }
        }
    }

    @Override
    public void endCompetition() {
        Logger.logCompetitionEnd();
        exit = true;
    }
}
