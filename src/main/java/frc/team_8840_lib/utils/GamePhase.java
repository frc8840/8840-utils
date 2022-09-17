package frc.team_8840_lib.utils;

import edu.wpi.first.wpilibj.DriverStation;

public enum GamePhase {
    Autonomous,
    Teleop,
    Test,
    Disabled;

    public static GamePhase getCurrentPhase() {
        if (DriverStation.isAutonomous() && DriverStation.isAutonomousEnabled()) {
            return Autonomous;
        } else if (DriverStation.isTeleop() && DriverStation.isTeleopEnabled()) {
            return Teleop;
        } else if (DriverStation.isTest() && DriverStation.isEnabled()) {
            return Test;
        } else {
            return Disabled;
        }
    }

    public static boolean isEnabled() {
        return getCurrentPhase() != Disabled;
    }

    public String getTimerName() {
        switch (this) {
            case Autonomous:
                return "auto";
            case Teleop:
                return "teleop";
            case Test:
                return "test";
            default:
                return "main";
        }
    }
}
