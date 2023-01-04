package frc.team_8840_lib.utils.controllers;

import com.ctre.phoenix.sensors.Pigeon2;
import com.ctre.phoenix.sensors.PigeonIMU;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.wpilibj.RobotBase;
import frc.team_8840_lib.info.console.Logger;

public class Pigeon {
    private PigeonIMU pigeonIMU;
    private Pigeon2 pigeon2;

    private Type type;

    private int id;

    private boolean inverted;

    private double dummyAngle = 0;

    /**
     * Sets the dummy angle of the pigeon gyroscope. Used primarly for simulation.
     * @param angle
     */
    public void setDummyAngle(double angle) {
        dummyAngle = angle;
    }

    /**
     * Creates a new Pigeon gyroscope.
     * @param pigeonType Type of pigeon (Two, IMU, Dummy)
     * @param pigeonID Can ID of Pigeon
     * @param inverted Whether the gyroscope is inverted or not.
     */
    public Pigeon(Type pigeonType, int pigeonID, boolean inverted) {
        type = pigeonType;

        id = pigeonID;

        if (RobotBase.isSimulation()) {
            Logger.Log("Warning: Pigeon is not supported in simulation yet, using a dummy Pigeon");
            type = Type.DUMMY;
        }

        if (type == Type.IMU) {
            pigeonIMU = new PigeonIMU(id);
        } else if (type == Type.TWO) {
            pigeon2 = new Pigeon2(id);
        }

        this.inverted = inverted;
    }

    /**
     * Creates a new Pigeon gyroscope (non-inverted)
     * @param pigeonType Type of the pigeon (Two, IMU, Dummy)
     * @param pigeonID CAN id of pigeon.
     */
    public Pigeon(Type pigeonType, int pigeonID) {
        this(pigeonType, pigeonID, false);
    }

    /**
     * Sets up the pigeon gyroscope for use.
     */
    public void config() {
        if (type == Type.IMU) {
            pigeonIMU.configFactoryDefault();
        } else if (type == Type.TWO) {
            pigeon2.configFactoryDefault();
        }

        zero();
    }

    /**
     * Zeros out the gyroscope.
     */
    public void zero() {
        if (type == Type.IMU) {
            pigeonIMU.setYaw(0);
        } else if (type == Type.TWO) {
            pigeon2.setYaw(0);
        } else if (type == Type.DUMMY) {
            dummyAngle = 0;
        }
    }

    /**
     * Gets the rotation of the gyroscope.
     * @param inverted Whether to invert it or not.
     * @return Rotation of gyroscope.
     */
    public Rotation2d getAngle(boolean inverted) {
        double angle = 0;

        if (type == Type.IMU) {
            double[] ypr = new double[3];
            pigeonIMU.getYawPitchRoll(ypr);
            angle = ypr[0];
        } else if (type == Type.TWO) {
            angle = pigeon2.getYaw();
        } else if (type == Type.DUMMY) {
            angle = dummyAngle;
        }

        return inverted ? Rotation2d.fromDegrees(360 - angle) : Rotation2d.fromDegrees(angle);
    }

    /**
     * Gets the rotation of the gyroscope, inverted based on what was provided at creation.
     * @return Rotation of gyroscope.
     */
    public Rotation2d getAngle() {
        return getAngle(inverted);
    }

    /**
     * Gets the yaw, pitch, and roll of gyroscope.
     * @return [yaw, pitch, roll]
     */
    public double[] getRotation() {
        double[] rotation = new double[3];

        if (type == Type.IMU) {
            pigeonIMU.getYawPitchRoll(rotation);
        } else if (type == Type.TWO) {
            pigeon2.getYawPitchRoll(rotation);
        } else if (type == Type.DUMMY) {
            rotation[0] = dummyAngle;
            rotation[1] = 0;
            rotation[2] = 0;
        }

        return rotation;
    }

    public static enum Type {
        TWO,
        IMU,
        DUMMY
    }
}
