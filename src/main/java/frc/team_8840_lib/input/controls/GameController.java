package frc.team_8840_lib.input.controls;

import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.GenericHID;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.XboxController;
import frc.team_8840_lib.info.console.Logger;
import frc.team_8840_lib.input.communication.CommunicationManager;
import frc.team_8840_lib.utils.controls.Axis;
import frc.team_8840_lib.utils.controls.Buttons;

import java.util.HashMap;

public class GameController {
    private static HashMap<Integer, GameController> controllers = new HashMap<>();

    public static GameController get(int port) {
        if (!controllers.containsKey(port)) {
            controllers.put(port, new GameController(port));
        }
        return controllers.get(port);
    }

    public static void addController(int port) {
        controllers.put(port, new GameController(port));
    }

    public static void expectController(int port, Type type) {
        controllers.put(port, new GameController(port, type));
    }

    public static void autoConnect() {
        for (int port = 0; port < 6; port++) {
            if (DriverStation.isJoystickConnected(port)) {
                addController(port);
                Logger.Log("Found controller on port " + port);
            }
        }
    }

    private GenericHID controller;
    private int port;
    private boolean connected;
    private Type type;
    private double threshold = 0.1;
    private boolean inverted = false;

    @Deprecated
    public GameController(int port) {
        this.port = port;
        if (checkIfConnected()) {
            type = Type.detectType(port);
            System.out.println("Successfully connected to controller (type " + type.name().toUpperCase() + ") on port " + port + "!");
            controller = type == Type.Xbox ? new XboxController(port) : (type == Type.Joystick ? new Joystick(port) : new GenericHID(port));
            if (type == Type.Joystick) {
                inverted = true;
                threshold = 0.15;
            }
        } else System.out.println("Controller " + port + " is not connected");
    }

    private boolean awaitingForConnection = false;

    @Deprecated
    public GameController(int port, Type type) {
        this.port = port;
        this.type = type;
        this.awaitingForConnection = true;

        controller = type == Type.Xbox ? new XboxController(port) : (type == Type.Joystick ? new Joystick(port) : new GenericHID(port));

        if (type == Type.Joystick) {
            inverted = true;
            threshold = 0.15;
        }

        if (type == Type.Simulated) {
            this.awaitingForConnection = false;
            this.connected = true;
            threshold = 0.05;
            Logger.Log("Initialized simulated controller.");

            CommunicationManager.getInstance()
                .updateInfo("simcontrols", "joystick/x", 0d)
                .updateInfo("simcontrols", "joystick/y", 0d)
                .updateInfo("simcontrols", "rotation/angle", 0d)
                .updateInfo("simcontrols", "fov/angle", -1d);

            return;
        }

        if (checkIfConnected()) {
            Logger.Log("Successfully connected to controller (type " + type.name().toUpperCase() + ") on port " + port + "!");
        } else {
            Logger.Log("Controller " + port + " is not connected.");
        }
    }

    public boolean getButton(int button) {
        if (awaitingForConnection && !connected) checkIfConnected();

        if (!connected) return false;
        return controller.getRawButton(button);
    }

    public boolean getButtonPressed(int button) {
        if (awaitingForConnection && !connected) checkIfConnected();

        if (!connected) return false;
        return controller.getRawButtonPressed(button);
    }

    public boolean getButtonReleased(int button) {
        if (awaitingForConnection && !connected) checkIfConnected();

        if (!connected) return false;
        return controller.getRawButtonReleased(button);
    }

