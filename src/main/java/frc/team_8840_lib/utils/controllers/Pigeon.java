package frc.team_8840_lib.utils.controllers;

import com.ctre.phoenix.sensors.CANCoderConfiguration;
import com.ctre.phoenix.sensors.Pigeon2;
import com.ctre.phoenix.sensors.Pigeon2Configuration;
import com.ctre.phoenix.sensors.PigeonIMU;
import edu.wpi.first.math.geometry.Rotation2d;
import frc.team_8840_lib.utils.controllers.swerve.FalconConversions;

public class Pigeon {
    private PigeonIMU pigeonIMU;
    private Pigeon2 pigeon2;

    private Type type;

    private int id;

    private boolean inverted;

    public Pigeon(Type pigeonType, int pigeonID, boolean inverted) {
        type = pigeonType;

        id = pigeonID;

        if (type == Type.IMU) {
            pigeonIMU = new PigeonIMU(pigeonID);
        } else if (type == Type.TWO) {
            pigeon2 = new Pigeon2(pigeonID);
        }

        this.inverted = inverted;
    }

    public Pigeon(Type pigeonType, int pigeonID) {
        this(pigeonType, pigeonID, false);
    }

    public void config() {
        if (type == Type.IMU) {
            pigeonIMU.configFactoryDefault();
        } else if (type == Type.TWO) {
            pigeon2.configFactoryDefault();
        }

        zero();
    }

    public void zero() {
        if (type == Type.IMU) {
            pigeonIMU.setYaw(0);
        } else if (type == Type.TWO) {
            pigeon2.setYaw(0);
        }
    }

    public Rotation2d getAngle(boolean inverted) {
        double angle = 0;

        if (type == Type.IMU) {
            double[] ypr = new double[3];
            pigeonIMU.getYawPitchRoll(ypr);
            angle = ypr[0];
        } else if (type == Type.TWO) {
            angle = pigeon2.getYaw();
        }

        return inverted ? Rotation2d.fromDegrees(360 - angle) : Rotation2d.fromDegrees(angle);
    }

    public Rotation2d getAngle() {
        return getAngle(inverted);
    }

    public double[] getRotation() {
        double[] rotation = new double[3];

        if (type == Type.IMU) {
            pigeonIMU.getYawPitchRoll(rotation);
        } else if (type == Type.TWO) {
            pigeon2.getYawPitchRoll(rotation);
        }

        return rotation;
    }

    public static enum Type {
        TWO,
        IMU
    }
}
