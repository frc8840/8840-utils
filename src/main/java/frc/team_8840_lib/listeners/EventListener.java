package frc.team_8840_lib.listeners;

import frc.team_8840_lib.utils.GamePhase;

public abstract class EventListener {
    public abstract void robotInit();
    public abstract void robotPeriodic();
    public abstract void onAutonomousEnable();
    public abstract void onAutonomousPeriodic();
    public abstract void onTeleopEnable();
    public abstract void onTeleopPeriodic();
    public abstract void onTestEnable();
    public abstract void onTestPeriodic();
    public abstract void onDisabled();
    public abstract void onDisabledPeriodic();

    public GamePhase phase() {
        return GamePhase.getCurrentPhase();
    }
}