    public double getAxis(Axis axis) {
        if (awaitingForConnection && !connected) checkIfConnected();

        if (!connected) return 0;
        int axisNum = -1;

        switch (type) {
            case Xbox:
                if (axis == Axis.Horizontal) axisNum = Buttons.Xbox.leftXAxis;
                else if (axis == Axis.Vertical) axisNum = Buttons.Xbox.leftYAxis;
                else if (axis == Axis.Trigger) axisNum = Buttons.Xbox.leftTrigger;
                else throw new IllegalArgumentException("Invalid axis");

                double axisValueXbox = ((XboxController) controller).getRawAxis(axisNum) * (inverted ? -1 : 1);

                return Math.abs(axisValueXbox) > threshold ? axisValueXbox : 0;
            case Joystick:
                if (axis == Axis.Horizontal) axisNum = Buttons.Joystick.JoystickXAxis;
                else if (axis == Axis.Vertical) axisNum = Buttons.Joystick.JoystickYAxis;
                else if (axis == Axis.Rotation || axis == Axis.Twist) axisNum = Buttons.Joystick.JoystickTwistAxis;
                else throw new IllegalArgumentException("Invalid axis");

                double axisValueJoystick = ((Joystick) controller).getRawAxis(axisNum) * (inverted ? -1 : 1);

                return Math.abs(axisValueJoystick) > threshold ? axisValueJoystick : 0;
            case Simulated:
                double value = 0;
                if (axis == Axis.Horizontal) {
                    value = CommunicationManager.getInstance().get("simcontrols", "joystick/x").getDouble(0);
                } else if (axis == Axis.Vertical) {
                    value = CommunicationManager.getInstance().get("simcontrols", "joystick/y").getDouble(0);
                } else if (axis == Axis.Rotation || axis == Axis.Twist) {
                    value = CommunicationManager.getInstance().get("simcontrols", "rotation/angle").getDouble(0);
                }

                return value;
            default:
                try {
                    double axisValue = controller.getRawAxis(axis.getValue()) * (inverted ? -1 : 1);

                    return Math.abs(axisValue) > threshold ? axisValue : 0;
                } catch (Exception e) {
                    throw new IllegalArgumentException("Could not find axis on controller.");
                }
        }
    }

    public double getAxis(Axis.Side side, Axis axis) {
        if (awaitingForConnection && !connected) checkIfConnected();

        if (!connected) return 0;
        if (type != Type.Xbox) throw new IllegalArgumentException("Only Xbox controllers have sides, unless using custom controllers. If using custom controllers, use getRawAxis() instead.");

        int axisNum = -1;
        if (side == Axis.Side.Left) {
            switch (axis) {
                case Horizontal:
                    axisNum = Buttons.Xbox.leftXAxis;
                    break;
                case Vertical:
                    axisNum = Buttons.Xbox.leftYAxis;
                    break;
                case Trigger:
                    axisNum = Buttons.Xbox.leftTrigger;
                default:
                    throw new IllegalArgumentException("Invalid axis");
            }

            double axisValue = ((XboxController) controller).getRawAxis(axisNum) * (inverted ? -1 : 1);

            return Math.abs(axisValue) > threshold ? axisValue : 0;
        } else if (side == Axis.Side.Right) {
            switch (axis) {
                case Horizontal:
                    axisNum = Buttons.Xbox.rightXAxis;
                    break;
                case Vertical:
                    axisNum = Buttons.Xbox.rightYAxis;
                    break;
                case Trigger:
                    axisNum = Buttons.Xbox.rightTrigger;
                    break;
                default:
                    throw new IllegalArgumentException("Invalid axis");
            }
        }

        double axisValue = ((XboxController) controller).getRawAxis(axisNum) * (inverted ? -1 : 1);

        return Math.abs(axisValue) > threshold ? axisValue : 0;
    }

    public double getRawAxis(int axis) {
        if (awaitingForConnection && !connected) checkIfConnected();
        if (!connected) return 0;

        return controller.getRawAxis(axis);
    }

    public double getPOV(int pov) {
        if (awaitingForConnection && !connected) checkIfConnected();
        if (!connected) return 0;

        return controller.getPOV(pov);
    }

    public double getPOV() {
        if (awaitingForConnection && !connected) checkIfConnected();
        if (!connected) return 0;

        if (type == Type.Simulated) {
            return CommunicationManager.getInstance().get("simcontrols", "fov/angle").getDouble(0);
        }

        return controller.getPOV();
    }

    private boolean checkIfConnected() {
        boolean isConnected = DriverStation.isJoystickConnected(this.port);
        this.connected = isConnected;
        return isConnected;
    }

    public static enum Type {
        Joystick,
        Xbox,
        Custom,
        Simulated,
        None;

        public static Type detectType(int port) {
            if (!DriverStation.isJoystickConnected(port)) return None;

            if (DriverStation.getJoystickIsXbox(port)) {
                return Xbox;
            } else if (DriverStation.getJoystickName(port).contains("Logitech Extreme 3D")) {
                return Joystick;
            } else {
                return Custom;
            }
        }
    }

}
