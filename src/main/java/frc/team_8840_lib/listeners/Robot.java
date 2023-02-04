package frc.team_8840_lib.listeners;

import java.nio.file.Path;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Supplier;

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
import frc.team_8840_lib.utils.interfaces.Callback;
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
            System.out.println("[8840-utils] Warning! Unsafe operation: assigning a new event listener. Old listener will be overwritten. In the future, please only assign the listener once.");
        }

        System.out.println("[8840-utils] Assigning listener: " + listener.getClass().getName());

        Robot.listener = listener;

        frameworkUtil = new FrameworkUtil();
    }

    public static String getEventListenerName() {
        return listener.getClass().getSimpleName();
    }

    public static boolean listenerAssigned() {
        return listener != null;
    }

    private static boolean lockEventListenerToOnlyCode = true;

    public static boolean eventListenerIsLockedToCode() {
        return lockEventListenerToOnlyCode;
    }

    public static void assignListenerThroughSettings(Path preferencesFilePath, Class<EventListener>[] listeners, Class<EventListener> _default) {
        if (!Preferences.loaded()) Preferences.loadPreferences(preferencesFilePath);

        lockEventListenerToOnlyCode = false;

        for (Class<EventListener> eventListener : listeners) {
            if (Preferences.getSelectedEventListener() == eventListener.getSimpleName()) {
                try {
                    EventListener newListener = (EventListener) eventListener.getConstructors()[0].newInstance();

                    assignListener(newListener);
                } catch (Exception e) {
                    Logger.Log("[Robot] Was unable to create class, an error occurred. Please check the stack trace for more information.");
                    e.printStackTrace();

                    try {
                        EventListener _defaultListener = (EventListener) _default.getConstructors()[0].newInstance();

                        assignListener(_defaultListener);
                    } catch (Exception e2) {
                        Logger.Log("[Robot] Was unable to create class FOR DEFAULT, an error occurred. Please check the stack trace for more information.");
                        e2.printStackTrace();

                        Logger.Log("[Robot] If you have not already, please make sure that you try assigning the default listener in an if-statement, checking Robot#listenerAssigned()");
                    }
                }
                return;
            }
        }

        try {
            EventListener _defaultListener = (EventListener) _default.getConstructors()[0].newInstance();

            Logger.Log("[Robot] Was unable to find class, assigning default listener: " + _defaultListener.getClass().getName());

            assignListener(_defaultListener);
        } catch (Exception e2) {
            Logger.Log("[Robot] Was unable to create class FOR DEFAULT, an error occurred. Please check the stack trace for more information.");
            e2.printStackTrace();

            Logger.Log("[Robot] If you have not already, please make sure that you try assigning the default listener in an if-statement, checking Robot#listenerAssigned()");
        }
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

    private boolean conditionsFullfilled = true;
    
    @SafeVarargs
    public final Robot waitForFullfillConditions(final int timeoutMS, Supplier<Boolean> ...conditions) {
        conditionsFullfilled = false;

        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                int total = conditions.length;
                int fullfilled = 0;

                for (Supplier<Boolean> condition : conditions) {
                    if (condition.get()) fullfilled++;
                }

                if (fullfilled == total) conditionsFullfilled = true;

                if (conditionsFullfilled) {
                    this.cancel();
                }
            }
        };

        TimerTask overrideFullfill = new TimerTask() {
            @Override
            public void run() {
                conditionsFullfilled = true;
            }
        };

        Timer timer = new Timer();
        timer.schedule(task, 10);

        Timer overrideTimer = new Timer();
        overrideTimer.schedule(overrideFullfill, timeoutMS);

        return this;
    }

    Callback finishFullfillmentCallback = null;

    public void onFinishFullfillment(Callback callback) {
        finishFullfillmentCallback = callback;
    }

    boolean doQuickStart = false;
    
    /**
     * Quick start the robot. This is not recommended for normal use.
     * This will tell HAL that the robot is READY to start, and will not wait for initialization code.
     * This is useful for if the robot looses power and needs to be restarted in order not to lose points due to the light not turning on.
     * Usually, you SHOULD have a delay until the robot is ready to start, especially if you are using a camera or at the start of a match.
     * Else, you're risking the robot not being ready to start, and thus losing points.
     * @param quickStart Whether to quick start or not. TRUE = quick start, FALSE = normal start. It is already set to FALSE by default.
     * @see #waitForFullfillConditions(int, Supplier[])
     * @see #onFinishFullfillment(Callback)
     */
    public void quickStart(boolean quickStart) {
        doQuickStart = quickStart;
        if (doQuickStart) Logger.Log("[Robot] DOING QUICK START. NOTE: THIS IS NOT RECOMMENDED FOR NORMAL USE.");
    }

    @Override
    public void startCompetition() {
        watchdog = new Watchdog(watchdogPeriod, this::printLoopOverrunMessage);

        startTime = System.currentTimeMillis();

        NotifierJNI.setNotifierName(notifier, "8840LibRobot");

        HAL.report(tResourceType.kResourceType_Framework, tInstances.kFramework_Timed);

        if (doQuickStart) DriverStationJNI.observeUserProgramStarting();

        frameworkUtil.onStart();

        boolean noRun = false;

        if (!hasListener()) {
            Logger.Log("[Robot] Warning: No event listener assigned. Please assign a listener before starting the competition.");

            Logger.Log("[Robot] Automatically stopping program due to no event listener.", TimeStamp.None);

            exit = true;
            noRun = true;
        } else {
            listener.robotInit();

            if (!conditionsFullfilled) {
                Logger.Log("[Robot] Conditions not fullfilled for startup, waiting until conditions are fullfilled...");
                Timer waitTimer = new Timer();
                waitTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        if (conditionsFullfilled) {
                            Logger.Log("[Robot] Conditions fullfilled, continuing startup!");
                            this.cancel();

                            if (finishFullfillmentCallback != null) finishFullfillmentCallback.run();
                        }
                    }
                }, 10);

                while (!conditionsFullfilled) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        Logger.Log("[Robot] Robot program startup completed in " + (System.currentTimeMillis() - startTime) + "ms.");

        lastPhase = GamePhase.Disabled;

        //This line is the line that alerts the DriverStation that the robot is ready to be enabled.
        //pretty much turns that red robot code light to green.
        //also this means that any delays in the robotInit() method will delay the robot from being enabled.
        //etc. potentially may add a small little delay method to add some processing or something to allow transfer or data
        //or something like that.
        //also might want to figure out how to make this call if the robot reboots or something to prevent the robot from
        //loosing a crap ton of points. Maybe an empty listener if the robot reboots? or just call this earlier.
        //TODO: figure out this stuff from above.
        if (!doQuickStart) DriverStationJNI.observeUserProgramStarting();

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
