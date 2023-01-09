package frc.team_8840_lib.input.controls;

import frc.team_8840_lib.info.console.Logger;
import frc.team_8840_lib.input.communication.CommunicationManager;
import frc.team_8840_lib.utils.controls.Axis;

public class SimulatedController {
    public SimulatedController() {
        Logger.Log("Initialized simulated controller.");

        CommunicationManager.getInstance()
            .updateInfo("simcontrols", "joystick/x", 0d)
            .updateInfo("simcontrols", "joystick/y", 0d)
            .updateInfo("simcontrols", "rotation/angle", 0d)
            .updateInfo("simcontrols", "fov/angle", -1d);
    }

    public double getPOV() {
        return CommunicationManager.getInstance().get("simcontrols", "fov/angle").getDouble(0);
    }

    public double getAxis(Axis axis) {
        double value = 0;

        if (axis == Axis.Horizontal) {
            value = CommunicationManager.getInstance().get("simcontrols", "joystick/x").getDouble(0);
        } else if (axis == Axis.Vertical) {
            value = CommunicationManager.getInstance().get("simcontrols", "joystick/y").getDouble(0);
        } else if (axis == Axis.Rotation || axis == Axis.Twist) {
            value = CommunicationManager.getInstance().get("simcontrols", "rotation/angle").getDouble(0);
        }
        
        return value;
    }
}
