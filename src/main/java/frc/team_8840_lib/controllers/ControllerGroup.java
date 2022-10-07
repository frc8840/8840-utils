package frc.team_8840_lib.controllers;

import com.revrobotics.CANSparkMax;
import com.revrobotics.CANSparkMaxLowLevel;
import com.revrobotics.RelativeEncoder;
import com.revrobotics.SparkMaxRelativeEncoder;
import edu.wpi.first.wpilibj.motorcontrol.PWMMotorController;
import frc.team_8840_lib.input.communication.CommunicationManager;
import frc.team_8840_lib.utils.controllers.EncoderInformation;
import frc.team_8840_lib.utils.controllers.MotorInfo;
import frc.team_8840_lib.utils.controllers.SCType;

import java.util.ArrayList;
import java.util.HashMap;

public class ControllerGroup {
    HashMap<Integer, SpeedController> controllers;

    private String name;

    public String getName() {
        return name;
    }

    private SCType defaultType;

    private HashMap<String, ArrayList<Integer>> subGroups;

    public ControllerGroup(String name, SCType mainType, int... ports) {
        this.name = name;

        controllers = new HashMap<>();

        this.defaultType = mainType;

        for (int port : ports) {
            addPort(port, mainType);
        }

        //this.updateComms();
    }

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

    public void addPort(int port) {
        addPort(port, defaultType);
    }

    public void invert() {
        controllers.values().forEach(SpeedController::invert);
    }

    public void invert(int... ports) {
        for (int port : ports) {
            if (!controllers.containsKey(port)) throw new IllegalArgumentException("Port " + port + " is not in use");
            controllers.get(port).invert();
        }
    }

    public void setSpeed(double speed) {
        controllers.values().forEach(sc -> sc.setSpeed(speed));
        updateComms();
    }

    public void setGroupSpeed(String name, double speed) {
        if (!isCombination()) throw new IllegalArgumentException("This is not a combination of speed controller groups.");
        if (!subGroups.containsKey(name)) throw new IllegalArgumentException("Group " + name + " is not in use");
        for (int port : subGroups.get(name)) {
            setPortSpeed(port, speed);
        }
        updateComms();
    }

    public void setPortSpeed(int port, double speed) {
        if (!controllers.containsKey(port)) throw new IllegalArgumentException("Port " + port + " is not in use");
        controllers.get(port).setSpeed(speed);
        updateComms();
    }

    public SpeedController getSpeedController(int port) {
        return controllers.get(port);
    }

    public void externalSet(int port, SpeedController controller) {
        controllers.put(port, controller);
    }

    public void externalSet(int port, SpeedController controller, String subGroup) {
        controllers.put(port, controller);
        if (subGroups == null) subGroups = new HashMap<>();

        if (!subGroups.containsKey(subGroup)) subGroups.put(subGroup, new ArrayList<>());
        subGroups.get(subGroup).add(port);
    }

    public boolean isCombination() {
        if (subGroups == null) {
            return false;
        }
        return subGroups.size() > 0;
    }

    public double getAverageSpeed() {
        double sum = 0;
        for (SpeedController sc : controllers.values()) {
            sum += sc.getSpeed();
        }
        return sum / controllers.size();
    }

    public double getAverageSpeed(String group) {
        double sum = 0;
        for (int port : subGroups.get(group)) {
            sum += controllers.get(port).getSpeed();
        }
        return sum / subGroups.get(group).size();
    }

    public double getSpeed(int port) {
        return controllers.get(port).getSpeed();
    }

    public double getSubGroupSpeed(String name) {
        if (!isCombination()) throw new IllegalArgumentException("Group is not a combination");
        if (!subGroups.containsKey(name)) throw new IllegalArgumentException("Group " + name + " is not in use");

        double sum = 0;
        for (int port : subGroups.get(name)) {
            sum += getSpeed(port);
        }
        return sum / subGroups.get(name).size();
    }

    public String[] getSubGroups() {
        return subGroups.keySet().toArray(new String[0]);
    }

    public static SpeedController createSC(int port, SCType type) {
        return new SpeedController(port, type);
    }

    public static class SpeedController {
        private int port;
        private SCType type;
        private PWMMotorController controller;

        private boolean isPWM;

        private double speed;

        private boolean initialized = false;

        public SpeedController(int port, SCType type) {
            this.port = port;
            this.type = type;
            this.controller = null;

            isPWM = type.isPWM();

            speed = 0;

            initialize();
        }

        public SpeedController(SCType type) {
            this.type = type;
            this.controller = null;

            isPWM = type.isPWM();

            speed = 0;

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

            if (speed == 0) {
                controller.stopMotor();
            } else {
                controller.set(speed);
            }
        }

        /**
         * Evolves the speed controller into a speed controller group. Speed Controller Groups send the info to the NetworkTables, so doing this will allow the info to send.
         */
        public ControllerGroup evolve(String name) {
            ControllerGroup group = new ControllerGroup(name, type);
            group.externalSet(port, this);
            return group;
        }

        public double getSpeed() {
            return speed;
        }

        public PWMMotorController getController() {
            return controller;
        }

        public SCType getType() {
            return type;
        }

        public int getPort() {
            return port;
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
    }

    public static class CANSpeedController extends SpeedController {
        private CANSparkMax controller;
        private final boolean isBrushed;

        public CANSpeedController(int port, boolean isBrushed, SCType type) {
            super(port, type);
            this.isBrushed = isBrushed;
        }

        private void initialize() {
            if (this.isInitialized()) return;

            if (!this.isPWM()) {
                this.controller = new CANSparkMax(this.getPort(), isBrushed ? CANSparkMaxLowLevel.MotorType.kBrushed : CANSparkMaxLowLevel.MotorType.kBrushless);
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

    public static ControllerGroup combine(String name, ControllerGroup... groups) {
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
