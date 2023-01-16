package frc.team_8840_lib.IO.devices;

import com.ctre.phoenix.sensors.CANCoder;
import com.ctre.phoenix.sensors.CANCoderConfiguration;

import edu.wpi.first.wpilibj.RobotBase;
import frc.team_8840_lib.utils.IO.IOAccess;
import frc.team_8840_lib.utils.IO.IOLayer;
import frc.team_8840_lib.utils.IO.IOMethod;
import frc.team_8840_lib.utils.IO.IOMethodType;
import frc.team_8840_lib.utils.IO.IOPermission;
import frc.team_8840_lib.utils.IO.IOValue;

@IOAccess(IOPermission.READ_WRITE)
public class IOCANCoder extends IOLayer {
    private int encoderPort = -1;

    private CANCoder encoder = null;
    
    private double cache = 0;

    public IOCANCoder(Object ...args) {
        super();

        this.encoderPort = (int) args[0];

        if (this.encoderPort >= 0) {
            this.encoder = new CANCoder(this.encoderPort);
        }

        this.setReal(!RobotBase.isSimulation());

        if (this.encoderPort < 0) {
            this.setReal(false);
        }
    }

    /**
     * Returns in degrees the position of the encoder
     * @return The position of the encoder in degrees
     */
    @IOMethod(name = "absolute position", method_type = IOMethodType.READ, value_type = IOValue.DOUBLE)
    public double getAbsolutePosition() {
        if (encoder == null) return 0;
        return isReal() ? encoder.getAbsolutePosition() : this.cache;
    }
    
    /**
     * Sets the cahced position of the encoder
     * @param value The degree amount to set the encoder to
     */
    @IOMethod(name = "set cached position", method_type = IOMethodType.WRITE, value_type = IOValue.DOUBLE)
    public void setCache(double value) {
        this.cache = value;
    }

    public void configAllSettings(CANCoderConfiguration config) {
        encoder.configAllSettings(config);
    }

    public void configFactoryDefault() {
        encoder.configFactoryDefault();
    }

    public String getBaseName() {
        return "CANCoder";
    }
}
