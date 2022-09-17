package frc.team_8840_lib.utils.controls;

/**
 * Axis of a joystick.
 *
 * @author JaidenAGrimminck
 * */
public enum Axis {
    Horizontal,
    Vertical,
    Twist,
    Rotation,
    Trigger;

    public int getValue() {
        switch (this) {
            case Horizontal:
                return 0;
            case Vertical:
                return 1;
            case Rotation:
            case Twist:
                return 2;
            default:
                return -1;
        }
    }

    public static enum Side {
        Left,
        Right
    }
}
