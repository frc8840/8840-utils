package frc.team_8840_lib.controllers;

import com.revrobotics.CANSparkMax;
import com.revrobotics.RelativeEncoder;
import com.revrobotics.CANSparkMaxLowLevel.MotorType;

import edu.wpi.first.wpilibj.motorcontrol.PWMMotorController;
import frc.team_8840_lib.input.communication.CommunicationManager;
import frc.team_8840_lib.utils.IO.IOAccess;
import frc.team_8840_lib.utils.IO.IOLayer;
import frc.team_8840_lib.utils.IO.IOMethod;
import frc.team_8840_lib.utils.IO.IOMethodType;
import frc.team_8840_lib.utils.IO.IOPermission;
import frc.team_8840_lib.utils.IO.IOValue;
import frc.team_8840_lib.utils.controllers.EncoderInformation;
import frc.team_8840_lib.utils.controllers.MotorInfo;
import frc.team_8840_lib.utils.controllers.SCType;

import java.util.ArrayList;
import java.util.HashMap;

public class ControllerGroup {
    HashMap<Integer, SpeedController> controllers;

    private String name;

    /**
     * Name of controller group
     * @return name
     */
    public String getName() {
        return name;
    }

    private SCType defaultType;

    private HashMap<String, ArrayList<Integer>> subGroups;

    /**
     * Creates a new controller group
     * @param name Name of the group
     * @param mainType Type of the ports
     * @param ports PWM Ports/CAN IDs of the controllers
     * @deprecated
     */
    @Deprecated(forRemoval = true)
    public ControllerGroup(String name, SCType mainType, int... ports) {
        this.name = name;

        controllers = new HashMap<>();

        this.defaultType = mainType;

        for (int port : ports) {
            addPort(port, mainType);
        }

        //this.updateComms();
    }

    /**
     * Adds a new controller to the controller group.
     * @param port Port of controller
     * @param type Type of controller
     */
    public void addPort(int port, SCType type) {
        if (controllers.containsKey(port)) {
            throw new IllegalArgumentException("Port " + port + " is already in use");
        }
        if (type.isPWM()) {
            controllers.put(port, new SpeedController(port, type));
        } else {
            controllers.put(port, new CANSpeedController(port, type.isBrushed(), type));
        }
    }

    /**
     * Adds a port with default type of group
     * @param port
     */
    public void addPort(int port) {
        addPort(port, defaultType);
    }

    /**
     * Invert every controller part of group.
     */
    public void invert() {
        controllers.values().forEach(SpeedController::invert);
    }

    /**
     * Invert only certain ports of the group
     * @param ports Ports
     */
    public void invert(int... ports) {
        for (int port : ports) {
            if (!controllers.containsKey(port)) throw new IllegalArgumentException("Port " + port + " is not in use");
            controllers.get(port).invert();
        }
    }

    /**
     * Sets the speed of the entire group
     * @param speed Speed (-1 to 1)
     */
    public void setSpeed(double speed) {
        controllers.values().forEach(sc -> sc.setSpeed(speed));
        updateComms();
    }

    /**
     * Sets the speed of a subgroup of the group
     * @param name Name of subgroup
     * @param speed Speed of subgroup (-1 to 1)
     */
    public void setSubGroupSpeed(String name, double speed) {
        if (!isCombination()) throw new IllegalArgumentException("This is not a combination of speed controller groups.");
        if (!subGroups.containsKey(name)) throw new IllegalArgumentException("Group " + name + " is not in use");
        for (int port : subGroups.get(name)) {
            setPortSpeed(port, speed);
        }
        updateComms();
    }

    /**
     * Sets the speed of a certain port
     * @param port SpeedController port
     * @param speed Speed (-1, 1)
     */
    public void setPortSpeed(int port, double speed) {
        if (!controllers.containsKey(port)) throw new IllegalArgumentException("Port " + port + " is not in use");
        controllers.get(port).setSpeed(speed);
        updateComms();
    }

    /**
     * Gets the speed controller object of a port
     * @param port port
     * @return SpeedController on port
     */
    public SpeedController getSpeedController(int port) {
        return controllers.get(port);
    }

