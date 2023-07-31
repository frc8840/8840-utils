package frc.team_8840_lib.utils.controllers.swerve;

public class ModuleConfig {
    private int driveMotorID;
    private int turnMotorID;
    private int encoderID;
    private double turnOffset;

    public ModuleConfig(int driveMotorID, int turnMotorID, int encoderID, double turnOffset) {
        this.driveMotorID = driveMotorID;
        this.turnMotorID = turnMotorID;
        this.encoderID = encoderID;
        this.turnOffset = turnOffset;
    }

    public int getDriveMotorID() {
        return driveMotorID;
    }

    public int getTurnMotorID() {
        return turnMotorID;
    }

    public int getEncoderID() {
        return encoderID;
    }

    public double getTurnOffset() {
        return turnOffset;
    }
}
