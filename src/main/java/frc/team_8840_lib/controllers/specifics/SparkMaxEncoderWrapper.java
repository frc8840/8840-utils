package frc.team_8840_lib.controllers.specifics;

import com.revrobotics.CANSparkMax;
import com.revrobotics.REVLibError;
import com.revrobotics.RelativeEncoder;

import frc.team_8840_lib.info.console.Logger;

/**
 * A wrapper to stop issues with the SparkMax encoder. We've had a lot of issues
 * with REV's api, so this is a wrapper to help with that.
 * 
 * @author Jaiden Grimminck
 */
public class SparkMaxEncoderWrapper {
    private RelativeEncoder encoder;

    private double offset = 0; //Offset is subtracted from the position

    private double positionConversionFactor = 1;
    private double velocityConversionFactor = 1;

    private boolean doManualOffset = false;
    private boolean doManualConversion = false;

    /**
     * Creates a new SparkMaxEncoderWrapper
     * 
     * @param sparkMax The speed controller to get the encoder from
     */
    public SparkMaxEncoderWrapper(CANSparkMax sparkMax) {
        encoder = sparkMax.getEncoder();
    }

    /**
     * Returns the encoder
     * @return relative encoder
     */
    public RelativeEncoder getEncoder() {
        return encoder;
    }

    /**
     * Changes whether or not the encoder is inverted.
     * @param inverted Whether or not the encoder is inverted
     * @return REVLibError
     */
    public REVLibError setInverted(boolean inverted) {
        if (encoder == null) {
            Logger.Log("[SparkMaxEncoder] WARNING: Encoder is null, and you're setting the inverted.");
            return REVLibError.kError;
        }

        return encoder.setInverted(inverted);
    }

    /**
     * Sets the offset of the encoder
     * @return The position of the encoder
     */
    public double getPosition() {
        if (encoder == null) {
            Logger.Log("[SparkMaxEncoder] WARNING: Encoder is null, and you're getting the position.");
            return 0;
        }

        return (encoder.getPosition() * (doManualConversion ? positionConversionFactor : 1)) + (doManualOffset ? offset : 0);
    }

    /**
     * Gets the velocity of the encoder
     * @return The velocity of the encoder
     */
    public double getVelocity() {
        if (encoder == null) {
            Logger.Log("[SparkMaxEncoder] WARNING: Encoder is null, and you're getting the velocity.");
            return 0;
        }

        return encoder.getVelocity() * (doManualConversion ? velocityConversionFactor : 1);
    }

    /**
     * Set whether to do manual conversion (true) or not (false) and use REV's API.
     * @param doManualConversion Whether to do manual conversion (true) or not (false)
     */
    public void setManualConversion(boolean doManualConversion) {
        this.doManualConversion = doManualConversion;
    }

    /**
     * Set whether to do manual offset (true) or not (false) and use REV's API.
     * @param doManualOffset Whether to do manual offset (true) or not (false)
     */
    public void setManualOffset(boolean doManualOffset) {
        this.doManualOffset = doManualOffset;
    }

    /**
     * Sets the position conversion factor
     * @param positionConversionFactor The position conversion factor
     * @return REVLibError
     */
    public REVLibError setPositionConversionFactor(double positionConversionFactor) {
        this.positionConversionFactor = positionConversionFactor;

        if (!this.doManualConversion) {
            return encoder.setPositionConversionFactor(positionConversionFactor);
        } else {
            return REVLibError.kOk;
        }
    }

    /**
     * Sets the velocity conversion factor
     * @param velocityConversionFactor The velocity conversion factor
     * @return REVLibError
     */
    public REVLibError setVelocityConversionFactor(double velocityConversionFactor) {
        this.velocityConversionFactor = velocityConversionFactor;

        if (!this.doManualConversion) {
            return encoder.setVelocityConversionFactor(velocityConversionFactor);
        } else {
            return REVLibError.kOk;
        }
    }

    /**
     * Sets the position of the encoder. Note: In conversion, this is subtracted from the position, not added3
     * @param offset The offset of the encoder (the new position)
     * @return REVLibError
     */
    public REVLibError setPosition(double offset) {
        this.offset = offset;

        if (!this.doManualOffset) {
            return encoder.setPosition(offset);
        } else {
            return REVLibError.kOk;
        }
    }

    /**
     * Calculates the position using the offset and conversion factor. Uses the booleans to determine whether to use REV's API or not.
     * @return Calculated position
     */
    public double calculatePosition(double position) {
        return (position / (doManualConversion ? positionConversionFactor : 1)) + (doManualOffset ? offset : 0);
    }

    /**
     * Calculates the position using the offset and conversion factor. Uses the preset booleans for REV's API, and an argument for the offset.
     * @param position The position to calculate
     * @param ignoreOffset Whether to ignore the offset or not
     * @return Calculated position
     */
    public double calculatePosition(double position, boolean ignoreOffset) {
        return (position / (doManualConversion ? positionConversionFactor : 1)) + (doManualOffset && !ignoreOffset ? offset : 0);
    }

    /**
     * Calculates the velocity using the conversion factor. Uses the booleans to determine whether to use REV's API or not.
     * @return Calculated velocity
     */
    public double calculateVelocity(double velocity) {
        return velocity / (doManualConversion ? velocityConversionFactor : 1);
    }

}
