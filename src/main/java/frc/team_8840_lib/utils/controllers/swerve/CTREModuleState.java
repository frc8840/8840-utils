package frc.team_8840_lib.utils.controllers.swerve;

import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.kinematics.SwerveModuleState;

//From https://github.com/Team364/BaseFalconSwerve/blob/main/src/main/java/frc/lib/util/CTREModuleState.java
//All credit goes to them, but this has been modified a bit to be a tiny bit more efficient
//TODO: Change this to work better/make more efficient
public class CTREModuleState {

    /**
     * Minimize the change in heading the desired swerve module state would require by potentially
     * reversing the direction the wheel spins. Customized from WPILib's version to include placing
     * in appropriate scope for CTRE onboard control.
     *
     * @param desiredState The desired state.
     * @param currentAngle The current module angle.
     */
    public static SwerveModuleState optimize(SwerveModuleState desiredState, Rotation2d currentAngle) {
        double targetAngle = placeInAppropriate0To360Scope(currentAngle.getDegrees(), desiredState.angle.getDegrees());
        double targetSpeed = desiredState.speedMetersPerSecond;
        double delta = targetAngle - currentAngle.getDegrees();
        if (Math.abs(delta) > 90){
            targetSpeed = -targetSpeed;
            if (delta > 90) {
                targetAngle -= 180;
            } else {
                targetAngle += 180;
            }
        }
        return new SwerveModuleState(targetSpeed, Rotation2d.fromDegrees(targetAngle));
    }

    /**
     * @param scopeReference Current Angle
     * @param newAngle Target Angle
     * @return Closest angle within scope
     */
    private static double placeInAppropriate0To360Scope(double scopeReference, double newAngle) {
        double lowerOffset = scopeReference % 360;
        double lowerBound = scopeReference + -(lowerOffset >= 0 ? lowerOffset : 360 + lowerOffset);
        double upperBound = scopeReference + (lowerOffset >= 0 ? 360 - lowerOffset : -lowerOffset);
        while (newAngle < lowerBound) {
            newAngle += 360;
        }
        while (newAngle > upperBound) {
            newAngle -= 360;
        }
        if (newAngle - scopeReference > 180) {
            newAngle -= 360;
        } else if (newAngle - scopeReference < -180) {
            newAngle += 360;
        }
        return newAngle;
    }
}