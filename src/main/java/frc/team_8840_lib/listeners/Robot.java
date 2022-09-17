package frc.team_8840_lib.listeners;

import edu.wpi.first.hal.HAL;
import edu.wpi.first.wpilibj.RobotBase;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import frc.team_8840_lib.info.console.Logger;
import frc.team_8840_lib.info.time.TimeKeeper;
import frc.team_8840_lib.input.communication.CommunicationManager;
import frc.team_8840_lib.utils.GamePhase;

public class Robot extends RobotBase {
    private static Robot instance;
    public static Robot getInstance() { return instance; }

    private static EventListener listener;

    public static void assignListener(EventListener listener) {
        if (Robot.listener != null) {
            Logger.Log("(Robot#assignListener, Line 13): Warning! Unsafe operation: assigning a new event listener. Old listener will be overwritten. In the future, please only assign the listener once.");
        }

        SmartDashboard.putString("EventListener", "Assigned to " + listener.toString());
        SmartDashboard.updateValues();

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

    private volatile boolean m_exit;
    private GamePhase lastPhase;

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
        while (!m_exit) {
            GamePhase currentPhase = GamePhase.getCurrentPhase();

            if (GamePhase.isEnabled()) {
                TimeKeeper.getInstance().checkSubscribers(GamePhase.getCurrentPhase());
            }

            if (lastPhase != currentPhase) {
                TimeKeeper.getInstance().changePhaseTimers(currentPhase);
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
        m_exit = true;
    }
}
