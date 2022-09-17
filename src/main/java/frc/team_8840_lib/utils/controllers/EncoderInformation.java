package frc.team_8840_lib.utils.controllers;

import com.revrobotics.RelativeEncoder;

public class EncoderInformation {
    public final double velocity;
    public final double position;
    public final boolean isInverted;

    public final double velocityConversionFactor;
    public final double positionConversionFactor;

    public EncoderInformation(double velocity, double position, boolean isInverted, double velocityConversionFactor, double positionConversionFactor) {
        this.velocity = velocity;
        this.position = position;
        this.isInverted = isInverted;
        this.velocityConversionFactor = velocityConversionFactor;
        this.positionConversionFactor = positionConversionFactor;
    }

    public static EncoderInformation fromEncoder(RelativeEncoder encoder) {
        return new EncoderInformation(encoder.getVelocity(), encoder.getPosition(), encoder.getInverted(), encoder.getVelocityConversionFactor(), encoder.getPositionConversionFactor());
    }
}
