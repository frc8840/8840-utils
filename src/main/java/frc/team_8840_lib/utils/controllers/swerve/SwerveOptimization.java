package frc.team_8840_lib.utils.controllers.swerve;

import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import frc.team_8840_lib.utils.math.MathUtils;

/**
 * A class that contains methods to optimize the swerve module states.
 * 
 * @author Jaiden Grimminck
 */
public class SwerveOptimization {

    /**
     * Optimizes the swerve module state to minimize the angle change between the current angle and the target angle.
     * @param state The target state
     * @param currentRotation The current rotation of the module
     * @return The optimized state
     */
    public static SwerveModuleState optimize(SwerveModuleState state, Rotation2d currentRotation, double lastSetSpeed) {
        double currentAngle = currentRotation.getDegrees();
        double targetAngle = state.angle.getDegrees();
        double currentSpeed = state.speedMetersPerSecond;

        //We can start off by making sure that the target angle is between 0 and 360.
        targetAngle = MathUtils.normalizeAngle(targetAngle);

        //We also need to make sure that the current angle is between 0 and 360.
        currentAngle = MathUtils.normalizeAngle(currentAngle);

        /**
         * The goal of this function is to minimize the angle change between the current angle and the target angle.
         * The angle change is minimized by finding the angle that is the shortest distance from the current angle.
         */

        //We can start off by finding the angle opposite to the target.
        double oppositeTarget = targetAngle - 180;

        //We can then find the distances to the target and the opposite target.
        double distanceToTarget = Math.abs(targetAngle - currentAngle);
        double distanceToOppositeTarget = Math.abs(oppositeTarget - currentAngle);

        //Comparing the distances, we can find the shortest distance.
        if (distanceToTarget < distanceToOppositeTarget) {
            //If the already found target is the shortest distance, we can just return the target.
            return new SwerveModuleState(currentSpeed, state.angle);
        } else {
            //Else, we can return the opposite target, but with the opposite speed.
            return new SwerveModuleState(-currentSpeed, Rotation2d.fromDegrees(oppositeTarget));
        }
    }
}
