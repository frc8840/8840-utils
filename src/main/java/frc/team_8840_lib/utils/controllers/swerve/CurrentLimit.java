package frc.team_8840_lib.utils.controllers.swerve;

import com.ctre.phoenix.motorcontrol.SupplyCurrentLimitConfiguration;

public class CurrentLimit {
    public double continuousCurrent;
    public double peakCurrent;
    public double currentDuration;

    public CurrentLimit(double continuousCurrent, double peakCurrent, double currentDuration) {
        this.continuousCurrent = continuousCurrent;
        this.peakCurrent = peakCurrent;
        this.currentDuration = currentDuration;
    }

    public SupplyCurrentLimitConfiguration toCTRE() {
        return new SupplyCurrentLimitConfiguration(true, continuousCurrent, peakCurrent, currentDuration);
    }

    public SupplyCurrentLimitConfiguration toCTRE(boolean enabled) {
        return new SupplyCurrentLimitConfiguration(enabled, continuousCurrent, peakCurrent, currentDuration);
    }
}
