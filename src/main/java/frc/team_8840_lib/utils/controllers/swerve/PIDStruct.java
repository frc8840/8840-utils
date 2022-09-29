package frc.team_8840_lib.utils.controllers.swerve;

import com.ctre.phoenix.motorcontrol.can.SlotConfiguration;

public class PIDStruct {
    public double kP;
    public double kI;
    public double kD;
    public double kF;

    public PIDStruct(double kp, double ki, double kd, double kf) {
        this.kP = kp;
        this.kI = ki;
        this.kD = kd;
        this.kF = kf;
    }

    public SlotConfiguration toCTRE() {
        SlotConfiguration slotConfiguration = new SlotConfiguration();

        slotConfiguration.kP = this.kP;
        slotConfiguration.kI = this.kI;
        slotConfiguration.kD = this.kD;
        slotConfiguration.kF = this.kF;

        return slotConfiguration;
    }

    public SlotConfiguration toCTRE(SlotConfiguration slotConfiguration) {
        slotConfiguration.kP = this.kP;
        slotConfiguration.kI = this.kI;
        slotConfiguration.kD = this.kD;
        slotConfiguration.kF = this.kF;

        return slotConfiguration;
    }
}
