package frc.team_8840_lib.IO.devices;

import com.ctre.phoenix.sensors.CANCoder;
import com.ctre.phoenix.sensors.CANCoderConfiguration;

import edu.wpi.first.wpilibj.RobotBase;
import frc.team_8840_lib.info.console.Logger;
import frc.team_8840_lib.listeners.Robot;
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
            Logger.Log("[IO] Initializing (real" + (Robot.isSimulation() ? " <in simulation>" : "") + ") CANCoder on port " + this.encoderPort);
            this.encoder = new CANCoder(this.encoderPort);
        } else {
            Logger.Log("[IO] Initializing (simulated) CANCoder on port " + this.encoderPort);
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
        //Check if the robot is real, and if the encoder is null. If it is, log a warning.
        if (Robot.isReal() && encoder == null) {
            Logger.Log("[" + getBaseName() + "] WARNING: CANCoder is null, and you're getting the absolute position.");
        }
        //If the encoder is null, or the encoder port is less than 0 (used mainly for testing), return the cache.
        //If the robot is real, return 0 if it satisfies the above conditions.
        if (encoder == null || this.encoderPort < 0) return isReal() ? 0 : this.cache;
        
        //Return the absolute position of the encoder, or the cache if the robot is not real.
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

    /**
     * Configures all settings of the CANCoder
     * @param config The configuration to set
     */
    public void configAllSettings(CANCoderConfiguration config) {
        if (encoder == null || this.encoderPort < 0) return;
        
        Logger.Log("[IO] Configuring CANCoder on port " + this.encoderPort);

        try {
            encoder.configAllSettings(config);
        } catch (Exception e) {
            Logger.Log("[IO] Error configuring CANCoder on port " + this.encoderPort);
            e.printStackTrace();
        }
    }

    public void configFactoryDefault() {
        if (encoder == null || this.encoderPort < 0) return;

        Logger.Log("[IO] Configuring to factory default CANCoder on port " + this.encoderPort);
        
        try {
            encoder.configFactoryDefault();
        } catch (Exception e) {
            Logger.Log("[IO] Error configuring to factory default CANCoder on port " + this.encoderPort);
            e.printStackTrace();
        }
    }

    public String getBaseName() {
        return "CANCoder";
    }
}
