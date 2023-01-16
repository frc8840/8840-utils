package frc.team_8840_lib.listeners;

import edu.wpi.first.hal.DriverStationJNI;
import edu.wpi.first.hal.HAL;
import edu.wpi.first.hal.NotifierJNI;
import edu.wpi.first.hal.FRCNetComm.tInstances;
import edu.wpi.first.hal.FRCNetComm.tResourceType;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.RobotBase;
import edu.wpi.first.wpilibj.Watchdog;
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

    private Watchdog watchdog;
    private final double watchdogPeriod = 0.02;

    private final int notifier = NotifierJNI.initializeNotifier();
    
    private double startTime;

    @Override
    public void startCompetition() {
        watchdog = new Watchdog(watchdogPeriod, this::printLoopOverrunMessage);

        startTime = System.currentTimeMillis();

        NotifierJNI.setNotifierName(notifier, "8840LibRobot");

        HAL.report(tResourceType.kResourceType_Framework, tInstances.kFramework_Timed);

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

        Logger.Log("[Robot] Robot program startup completed in " + (System.currentTimeMillis() - startTime) + "ms.");

        //This line is the line that alerts the DriverStation that the robot is ready to be enabled.
        //pretty much turns that red robot code light to green.
        //also this means that any delays in the robotInit() method will delay the robot from being enabled.
        //etc. potentially may add a small little delay method to add some processing or something to allow transfer or data
        //or something like that.
        //also might want to figure out how to make this call if the robot reboots or something to prevent the robot from
        //loosing a crap ton of points. Maybe an empty listener if the robot reboots? or just call this earlier.
        //TODO: figure out this stuff from above.
        DriverStationJNI.observeUserProgramStarting();

        lastPhase = GamePhase.Disabled;

        // Loop forever, calling the appropriate mode-dependent function
        while (!exit) {
            NotifierJNI.updateNotifierAlarm(notifier, (long) (System.currentTimeMillis() + DELTA_TIME * 1000));

            //Don't think this is needed since this is a custom framework.
            // long curTime = NotifierJNI.waitForNotifierAlarm(notifier);
            // if (curTime == 0) {
            //     break;
            // }

            frameworkUtil.periodicCall();

            GamePhase currentPhase = GamePhase.getCurrentPhase();

            listener.robotPeriodic();
            watchdog.reset();

            if (Robot.isSimulation()) {
                HAL.simPeriodicBefore();
            }
            
            switch (currentPhase) {
                case Disabled:
                    if (lastPhase != currentPhase) {
                        try {
                            listener.onDisabled();
                        } catch (Exception e) {
                            Logger.Log("[Robot] Error in onDisabled()!");
                            e.printStackTrace();
                        }
                        watchdog.addEpoch("onDisabled()");
                        lastPhase = currentPhase;
                    }

                    DriverStationJNI.observeUserProgramDisabled();

                    try {
                        listener.onDisabledPeriodic();
                    } catch (Exception e) {
                        Logger.Log("[Robot] Error in onDisabledPeriodic()!");
                        e.printStackTrace();
                    }

                    watchdog.addEpoch("onDisabledPeriodic()");

                    break;
                case Autonomous:
                    if (lastPhase != currentPhase) {
                        try {
                            listener.onAutonomousEnable();
                        } catch (Exception e) {
                            Logger.Log("[Robot] Error in onAutonomousEnable()!");
                            e.printStackTrace();
                        }
                        watchdog.addEpoch("onAutonomousEnable()");
                        lastPhase = currentPhase;
                    }

                    DriverStationJNI.observeUserProgramAutonomous();

                    try {
                        listener.onAutonomousPeriodic();
                    } catch (Exception e) {
                        Logger.Log("[Robot] Error in onAutonomousPeriodic()!");
                        e.printStackTrace();
                    }

                    watchdog.addEpoch("onAutonomousPeriodic()");

                    break;
                case Teleop:
                    if (lastPhase != currentPhase) {
                        try {
                            listener.onTeleopEnable();
                        } catch (Exception e) {
                            Logger.Log("[Robot] Error in onTeleopEnable()!");
                            e.printStackTrace();
                        }
                        watchdog.addEpoch("onTeleopEnable()");
                        lastPhase = currentPhase;
                    }

                    DriverStationJNI.observeUserProgramTeleop();

                    try {
                        listener.onTeleopPeriodic();
                    } catch (Exception e) {
                        Logger.Log("[Robot] Error in onTeleopPeriodic()!");
                        e.printStackTrace();
                    }

                    watchdog.addEpoch("onTeleopPeriodic()");

                    break;
                case Test:
                    if (lastPhase != currentPhase) {
                        listener.onTestEnable();
                        watchdog.addEpoch("onTestEnable()");
                        lastPhase = currentPhase;
                    }

                    DriverStationJNI.observeUserProgramTest();

                    try {
                        listener.onTestPeriodic();
                    } catch (Exception e) {
                        Logger.Log("[Robot] Error in onTestPeriodic()!");
                        e.printStackTrace();
                    }

                    watchdog.addEpoch("onTestPeriodic()");

                    break;
                default:
                    Logger.Log("[Robot] Warning: Unknown game phase. Please report this to the developers.");
                    break;
            }

            if (Robot.isSimulation()) {
                HAL.simPeriodicAfter();
            }

            watchdog.disable();

            if (watchdog.isExpired()) {
                watchdog.printEpochs();
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

        NotifierJNI.stopNotifier(notifier);

        exit = true;
    }

    @Override
    public void close() {
        NotifierJNI.stopNotifier(notifier);
        NotifierJNI.cleanNotifier(notifier);
    }

    private void printLoopOverrunMessage() {
        DriverStation.reportWarning("Loop time of " + watchdogPeriod + "s overrun\n", false);
        Logger.Log("[Robot] Warning: Loop time of " + watchdogPeriod + "s overrun", TimeStamp.BothRealAndGameTime);
    }
}