    /**
     * Returns a list of the controllers that are part of the group
     * @return list of speed controller objects
     */
    public SpeedController[] getControllers() {
        return controllers.values().toArray(new SpeedController[0]);
    }

    /**
     * Returns a list of speeds in subgroup matched up to their ports
     * @param key Subgroup name
     * @return HashMap<Port, Speed>
     */
    public HashMap<Integer, Double> subgroupSpeeds(String key) {
        if (!isCombination()) throw new IllegalArgumentException("This is not a combination of speed controller groups.");
        if (!subGroups.containsKey(key)) throw new IllegalArgumentException("Group " + key + " is not in use");

        ArrayList<Integer> ports = subGroups.get(key);
        HashMap<Integer, Double> speeds = new HashMap<>();
        for (Integer port : ports) {
            speeds.put(port, controllers.get(port).getSpeed());
        }

        return speeds;
    }

    /**
     * Externally put in the port and speed controller object
     * @param port Port of controller
     * @param controller SpeedController object
     */
    public void externalSet(int port, SpeedController controller) {
        controllers.put(port, controller);
    }

    /**
     * Externally put in the controller of a subgroup
     * @param port Port of speed controller
     * @param controller SpeedController object
     * @param subGroup Subgroup key of controller
     */
    public void externalSet(int port, SpeedController controller, String subGroup) {
        controllers.put(port, controller);
        if (subGroups == null) subGroups = new HashMap<>();

        if (!subGroups.containsKey(subGroup)) subGroups.put(subGroup, new ArrayList<>());
        subGroups.get(subGroup).add(port);
    }

    /**
     * Returns whether the group has subgroups or not
     * @return has subgroups/is combination of groups?
     */
    public boolean isCombination() {
        if (subGroups == null) {
            return false;
        }
        return subGroups.size() > 0;
    }

    /**
     * Gets the average speed of the controllers
     * @return Average speed.
     */
    public double getAverageSpeed() {
        double sum = 0;
        for (SpeedController sc : controllers.values()) {
            sum += sc.getSpeed();
        }
        return sum / controllers.size();
    }

    /**
     * Returns the average speed of a subgroup
     * @param group Subgroup name
     * @return Average speed of subgroup
     */
    public double getAverageSpeed(String group) {
        if (!isCombination()) throw new IllegalArgumentException("Cannot call, ControllerGroup is not a combination of groups");
        if (!subGroups.containsKey(group)) throw new IllegalArgumentException("Group " + group + " is not in use");

        double sum = 0;
        for (int port : subGroups.get(group)) {
            sum += controllers.get(port).getSpeed();
        }
        return sum / subGroups.get(group).size();
    }

    /**
     * Gets the speed of a specific controller at port
     * @param port
     * @return Speed of controller
     */
    public double getSpeed(int port) {
        return controllers.get(port).getSpeed();
    }

    /**
     * Gets a list of names of the subgroups.
     * @return Subgroup names
     */
    public String[] getSubGroups() {
        return subGroups.keySet().toArray(new String[0]);
    }

    /**
     * Creates a speed controller at port with type 
     * @param port Port of speed controller
     * @param type Type of speed controller
     * @deprecated
     * @return SpeedController
     */
    @Deprecated(forRemoval = true)
    public static SpeedController createSC(int port, SCType type) {
        return new SpeedController(port, type);
    }

    /**
     * Stops the controller group by setting the speed to 0.
     */
    public void stop() {
        setSpeed(0);
    }

    @IOAccess(IOPermission.READ_WRITE)
    public static class SpeedController extends IOLayer {
        private int port;
        private SCType type;
        private PWMMotorController controller;

        private boolean isPWM;

        private double speed;

        private boolean initialized = false;

        /**
         * Creates a speed controller at port with type
         * @param port
         * @param type
         * @deprecated
         */
        @Deprecated(forRemoval = true)
        public SpeedController(int port, SCType type) {
            this.port = port;
            this.type = type;
            this.controller = null;

            isPWM = type.isPWM();

            speed = 0;

            setReal(true);

            initialize();
        }

