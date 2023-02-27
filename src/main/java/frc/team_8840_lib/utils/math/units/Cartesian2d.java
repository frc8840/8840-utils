package frc.team_8840_lib.utils.math.units;

import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;

/**
 * A class to represent a 2d cartesian coordinate. 
 * 
 * @author Jaiden Grimminck
 */
public class Cartesian2d {
    private double r;
    private Rotation2d theta;

    /**
     * Creates a new Cartesian2d
     * @param r The radius
     * @param theta The angle
     */
    public Cartesian2d(double r, Rotation2d theta) {
        assert r >= 0 : "Radius must be positive!";
        
        if (theta == null) {
            throw new IllegalArgumentException("Angle cannot be null!");
        }

        //Run incase assert is disabled
        if (r < 0) {
            throw new IllegalArgumentException("Radius must be positive!");
        }

        this.r = r;
        this.theta = theta;
    }

    /**
     * Returns the radius
     * @return radius
     */
    public double getR() {
        return r;
    }

    /**
     * Returns the angle
     * @return angle (Rotation2d)
     */
    public Rotation2d getTheta() {
        return theta;
    }

    /**
     * Returns the angle in degrees
     * @param other
     * @return
     */
    public Cartesian2d add(Cartesian2d other) {
        return new Cartesian2d(
            this.r + other.r,
            this.theta.plus(other.theta)
        );
    }

    /**
     * Converts class into a Translation2d
     * @return Translation2d
     */
    public Translation2d toTranslation2d() {
        return new Translation2d(r * Math.cos(theta.getRadians()), r * Math.sin(theta.getRadians()));
    }

    /**
     * Converts a Translation2d into a Cartesian2d
     * @param translation2d The Translation2d to convert
     * @return Cartesian2d
     */
    public static Cartesian2d fromTranslation2d(Translation2d translation2d) {
        return new Cartesian2d(
            Math.hypot(translation2d.getX(), translation2d.getY()),
            new Rotation2d(Math.atan2(translation2d.getY(), translation2d.getX()))
        );
    }
}