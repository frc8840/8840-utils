package frc.team_8840_lib.IO.devices;

import edu.wpi.first.wpilibj.PowerDistribution;
import edu.wpi.first.wpilibj.RobotBase;
import frc.team_8840_lib.info.console.AutoLog;
import frc.team_8840_lib.info.console.Logger;
import frc.team_8840_lib.info.console.Logger.LogType;
import frc.team_8840_lib.utils.IO.IOAccess;
import frc.team_8840_lib.utils.IO.IOLayer;
import frc.team_8840_lib.utils.IO.IOMethod;
import frc.team_8840_lib.utils.IO.IOMethodType;
import frc.team_8840_lib.utils.IO.IOPermission;
import frc.team_8840_lib.utils.IO.IOValue;
import frc.team_8840_lib.utils.buffer.ByteConversions;
import frc.team_8840_lib.utils.logging.Loggable;

@IOAccess(IOPermission.READ_WRITE)
public class IOPowerDistribution extends IOLayer implements Loggable {
    private static IOPowerDistribution instance;
    public static void init() {
        if (instance == null) {
            instance = new IOPowerDistribution();
        }
    }

    public static IOPowerDistribution getInstance() {
        if (instance == null) {
            Logger.Log("IOPowerDistribution has not been initialized");
            return null;
        }

        return instance;
    }

    private int numberOfPDChannels = 16;

    private double[] simulatedCurrents = new double[numberOfPDChannels];
    private double simulatedVoltage = 12;

    private PowerDistribution pd = null;

    public IOPowerDistribution() {
        super();

        setReal(!RobotBase.isSimulation());

        pd = new PowerDistribution();
        numberOfPDChannels = pd.getNumChannels() <= 0 ? 16 : pd.getNumChannels();

        simulatedCurrents = new double[numberOfPDChannels];

        for (int i = 0; i < numberOfPDChannels; i++) {
            simulatedCurrents[i] = 0;
        }

        simulatedVoltage = 12;

        Logger.addClassToBeAutoLogged(this);
    }

    /**
     * Returns the current of the specified PDP channel
     * @param channel The PDP channel to get the current of
     * @return The current of the specified PDP channel
     */
    public double getCurrent(int channel) {
        return isReal() ? pd.getCurrent(channel) : simulatedCurrents[channel];
    }

    @IOMethod(name = "set current [channel, current]", method_type = IOMethodType.WRITE, value_type = IOValue.DOUBLE_ARRAY)
    public void setCurrent(double[] channel_current) {
        //Arg 0 will be the channel number, arg 1 will be the current
        if (channel_current.length != 2) return;

        int channel = (int) Math.round(channel_current[0]);

        if (channel < 0 || channel >= numberOfPDChannels) return;

        simulatedCurrents[channel] = channel_current[1];
    }

    /**
     * Returns a double array of the currents of all the PDP channels
     * @return The currents of all the PDP channels
     */
    @IOMethod(name = "current", method_type = IOMethodType.READ, value_type = IOValue.DOUBLE_ARRAY)
    public double[] getCurrents() {
        double[] currents = new double[numberOfPDChannels];

        for (int i = 0; i < numberOfPDChannels; i++) {
            currents[i] = getCurrent(i);
        }

        return currents;
    }

    @IOMethod(name = "voltage", method_type = IOMethodType.READ, value_type = IOValue.DOUBLE)
    public double getVoltage() {
        return isReal() ? pd.getVoltage() : simulatedVoltage;
    }

    @IOMethod(name = "set voltage", method_type = IOMethodType.WRITE, value_type = IOValue.DOUBLE)
    public void setSimulatedVoltage(double voltage) {
        simulatedVoltage = voltage;
    }

    @IOMethod(name = "number of channels", method_type = IOMethodType.READ, value_type = IOValue.INT)
    public int getNumberOfChannels() {
        return numberOfPDChannels;
    }

    @IOMethod(name = "tempature", method_type = IOMethodType.READ, value_type = IOValue.DOUBLE)
    public double getTempature() {
        return isReal() ? pd.getTemperature() : 0;
    }

    public double getTotalCurrent() {
        double simTotal = 0;
        for (int i = 0; i < numberOfPDChannels; i++) {
            simTotal += simulatedCurrents[i];
        }
        return isReal() ? pd.getTotalCurrent() : simTotal;
    }

    public void close() {
        pd.close();
    }

    public String getBaseName() {
        return "Power Distribution";
    }

    @AutoLog(logtype = LogType.BYTE_ARRAY, name = "Power Distribution Info")
    public byte[] logPD() {
        int nOfChannels = getNumberOfChannels();

        double tempature = getTempature();
        double totalCurrent = getTotalCurrent();
        double voltage = getVoltage();

        //Format: 
        /**
         * 0: voltage
         * 1: totalCurrent
         * 2: empty (0)
         * 3: tempature
         * 4: empty (0)
         * 5: number of channels
         * 6: empty (0)
         * 7+: current of channel 0 to n
         */

        try {
            byte[] voltageEncoded = ByteConversions.doubleToByteArray(voltage);
            byte[] nOfChannelsEncoded = ByteConversions.doubleToByteArray((double) nOfChannels);
            byte[] tempatureEncoded = ByteConversions.doubleToByteArray(tempature);
            byte[] totalCurrentEncoded = ByteConversions.doubleToByteArray(totalCurrent);

            /*
             * Format is as follow
             * voltage,
             * 0x00000000,
             * tempature,
             * 0x00000000,
             * total current,
             * 0x00000000,
             * nOfChannels,
             * 0x00000000,
             * current of channel 0,
             * current of channel 1,
             * etc..
             */

            byte[] data = new byte[4 + voltageEncoded.length + tempatureEncoded.length + totalCurrentEncoded.length + nOfChannelsEncoded.length + (8 * nOfChannels)];

            int index = 0;

            for (int i = 0; i < voltageEncoded.length; i++) {
                data[index] = voltageEncoded[i];
                index++;
            }

            data[index] = 0x00;

            index++;

            for (int i = 0; i < tempatureEncoded.length; i++) {
                data[index] = tempatureEncoded[i];
                index++;
            }

            data[index] = 0x00;

            index++;

            for (int i = 0; i < totalCurrentEncoded.length; i++) {
                data[index] = tempatureEncoded[i];
                index++;
            }

            data[index] = 0x00;

            index++;

            for (int i = 0; i < nOfChannelsEncoded.length; i++) {
                data[index] = nOfChannelsEncoded[i];
                index++;
            }

            data[index] = 0x00;

            index++;

            for (int i = 0; i < nOfChannels; i++) {
                byte[] currentEncoded = ByteConversions.doubleToByteArray(getCurrent(i));

                for (int j = 0; j < currentEncoded.length; j++) {
                    data[index] = currentEncoded[j];
                    index++;
                }
            }

            return data;
        } catch (Exception e) {
            return new byte[0];
        }
    }
}
