package frc.team_8840_lib.utils;

import edu.wpi.first.wpilibj.DSControlWord;
import frc.team_8840_lib.listeners.Robot;

public enum GamePhase {
    Autonomous,
    Teleop,
    Test,
    Disabled;

    public static GamePhase getCurrentPhase() {
        DSControlWord dsControlWord = Robot.getInstance().getDSControlWord();
        
        if (dsControlWord.isAutonomous() && dsControlWord.isAutonomousEnabled()) {
            return Autonomous;
        } else if (dsControlWord.isTeleop() && dsControlWord.isTeleopEnabled()) {
            return Teleop;
        } else if (dsControlWord.isTest() && dsControlWord.isEnabled()) {
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
