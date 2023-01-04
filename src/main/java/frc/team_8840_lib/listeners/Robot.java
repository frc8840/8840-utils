package frc.team_8840_lib.listeners;

import edu.wpi.first.hal.DriverStationJNI;
import edu.wpi.first.hal.NotifierJNI;
import edu.wpi.first.wpilibj.DSControlWord;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.RobotBase;
import frc.team_8840_lib.IO.IOManager;
import frc.team_8840_lib.info.console.Logger;
import frc.team_8840_lib.info.time.TimeKeeper;
import frc.team_8840_lib.input.communication.CommunicationManager;
import frc.team_8840_lib.utils.GamePhase;
import frc.team_8840_lib.utils.time.TimeStamp;

import java.util.Timer;
import java.util.TimerTask;

public class Robot extends RobotBase {
    private static Robot instance;
    public static Robot getInstance() { return instance; }

    private static EventListener listener;

    public static void assignListener(EventListener listener) {
        //using system.out.println just in case the logger hasn't been initialized yet
        if (Robot.listener != null) {
            System.out.println("[Robot] Warning! Unsafe operation: assigning a new event listener. Old listener will be overwritten. In the future, please only assign the listener once.");
        }

        System.out.println("[Robot] Assigning listener: " + listener.getClass().getName());

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

    private DSControlWord controlWord;

    private GamePhase lastPhase;

    public static final double DELTA_TIME = 0.03125; //32 times per sec

    //private final int m_notifier = NotifierJNI.initializeNotifier();

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

    @Override
    public void startCompetition() {
        controlWord = new DSControlWord();

        //DriverStationJNI.observeUserProgramStarting();

        Logger.addClassToBeAutoLogged(new Logger());

        CommunicationManager.init();
        Logger.initWriter();
        IOManager.init();
        TimeKeeper.init();
        Logger.logCompetitionStart();

        lastPhase = GamePhase.Disabled;

        if (!hasListener()) {
            Logger.Log("[Robot] Warning: No event listener assigned. Please assign a listener before starting the competition.");

            Logger.Log("[Robot] Automatically stopping program due to no event listener.", TimeStamp.None);

            exit = true;
        } else {
            listener.robotInit();
        }

        // Loop forever, calling the appropriate mode-dependent function
        while (!exit) {
            DriverStation.refreshData();
            controlWord.refresh();

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

                   // DriverStationJNI.observeUserProgramDisabled();

                    listener.onDisabledPeriodic();
                    break;
                case Autonomous:
                    if (lastPhase != currentPhase) {
                        listener.onAutonomousEnable();
                        lastPhase = currentPhase;
                    }

                   // DriverStationJNI.observeUserProgramAutonomous();

                    listener.onAutonomousPeriodic();
                    break;
                case Teleop:
                    if (lastPhase != currentPhase) {
                        listener.onTeleopEnable();
                        lastPhase = currentPhase;
                    }

                   // DriverStationJNI.observeUserProgramTeleop();

                    listener.onTeleopPeriodic();
                    break;
                case Test:
                    if (lastPhase != currentPhase) {
                        listener.onTestEnable();
                        lastPhase = currentPhase;
                    }

                    //DriverStationJNI.observeUserProgramTest();

                    listener.onTestPeriodic();
                    break;
                default:
                    Logger.Log("[Robot] Warning: Unknown game phase. Please report this to the developers.");
                    break;
            }
        }

        System.out.println("[Robot] Exiting the program...");

        // DriverStationJNI.observeUserProgramDisabled();

        // NotifierJNI.stopNotifier(m_notifier);
        // NotifierJNI.cleanNotifier(m_notifier);
    }

    @Override
    public void endCompetition() {
        Logger.logCompetitionEnd();
        Logger.closeLogger();

        exit = true;
    }
}