        public SpeedController(SCType type) {
            this.type = type;
            this.controller = null;

            isPWM = type.isPWM();

            speed = 0;

            setReal(false);

            initialized = true; //ignore initialization for this one.
        }

        private void initialize() {
            if (initialized) return;

            if (isPWM) {
                controller = this.type.create(port);
            } else throw new UnsupportedOperationException("Cannot create SpeedController for non-PWM type, instead use class CANSpeedController.");

            initialized = true;
        }

        public void invert() {
            controller.setInverted(!controller.getInverted());
        }

        public void setSpeed(double speed) {
            this.speed = speed;
            
            if (!isReal()) return;

            if (speed == 0) {
                controller.stopMotor();
            } else {
                controller.set(speed);
            }
        }

        public void stop() {
            setSpeed(0);
        }

        /**
         * Evolves the speed controller into a speed controller group. Speed Controller Groups send the info to the NetworkTables, so doing this will allow the info to send.
         */
        public ControllerGroup evolve(String name) {
            ControllerGroup group = new ControllerGroup(name, type);
            group.externalSet(port, this);
            return group;
        }

        @IOMethod(name = "Speed", value_type = IOValue.DOUBLE, method_type = IOMethodType.READ)
        public double getSpeed() {
            return speed;
        }

        public PWMMotorController getController() {
            return controller;
        }

        public SCType getType() {
            return type;
        }

        @IOMethod(name = "Port", value_type = IOValue.INT, method_type = IOMethodType.READ)
        public int getPort() {
            return port;
        }

        @IOMethod(name = "Initialized", value_type = IOValue.STRING, method_type = IOMethodType.READ)
        public String getInitializedName() {
            return initialized ? "Yes" : "No";
        }

        public boolean isInitialized() {
            return initialized;
        }

        void setInitialized() {
            this.initialized = true;
        }

        public boolean isPWM() {
            return isPWM;
        }

        @IOMethod(name = "Set Speed", value_type = IOValue.DOUBLE, method_type = IOMethodType.WRITE)
        public void setSpeedIO(double speed) {
            setSpeed(speed);
        }
    }

    public static class CANSpeedController extends SpeedController {
        private CANSparkMax controller;
        private final boolean isBrushed;

        public CANSpeedController(int port, boolean isBrushed, SCType type) {
            super(port, type);
            this.isBrushed = isBrushed;

            this.initialize();
        }

        private void initialize() {
            if (this.isInitialized()) return;

            if (!this.isPWM()) {
                this.controller = new CANSparkMax(this.getPort(), isBrushed ? MotorType.kBrushed : MotorType.kBrushless);
            } else throw new UnsupportedOperationException("Cannot create CANSpeedController for PWM type, instead use class SpeedController.");

            this.setInitialized();
        }

        public RelativeEncoder getEncoder() {
            return controller.getEncoder();
        }

        public void setEncoderPosition(double position) {
            getEncoder().setPosition(position);
        }

        public EncoderInformation getEncoderInfo() {
            return EncoderInformation.fromEncoder(getEncoder());
        }

        public MotorInfo getMotorInfo() {
            return MotorInfo.fromController(controller);
        }

        public CANSparkMax getCANController() {
            return this.controller;
        }

        public PWMMotorController getController() {
            throw new IllegalArgumentException("CANSpeedController does not have a PWM controller");
        }
    }

    /**
     * Combines two or more groups to make another group
     * @param name Name of combination
     * @param groups Groups to be combined
     * @return Combined controller group.
     */
    public static ControllerGroup combine(String name, ControllerGroup... groups) {
        if (groups.length < 2) throw new IllegalArgumentException("Need two or more groups to make a combination.");

        ControllerGroup base = new ControllerGroup(name, groups[0].defaultType);
        for (ControllerGroup group : groups) {
            for (int port : group.controllers.keySet()) {
                base.externalSet(port, group.getSpeedController(port), group.getName());
            }
        }

        return base;
    }

    private void updateComms() {
        CommunicationManager.getInstance().updateSpeedControllerInfo(this);
    }
}
