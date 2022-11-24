package frc.team_8840_lib.utils.controllers;

import com.revrobotics.CANSparkMax;
import edu.wpi.first.wpilibj.motorcontrol.*;

public enum SCType {
    PWM_SparkMax,
    SWERVE_SparkMax, //Specifically used for swerve drive
    CAN_SparkMaxBrushless,
    CAN_SparkMaxBrushed,
    Spark,
    DMC60,
    Jaguar,
    NidecBrushless,
    PWM_Talon_FX,
    SWERVE_Talon_FX, //Specifically used for swerve drive
    PWM_Talon_SRX,
    PWM_Venom,
    PWM_Victor_SPX,
    SD540,
    Talon,
    Victor,
    Victor_SP;

    public PWMMotorController createController(int port, SCType type) {
        switch (type) {
            case PWM_SparkMax:
                return new PWMSparkMax(port);
            case Spark:
                return new Spark(port);
            case DMC60:
                return new DMC60(port);
            case Jaguar:
                return new Jaguar(port);
            case NidecBrushless:
                //This is opted out for
                throw new UnsupportedOperationException("Nidec Brushless is not supported in this function due to the fact it also needs a DIO port.");
            case PWM_Talon_FX:
                return new PWMTalonFX(port);
            case PWM_Talon_SRX:
                return new PWMTalonSRX(port);
            case PWM_Venom:
                return new PWMVenom(port);
            case PWM_Victor_SPX:
                return new PWMVictorSPX(port);
            case SD540:
                return new SD540(port);
            case Talon:
                return new Talon(port);
            case Victor:
                return new Victor(port);
            case Victor_SP:
                return new VictorSP(port);
            default:
                return null;
        }
    }

    public NidecBrushless createNB(int port, int dioPort) {
        return new NidecBrushless(port, dioPort);
    }

    public static boolean isPWM(SCType type) {
        if (type == CAN_SparkMaxBrushed || type == CAN_SparkMaxBrushless) {
            return false;
        } else if (type == PWM_SparkMax || type == Spark || type == DMC60 || type == Jaguar || type == NidecBrushless || type == PWM_Talon_FX || type == PWM_Talon_SRX || type == PWM_Venom || type == PWM_Victor_SPX || type == SD540 || type == Talon || type == Victor || type == Victor_SP || type == SWERVE_Talon_FX || type == SWERVE_SparkMax) {
            return true;
        } else {
            throw new UnsupportedOperationException("This controller type is not supported by this function.");
        }
    }

    public boolean isPWM() {
        return isPWM(this);
    }

    public PWMMotorController create(int port) {
        return createController(port, this);
    }

    public boolean isBrushed() {
        if (!this.isPWM()) throw new UnsupportedOperationException("This function is only supported for PWM controllers.");
        return this == CAN_SparkMaxBrushed;
    }
}
