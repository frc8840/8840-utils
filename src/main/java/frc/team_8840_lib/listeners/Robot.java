package frc.team_8840_lib.listeners;

import edu.wpi.first.wpilibj.RobotBase;
import frc.team_8840_lib.info.console.Logger;
import frc.team_8840_lib.utils.GamePhase;
import frc.team_8840_lib.utils.time.TimeStamp;

public class Robot extends RobotBase {
    private static Robot instance;
    public static FrameworkUtil getInstance() { 
        return frameworkUtil;
    }

    public static Robot getRealInstance() {
        return instance;
    }

    public static final double DELTA_TIME = 0.03125; //32 times per sec

    private static boolean runningWithInstance = true;

    private static FrameworkUtil frameworkUtil;

    private static EventListener listener;

    public static void assignListener(EventListener listener) {
        //using system.out.println just in case the logger hasn't been initialized yet
        if (Robot.listener != null) {
            System.out.println("[Robot] Warning! Unsafe operation: assigning a new event listener. Old listener will be overwritten. In the future, please only assign the listener once.");
        }

        System.out.println("[Robot] Assigning listener: " + listener.getClass().getName());

        Robot.listener = listener;

        frameworkUtil = new FrameworkUtil();
    }

    private static boolean hasListener() {
        return listener != null;
    }

    public static FrameworkUtil no8840LibEventListener() {
        runningWithInstance = false;
        frameworkUtil = new FrameworkUtil();
        return frameworkUtil;
    }

    public Robot() {
        super();

        if (!runningWithInstance) exit = true;

        if (instance == null) {
            instance = this;
        }// else throw new RuntimeException("Robot already instantiated.");
    }

    //Volatile since can be assessed by stuff outside the program
    private volatile boolean exit;

    private GamePhase lastPhase;

    @Override
    public void startCompetition() {
        frameworkUtil.onStart();

        boolean noRun = false;

        if (!hasListener()) {
            Logger.Log("[Robot] Warning: No event listener assigned. Please assign a listener before starting the competition.");

            Logger.Log("[Robot] Automatically stopping program due to no event listener.", TimeStamp.None);

            exit = true;
            noRun = true;
        } else {
            listener.robotInit();
        }

        lastPhase = GamePhase.Disabled;

        // Loop forever, calling the appropriate mode-dependent function
        while (!exit) {            
            frameworkUtil.periodicCall();

            GamePhase currentPhase = GamePhase.getCurrentPhase();

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

        if (!noRun) System.out.println("[Robot] Exiting the program...");

        // DriverStationJNI.observeUserProgramDisabled();

        // NotifierJNI.stopNotifier(m_notifier);
        // NotifierJNI.cleanNotifier(m_notifier);
    }

    @Override
    public void endCompetition() {
        frameworkUtil.onEnd();

        exit = true;
    }
}
